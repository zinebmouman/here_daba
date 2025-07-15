package com.boutique_catalogue_produits.dto;
import org.springframework.web.multipart.MultipartFile;
import java.util.Date;
import java.util.List;
import java.util.Map;
public class UniversalSearchResponse {
    private String query;
    private String strategy;
    private boolean success;
    private String errorMessage;

    // Résultats des différents services
    private RagSearchResponse ragResults;
    private ImageSearchResponse imageResults;
    private List<RecommendationItem> recommendations;

    // Enrichissements IA
    private String aiInsights;
    private Map<String, Object> metadata;

    // Métriques
    private long processingTimeMs;
    private Date timestamp;

    // Constructeurs
    public UniversalSearchResponse() {
        this.timestamp = new Date();
    }

    // Getters et Setters
    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }

    public String getStrategy() { return strategy; }
    public void setStrategy(String strategy) { this.strategy = strategy; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public RagSearchResponse getRagResults() { return ragResults; }
    public void setRagResults(RagSearchResponse ragResults) { this.ragResults = ragResults; }

    public ImageSearchResponse getImageResults() { return imageResults; }
    public void setImageResults(ImageSearchResponse imageResults) { this.imageResults = imageResults; }

    public List<RecommendationItem> getRecommendations() { return recommendations; }
    public void setRecommendations(List<RecommendationItem> recommendations) {
        this.recommendations = recommendations;
    }

    public String getAiInsights() { return aiInsights; }
    public void setAiInsights(String aiInsights) { this.aiInsights = aiInsights; }

    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }

    public long getProcessingTimeMs() { return processingTimeMs; }
    public void setProcessingTimeMs(long processingTimeMs) { this.processingTimeMs = processingTimeMs; }

    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }
}
