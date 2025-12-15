package com.example.demo.dto.response;

import java.time.LocalDateTime;

public class PetStatusSummaryResponse {
    private Long petId;
    private String petName;
    private String summary;
    private String timeline; // 文字描述的时间线
    private String keyFindings;
    private String recommendations;
    private LocalDateTime generatedAt;

    // Getters and Setters
    public Long getPetId() { return petId; }
    public void setPetId(Long petId) { this.petId = petId; }

    public String getPetName() { return petName; }
    public void setPetName(String petName) { this.petName = petName; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getTimeline() { return timeline; }
    public void setTimeline(String timeline) { this.timeline = timeline; }

    public String getKeyFindings() { return keyFindings; }
    public void setKeyFindings(String keyFindings) { this.keyFindings = keyFindings; }

    public String getRecommendations() { return recommendations; }
    public void setRecommendations(String recommendations) { this.recommendations = recommendations; }

    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }
}
