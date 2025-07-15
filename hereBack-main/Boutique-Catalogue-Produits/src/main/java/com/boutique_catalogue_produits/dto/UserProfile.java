package com.boutique_catalogue_produits.dto;

import java.util.Date;
import java.util.Map;
import java.util.Set;

public class UserProfile {
    private String userId;
    private Map<String, Double> categoryPreferences;
    private PriceRange priceRange;
    private Set<String> preferredBrands;
    private String preferredLocation;
    private double diversityScore;
    private Date lastUpdated;

    // Constructeurs
    public UserProfile() {
        this.lastUpdated = new Date();
    }

    // Getters et Setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public Map<String, Double> getCategoryPreferences() { return categoryPreferences; }
    public void setCategoryPreferences(Map<String, Double> categoryPreferences) {
        this.categoryPreferences = categoryPreferences;
    }

    public PriceRange getPriceRange() { return priceRange; }
    public void setPriceRange(PriceRange priceRange) { this.priceRange = priceRange; }

    public Set<String> getPreferredBrands() { return preferredBrands; }
    public void setPreferredBrands(Set<String> preferredBrands) { this.preferredBrands = preferredBrands; }

    public String getPreferredLocation() { return preferredLocation; }
    public void setPreferredLocation(String preferredLocation) { this.preferredLocation = preferredLocation; }

    public double getDiversityScore() { return diversityScore; }
    public void setDiversityScore(double diversityScore) { this.diversityScore = diversityScore; }

    public Date getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(Date lastUpdated) { this.lastUpdated = lastUpdated; }
}
