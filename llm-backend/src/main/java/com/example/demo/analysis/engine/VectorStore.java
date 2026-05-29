package com.example.demo.analysis.engine;

import com.example.demo.analysis.entity.KnowledgeChunk;
import com.example.demo.analysis.repository.KnowledgeChunkRepository;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class VectorStore {

    private final KnowledgeChunkRepository repository;
    private final EmbeddingService embeddingService;

    public VectorStore(KnowledgeChunkRepository repository, EmbeddingService embeddingService) {
        this.repository = repository;
        this.embeddingService = embeddingService;
    }

    @PostConstruct
    public void init() {
        if (repository.count() > 0) {
            log.info("知识库已有数据（{}条），跳过初始化", repository.count());
            return;
        }
        log.info("从CSV加载知识库...");
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new ClassPathResource("knowledge/pet_care.csv").getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            boolean header = true;
            int count = 0;
            while ((line = br.readLine()) != null) {
                if (header) { header = false; continue; }
                String[] parts = parseCsvLine(line);
                if (parts.length >= 3) {
                    addEntry(parts[0].trim(), parts[1].trim(), parts[2].trim());
                    count++;
                }
            }
            log.info("知识库初始化完成，共{}条", count);
        } catch (Exception e) {
            log.error("读取CSV知识库失败", e);
        }
    }

    private String[] parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuote = false;
        for (char c : line.toCharArray()) {
            if (c == '"') { inQuote = !inQuote; }
            else if (c == ',' && !inQuote) { fields.add(cur.toString()); cur = new StringBuilder(); }
            else { cur.append(c); }
        }
        fields.add(cur.toString());
        return fields.toArray(new String[0]);
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

    private double cosineSimilarity(float[] a, float[] b) {
        int len = Math.min(a.length, b.length);
        double dot = 0, na = 0, nb = 0;
        for (int i = 0; i < len; i++) { dot += a[i] * b[i]; na += a[i] * a[i]; nb += b[i] * b[i]; }
        double d = Math.sqrt(na) * Math.sqrt(nb);
        return d == 0 ? 0 : dot / d;
    }

    private void addEntry(String title, String category, String content) {
        float[] v = embeddingService.embed(content);
        KnowledgeChunk c = new KnowledgeChunk();
        c.setTitle(title);
        c.setContent(content);
        c.setContentHash(Integer.toHexString(content.hashCode()));
        c.setEmbedding(embeddingService.floatToBytes(v));
        c.setCategory(category);
        repository.save(c);
    }

    public long count() { return repository.count(); }

    @Data
    public static class SearchResult {
        private final KnowledgeChunk chunk;
        private final double score;
    }
}
