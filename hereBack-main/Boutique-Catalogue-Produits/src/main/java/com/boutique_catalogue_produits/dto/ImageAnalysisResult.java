package com.boutique_catalogue_produits.dto;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class ImageAnalysisResult {
    private String imageUrl;
    private Map<String, Object> technicalInfo;
    private List<String> detectedObjects;
    private List<String> dominantColors;
    private String suggestedCategory;
    private List<String> tags;
    private String aiDescription;
    private double confidenceScore;
    private Map<String, Double> categoryProbabilities;
    private Date analysisDate;

    // Constructeurs
    public ImageAnalysisResult() {
        this.analysisDate = new Date();
    }

    // Getters et Setters
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public Map<String, Object> getTechnicalInfo() { return technicalInfo; }
    public void setTechnicalInfo(Map<String, Object> technicalInfo) { this.technicalInfo = technicalInfo; }

    public List<String> getDetectedObjects() { return detectedObjects; }
    public void setDetectedObjects(List<String> detectedObjects) { this.detectedObjects = detectedObjects; }

    public List<String> getDominantColors() { return dominantColors; }
    public void setDominantColors(List<String> dominantColors) { this.dominantColors = dominantColors; }

    public String getSuggestedCategory() { return suggestedCategory; }
    public void setSuggestedCategory(String suggestedCategory) { this.suggestedCategory = suggestedCategory; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    public String getAiDescription() { return aiDescription; }
    public void setAiDescription(String aiDescription) { this.aiDescription = aiDescription; }

    public double getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(double confidenceScore) { this.confidenceScore = confidenceScore; }

    public Map<String, Double> getCategoryProbabilities() { return categoryProbabilities; }
    public void setCategoryProbabilities(Map<String, Double> categoryProbabilities) {
        this.categoryProbabilities = categoryProbabilities;
    }

    public Date getAnalysisDate() { return analysisDate; }
    public void setAnalysisDate(Date analysisDate) { this.analysisDate = analysisDate; }
}
