package com.example.demo.analysis.engine;

import lombok.Data;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

@Component
public class TextChunker {

    public List<Chunk> chunk(String text, String title) {
        List<Chunk> chunks = new ArrayList<>();
        String[] paragraphs = text.split("\n\n");
        StringBuilder current = new StringBuilder();
        int index = 0;

        for (String para : paragraphs) {
            String t = para.trim();
            if (t.isEmpty()) continue;
            if (current.length() + t.length() > 500 && current.length() > 0) {
                chunks.add(new Chunk(index++, title, current.toString().trim()));
                int overlap = Math.min(100, current.length());
                current = new StringBuilder(current.substring(current.length() - overlap));
            }
            current.append(t).append("\n\n");
        }
        if (current.length() > 0) {
            chunks.add(new Chunk(index, title, current.toString().trim()));
        }
        return chunks;
    }

    @Data
    public static class Chunk {
        private final int index;
        private final String title;
        private final String content;
        public Chunk(int index, String title, String content) {
            this.index = index; this.title = title; this.content = content;
        }
    }
}
