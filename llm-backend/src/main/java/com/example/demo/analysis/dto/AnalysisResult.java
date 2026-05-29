package com.example.demo.analysis.dto;

import lombok.Data;
import java.util.List;

@Data
public class AnalysisResult {
    private String petId;
    private String petName;
    private String breed;
    private String species;
    private String analysis;
    private List<KnowledgeSource> knowledgeSources;
    private String analysisType;

    @Data
    public static class KnowledgeSource {
        private String title;
        private String content;
        private double score;
    }
}
