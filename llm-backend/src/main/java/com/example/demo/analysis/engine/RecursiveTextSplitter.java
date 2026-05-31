package com.example.demo.analysis.engine;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class RecursiveTextSplitter {

    private final int chunkSize;
    private final int chunkOverlap;

    public RecursiveTextSplitter() {
        this(500, 50);
    }

    public RecursiveTextSplitter(int chunkSize, int chunkOverlap) {
        this.chunkSize = chunkSize;
        this.chunkOverlap = chunkOverlap;
    }

    public List<Chunk> split(List<PdfDocument> docs) {
        List<Chunk> result = new ArrayList<>();
        for (PdfDocument doc : docs) {
            result.addAll(splitSingle(doc));
        }
        return result;
    }

    private List<Chunk> splitSingle(PdfDocument doc) {
        List<String> raw = recursiveSplitText(doc.getContent());
        List<Chunk> chunks = new ArrayList<>();
        for (int i = 0; i < raw.size(); i++) {
            chunks.add(new Chunk(doc.getTitle() + "_c" + i, doc.getCategory(), raw.get(i)));
        }
        return chunks;
    }

    private List<String> recursiveSplitText(String text) {
        List<String> seps = List.of("\n\n", "。", "！", "？", "，", "、", ";", "；");
        return splitWithSeparators(text, seps, 0);
    }

    private List<String> splitWithSeparators(String text, List<String> seps, int depth) {
        List<String> result = new ArrayList<>();
        if (text.length() <= chunkSize) {
            result.add(text);
            return result;
        }
        if (depth >= seps.size()) {
            return splitByChar(text);
        }
        String sep = seps.get(depth);
        String[] parts = text.split(sep, -1);
        StringBuilder cur = new StringBuilder();

        for (String part : parts) {
            String candidate = cur.length() > 0 ? cur + sep + part : part;
            if (candidate.length() > chunkSize && cur.length() > 0) {
                result.add(cur.toString().trim());
                String overlap = cur.substring(Math.max(0, cur.length() - chunkOverlap));
                cur = new StringBuilder(overlap + sep + part);
            } else if (candidate.length() > chunkSize) {
                result.addAll(splitWithSeparators(part, seps, depth + 1));
            } else {
                cur = new StringBuilder(candidate);
            }
        }
        if (cur.length() > 0) {
            result.add(cur.toString().trim());
        }
        return result;
    }

    private List<String> splitByChar(String text) {
        List<String> result = new ArrayList<>();
        for (int i = 0; i < text.length(); i += (chunkSize - chunkOverlap)) {
            int end = Math.min(i + chunkSize, text.length());
            result.add(text.substring(i, end));
        }
        return result;
    }

    @Data
    @AllArgsConstructor
    public static class Chunk {
        private String title;
        private String category;
        private String content;
    }
}
