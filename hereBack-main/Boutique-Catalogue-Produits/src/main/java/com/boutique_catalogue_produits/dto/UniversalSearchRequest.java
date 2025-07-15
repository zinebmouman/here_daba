package com.boutique_catalogue_produits.dto;

import org.springframework.web.multipart.MultipartFile;
import java.util.Date;
import java.util.List;
import java.util.Map;

// =============== DTOs POUR LA RECHERCHE UNIVERSELLE ===============

/**
 * 🌍 Requête de recherche universelle
 */
public class UniversalSearchRequest {
    private String query;
    private MultipartFile imageFile;
    private String userId;
    private String searchMode; // "text", "image", "hybrid", "recommendation"
    private String searchType; // "PRODUIT", "BOUTIQUE", "MIXED"
    private Map<String, Object> filters;
    private Map<String, Object> context;
    private int maxResults = 50;

    // Constructeurs
    public UniversalSearchRequest() {}

    public UniversalSearchRequest(String query, String userId) {
        this.query = query;
        this.userId = userId;
    }

    // Getters et Setters
    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }

    public MultipartFile getImageFile() { return imageFile; }
    public void setImageFile(MultipartFile imageFile) { this.imageFile = imageFile; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getSearchMode() { return searchMode; }
    public void setSearchMode(String searchMode) { this.searchMode = searchMode; }

    public String getSearchType() { return searchType; }
    public void setSearchType(String searchType) { this.searchType = searchType; }

    public Map<String, Object> getFilters() { return filters; }
    public void setFilters(Map<String, Object> filters) { this.filters = filters; }

    public Map<String, Object> getContext() { return context; }
    public void setContext(Map<String, Object> context) { this.context = context; }

    public int getMaxResults() { return maxResults; }
    public void setMaxResults(int maxResults) { this.maxResults = maxResults; }
}
