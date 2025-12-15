package com.example.demo.dto.request;

public class PetStatusSummaryRequest {
    private Long petId;
    private String petName; // 可选，用于个性化总结
    private SummaryType summaryType = SummaryType.DETAILED;
    private boolean includeTimeline = true;
    private boolean includeRecommendations = true;

    public enum SummaryType {
        CONCISE,     // 简洁总结
        DETAILED,    // 详细分析
        ANALYTICAL   // 专业分析
    }

    // Getters and Setters
    public Long getPetId() { return petId; }
    public void setPetId(Long petId) { this.petId = petId; }

    public String getPetName() { return petName; }
    public void setPetName(String petName) { this.petName = petName; }

    public SummaryType getSummaryType() { return summaryType; }
    public void setSummaryType(SummaryType summaryType) { this.summaryType = summaryType; }

    public boolean isIncludeTimeline() { return includeTimeline; }
    public void setIncludeTimeline(boolean includeTimeline) { this.includeTimeline = includeTimeline; }

    public boolean isIncludeRecommendations() { return includeRecommendations; }
    public void setIncludeRecommendations(boolean includeRecommendations) { this.includeRecommendations = includeRecommendations; }
}