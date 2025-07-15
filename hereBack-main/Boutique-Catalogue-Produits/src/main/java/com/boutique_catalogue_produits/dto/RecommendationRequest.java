package com.boutique_catalogue_produits.dto;

import java.util.List;
import java.util.Map;

public class RecommendationRequest {
    private String userId;
    private String recommendationType; // USER_BASED, ITEM_BASED, HYBRID, CONTEXTUAL
    private String contextType; // trending, seasonal, budget, location
    private Map<String, Object> parameters;
    private int maxRecommendations = 10;
    private List<String> excludeItems;

    // Constructeurs
    public RecommendationRequest() {}

    public RecommendationRequest(String userId, String recommendationType) {
        this.userId = userId;
        this.recommendationType = recommendationType;
    }

    // Getters et Setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getRecommendationType() { return recommendationType; }
    public void setRecommendationType(String recommendationType) { this.recommendationType = recommendationType; }

    public String getContextType() { return contextType; }
    public void setContextType(String contextType) { this.contextType = contextType; }

    public Map<String, Object> getParameters() { return parameters; }
    public void setParameters(Map<String, Object> parameters) { this.parameters = parameters; }

    public int getMaxRecommendations() { return maxRecommendations; }
    public void setMaxRecommendations(int maxRecommendations) { this.maxRecommendations = maxRecommendations; }

    public List<String> getExcludeItems() { return excludeItems; }
    public void setExcludeItems(List<String> excludeItems) { this.excludeItems = excludeItems; }
}