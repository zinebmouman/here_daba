package com.boutique_catalogue_produits.dto;

import java.util.Map;

public class ImageSearchRequest {
    private String imageUrl;
    private String searchType; // PRODUIT, BOUTIQUE, MIXED
    private String userId;
    private int maxResults = 20;
    private double similarityThreshold = 0.3;
    private Map<String, Object> filters;

    // Constructeurs
    public ImageSearchRequest() {}

    // Getters et Setters
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getSearchType() { return searchType; }
    public void setSearchType(String searchType) { this.searchType = searchType; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public int getMaxResults() { return maxResults; }
    public void setMaxResults(int maxResults) { this.maxResults = maxResults; }

    public double getSimilarityThreshold() { return similarityThreshold; }
    public void setSimilarityThreshold(double similarityThreshold) { this.similarityThreshold = similarityThreshold; }

    public Map<String, Object> getFilters() { return filters; }
    public void setFilters(Map<String, Object> filters) { this.filters = filters; }
}