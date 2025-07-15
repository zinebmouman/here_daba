package com.boutique_catalogue_produits.dto;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class RecommendationResponse {
    private String userId;
    private String recommendationType;
    private List<RecommendationItem> recommendations;
    private int totalRecommendations;
    private String algorithm;
    private double confidenceScore;
    private Map<String, Object> metadata;
    private Date timestamp;
    private long processingTimeMs;

    // Constructeurs
    public RecommendationResponse() {
        this.timestamp = new Date();
    }

    // Getters et Setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getRecommendationType() { return recommendationType; }
    public void setRecommendationType(String recommendationType) { this.recommendationType = recommendationType; }

    public List<RecommendationItem> getRecommendations() { return recommendations; }
    public void setRecommendations(List<RecommendationItem> recommendations) {
        this.recommendations = recommendations;
        this.totalRecommendations = recommendations != null ? recommendations.size() : 0;
    }

    public int getTotalRecommendations() { return totalRecommendations; }
    public void setTotalRecommendations(int totalRecommendations) {
        this.totalRecommendations = totalRecommendations;
    }

    public String getAlgorithm() { return algorithm; }
    public void setAlgorithm(String algorithm) { this.algorithm = algorithm; }

    public double getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(double confidenceScore) { this.confidenceScore = confidenceScore; }

    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }

    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }

    public long getProcessingTimeMs() { return processingTimeMs; }
    public void setProcessingTimeMs(long processingTimeMs) { this.processingTimeMs = processingTimeMs; }
}
