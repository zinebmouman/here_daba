package com.boutique_catalogue_produits.dto;
import org.springframework.web.multipart.MultipartFile;
import java.util.Date;
import java.util.List;
import java.util.Map;
public class IntelligentRecommendationResponse {
    private String userId;
    private Map<String, Object> context;
    private List<RecommendationItem> recommendations;
    private boolean success;
    private String errorMessage;

    // Enrichissements IA
    private String aiExplanation;
    private Map<String, Double> confidenceScores;
    private List<String> reasoningSteps;

    // Métriques
    private long processingTimeMs;
    private Date timestamp;

    // Constructeurs
    public IntelligentRecommendationResponse() {
        this.timestamp = new Date();
    }

    // Getters et Setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public Map<String, Object> getContext() { return context; }
    public void setContext(Map<String, Object> context) { this.context = context; }

    public List<RecommendationItem> getRecommendations() { return recommendations; }
    public void setRecommendations(List<RecommendationItem> recommendations) {
        this.recommendations = recommendations;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public String getAiExplanation() { return aiExplanation; }
    public void setAiExplanation(String aiExplanation) { this.aiExplanation = aiExplanation; }

    public Map<String, Double> getConfidenceScores() { return confidenceScores; }
    public void setConfidenceScores(Map<String, Double> confidenceScores) {
        this.confidenceScores = confidenceScores;
    }

    public List<String> getReasoningSteps() { return reasoningSteps; }
    public void setReasoningSteps(List<String> reasoningSteps) { this.reasoningSteps = reasoningSteps; }

    public long getProcessingTimeMs() { return processingTimeMs; }
    public void setProcessingTimeMs(long processingTimeMs) { this.processingTimeMs = processingTimeMs; }

    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }
}
