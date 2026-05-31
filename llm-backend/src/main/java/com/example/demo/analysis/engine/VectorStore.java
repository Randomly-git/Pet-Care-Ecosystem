package com.example.demo.analysis.engine;

import com.example.demo.analysis.entity.KnowledgeChunk;
import com.example.demo.analysis.repository.KnowledgeChunkRepository;
import com.example.demo.analysis.engine.RecursiveTextSplitter.Chunk;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class VectorStore {

    private final KnowledgeChunkRepository repository;
    private final EmbeddingService embeddingService;
    private final PdfLoader pdfLoader;
    private final RecursiveTextSplitter textSplitter;
    private final ObjectMapper objectMapper;

    private static final String BACKUP_FILE = "knowledge_base.json";

    public VectorStore(KnowledgeChunkRepository repository,
                       EmbeddingService embeddingService,
                       PdfLoader pdfLoader,
                       RecursiveTextSplitter textSplitter,
                       ObjectMapper objectMapper) {
        this.repository = repository;
        this.embeddingService = embeddingService;
        this.pdfLoader = pdfLoader;
        this.textSplitter = textSplitter;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        if (repository.count() > 0) {
            log.info("知识库已有数据（{}条），跳过初始化", repository.count());
            return;
        }

        // ① 优先从备份文件加载
        if (loadFromBackup()) return;

        // ② 从 PDF 构建
        List<PdfDocument> docs = pdfLoader.load("knowledge/");
        if (docs.isEmpty()) {
            log.warn("knowledge/ 目录为空或无PDF，知识库为空");
            return;
        }

        List<Chunk> chunks = textSplitter.split(docs);
        List<ChunkEntry> entries = new ArrayList<>();

        for (Chunk chunk : chunks) {
            float[] vec = embeddingService.embed(chunk.getContent());
            KnowledgeChunk kc = new KnowledgeChunk();
            kc.setTitle(chunk.getTitle());
            kc.setContent(chunk.getContent());
            kc.setContentHash(Integer.toHexString(chunk.getContent().hashCode()));
            kc.setEmbedding(embeddingService.floatToBytes(vec));
            kc.setCategory(chunk.getCategory());
            repository.save(kc);

            ChunkEntry entry = new ChunkEntry();
            entry.title = chunk.getTitle();
            entry.category = chunk.getCategory();
            entry.content = chunk.getContent();
            entry.embedding = vec;
            entries.add(entry);
        }

        // ③ 导出备份文件
        saveBackup(entries);
        log.info("知识库初始化完成：{}条知识（来自{}页PDF，{}个分块）", chunks.size(), docs.size(), chunks.size());
    }

    public List<SearchResult> search(String query, int topK) {
        float[] qv = embeddingService.embed(query);
        List<KnowledgeChunk> all = repository.findAll();
        return all.parallelStream()
                .map(c -> { double s = cosineSimilarity(qv, embeddingService.bytesToFloat(c.getEmbedding())); return new SearchResult(c, s); })
                .sorted(Comparator.comparingDouble(SearchResult::getScore).reversed())
                .limit(topK)
                .filter(r -> r.getScore() > 0.3)
                .collect(Collectors.toList());
    }

    public long count() { return repository.count(); }

    private boolean loadFromBackup() {
        File file = new File(BACKUP_FILE);
        if (!file.exists()) return false;

        try {
            List<ChunkEntry> entries = objectMapper.readValue(file, new TypeReference<List<ChunkEntry>>() {});
            for (ChunkEntry e : entries) {
                KnowledgeChunk kc = new KnowledgeChunk();
                kc.setTitle(e.title);
                kc.setContent(e.content);
                kc.setContentHash(Integer.toHexString(e.content.hashCode()));
                kc.setEmbedding(embeddingService.floatToBytes(e.embedding));
                kc.setCategory(e.category);
                repository.save(kc);
            }
            log.info("从备份文件恢复知识库：{}条", entries.size());
            return true;
        } catch (IOException e) {
            log.warn("读取备份文件失败，将重新构建", e);
            return false;
        }
    }

    private void saveBackup(List<ChunkEntry> entries) {
        try {
            objectMapper.writeValue(new File(BACKUP_FILE), entries);
            log.info("知识库已导出到 {}", new File(BACKUP_FILE).getAbsolutePath());
        } catch (IOException e) {
            log.warn("导出知识库备份失败", e);
        }
    }

    private double cosineSimilarity(float[] a, float[] b) {
        int len = Math.min(a.length, b.length);
        double dot = 0, na = 0, nb = 0;
        for (int i = 0; i < len; i++) { dot += a[i] * b[i]; na += a[i] * a[i]; nb += b[i] * b[i]; }
        double d = Math.sqrt(na) * Math.sqrt(nb);
        return d == 0 ? 0 : dot / d;
    }

    @Data
    public static class SearchResult {
        private final KnowledgeChunk chunk;
        private final double score;
    }

    @Data
    private static class ChunkEntry {
        private String title;
        private String category;
        private String content;
        private float[] embedding;
    }
}
