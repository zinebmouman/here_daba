package com.boutique_catalogue_produits.dto;
import org.springframework.web.multipart.MultipartFile;
import java.util.Date;
import java.util.List;
import java.util.Map;
public class HybridSearchRequest {
    private String textQuery;
    private MultipartFile imageFile;
    private String userId;
    private String searchType;
    private Map<String, Object> filters;
    private boolean enablePersonalization = true;
    private int maxResults = 30;

    // Constructeurs
    public HybridSearchRequest() {}

    public HybridSearchRequest(String textQuery, MultipartFile imageFile, String userId) {
        this.textQuery = textQuery;
        this.imageFile = imageFile;
        this.userId = userId;
    }

    // Getters et Setters
    public String getTextQuery() { return textQuery; }
    public void setTextQuery(String textQuery) { this.textQuery = textQuery; }

    public MultipartFile getImageFile() { return imageFile; }
    public void setImageFile(MultipartFile imageFile) { this.imageFile = imageFile; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getSearchType() { return searchType; }
    public void setSearchType(String searchType) { this.searchType = searchType; }

    public Map<String, Object> getFilters() { return filters; }
    public void setFilters(Map<String, Object> filters) { this.filters = filters; }

    public boolean isEnablePersonalization() { return enablePersonalization; }
    public void setEnablePersonalization(boolean enablePersonalization) {
        this.enablePersonalization = enablePersonalization;
    }

    public int getMaxResults() { return maxResults; }
    public void setMaxResults(int maxResults) { this.maxResults = maxResults; }
}
