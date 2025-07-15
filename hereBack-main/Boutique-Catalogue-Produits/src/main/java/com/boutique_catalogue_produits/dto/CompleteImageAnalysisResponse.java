package com.boutique_catalogue_produits.dto;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class CompleteImageAnalysisResponse {
    private String fileName;
    private boolean success;
    private String errorMessage;

    // Résultats des différentes analyses
    private Map<String, Object> geminiAnalysis;
    private ImageSearchResponse similarityResults;
    private List<RecommendationItem> personalizedRecommendations;
    private float[] imageEmbeddings;

    // Synthèse intelligente
    private String aiSynthesis;
    private Map<String, Object> extractedMetadata;

    // Métriques
    private long processingTimeMs;
    private Date timestamp;

    // Constructeurs
    public CompleteImageAnalysisResponse() {
        this.timestamp = new Date();
    }

    // Getters et Setters
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public Map<String, Object> getGeminiAnalysis() { return geminiAnalysis; }
    public void setGeminiAnalysis(Map<String, Object> geminiAnalysis) { this.geminiAnalysis = geminiAnalysis; }

    public ImageSearchResponse getSimilarityResults() { return similarityResults; }
    public void setSimilarityResults(ImageSearchResponse similarityResults) {
        this.similarityResults = similarityResults;
    }

    public List<RecommendationItem> getPersonalizedRecommendations() { return personalizedRecommendations; }
    public void setPersonalizedRecommendations(List<RecommendationItem> personalizedRecommendations) {
        this.personalizedRecommendations = personalizedRecommendations;
    }

    public float[] getImageEmbeddings() { return imageEmbeddings; }
    public void setImageEmbeddings(float[] imageEmbeddings) { this.imageEmbeddings = imageEmbeddings; }

    public String getAiSynthesis() { return aiSynthesis; }
    public void setAiSynthesis(String aiSynthesis) { this.aiSynthesis = aiSynthesis; }

    public Map<String, Object> getExtractedMetadata() { return extractedMetadata; }
    public void setExtractedMetadata(Map<String, Object> extractedMetadata) {
        this.extractedMetadata = extractedMetadata;
    }

    public long getProcessingTimeMs() { return processingTimeMs; }
    public void setProcessingTimeMs(long processingTimeMs) { this.processingTimeMs = processingTimeMs; }

    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }
}