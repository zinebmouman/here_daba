package com.boutique_catalogue_produits.dto;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 🖼️ Réponse de recherche par image
 */
public class ImageSearchResponse {
    private String queryImage;
    private String searchType;
    private List<SimilarImageResult> similarProducts;
    private int totalResults;
    private boolean success;
    private String errorMessage;
    private Date timestamp;
    private long processingTimeMs;
    private Map<String, Object> aiAnalysis;
    private List<String> recommendations;
    private Map<String, Object> statistics;

    // Constructeurs
    public ImageSearchResponse() {}

    // Getters et Setters
    public String getQueryImage() { return queryImage; }
    public void setQueryImage(String queryImage) { this.queryImage = queryImage; }

    public String getSearchType() { return searchType; }
    public void setSearchType(String searchType) { this.searchType = searchType; }

    public List<SimilarImageResult> getSimilarProducts() { return similarProducts; }
    public void setSimilarProducts(List<SimilarImageResult> similarProducts) { this.similarProducts = similarProducts; }

    public int getTotalResults() { return totalResults; }
    public void setTotalResults(int totalResults) { this.totalResults = totalResults; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }

    public long getProcessingTimeMs() { return processingTimeMs; }
    public void setProcessingTimeMs(long processingTimeMs) { this.processingTimeMs = processingTimeMs; }

    public Map<String, Object> getAiAnalysis() { return aiAnalysis; }
    public void setAiAnalysis(Map<String, Object> aiAnalysis) { this.aiAnalysis = aiAnalysis; }

    public List<String> getRecommendations() { return recommendations; }
    public void setRecommendations(List<String> recommendations) { this.recommendations = recommendations; }

    public Map<String, Object> getStatistics() { return statistics; }
    public void setStatistics(Map<String, Object> statistics) { this.statistics = statistics; }
}
