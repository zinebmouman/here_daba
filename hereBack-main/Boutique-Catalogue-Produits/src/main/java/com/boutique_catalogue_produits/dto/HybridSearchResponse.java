package com.boutique_catalogue_produits.dto;
import org.springframework.web.multipart.MultipartFile;
import java.util.Date;
import java.util.List;
import java.util.Map;
public class HybridSearchResponse {
    private boolean success;
    private String errorMessage;

    // Résultats des différents moteurs
    private RagSearchResponse ragResults;
    private ImageSearchResponse imageResults;
    private List<RecommendationItem> personalizedRecommendations;

    // Fusion intelligente
    private List<Object> fusedResults; // Produits et boutiques fusionnés
    private Map<String, Double> fusionScores;
    private String fusionStrategy;

    // Enrichissements IA
    private String aiSummary;
    private List<String> aiSuggestions;

    // Métriques
    private long processingTimeMs;
    private Date timestamp;

    // Constructeurs
    public HybridSearchResponse() {
        this.timestamp = new Date();
    }

    // Getters et Setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public RagSearchResponse getRagResults() { return ragResults; }
    public void setRagResults(RagSearchResponse ragResults) { this.ragResults = ragResults; }

    public ImageSearchResponse getImageResults() { return imageResults; }
    public void setImageResults(ImageSearchResponse imageResults) { this.imageResults = imageResults; }

    public List<RecommendationItem> getPersonalizedRecommendations() { return personalizedRecommendations; }
    public void setPersonalizedRecommendations(List<RecommendationItem> personalizedRecommendations) {
        this.personalizedRecommendations = personalizedRecommendations;
    }

    public List<Object> getFusedResults() { return fusedResults; }
    public void setFusedResults(List<Object> fusedResults) { this.fusedResults = fusedResults; }

    public Map<String, Double> getFusionScores() { return fusionScores; }
    public void setFusionScores(Map<String, Double> fusionScores) { this.fusionScores = fusionScores; }

    public String getFusionStrategy() { return fusionStrategy; }
    public void setFusionStrategy(String fusionStrategy) { this.fusionStrategy = fusionStrategy; }

    public String getAiSummary() { return aiSummary; }
    public void setAiSummary(String aiSummary) { this.aiSummary = aiSummary; }

    public List<String> getAiSuggestions() { return aiSuggestions; }
    public void setAiSuggestions(List<String> aiSuggestions) { this.aiSuggestions = aiSuggestions; }

    public long getProcessingTimeMs() { return processingTimeMs; }
    public void setProcessingTimeMs(long processingTimeMs) { this.processingTimeMs = processingTimeMs; }

    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }
}