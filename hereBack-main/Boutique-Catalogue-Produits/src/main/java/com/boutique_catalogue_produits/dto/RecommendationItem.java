package com.boutique_catalogue_produits.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 💡 DTO pour les recommandations IA
 */
public class RecommendationItem {

    private String id;
    private String type; // PRODUIT, BOUTIQUE, CONSEIL
    private String title;
    private String description;
    private String url;
    private Double score; // Score de pertinence (0.0 à 1.0)
    private String category;
    private String reason; // Raison de la recommandation

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    private Map<String, Object> metadata;

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    // Constructeurs
    public RecommendationItem() {
        this.createdAt = LocalDateTime.now();
    }

    public RecommendationItem(String type, String title, String description) {
        this();
        this.type = type;
        this.title = title;
        this.description = description;
    }

    public RecommendationItem(String type, String title, String description, Double score) {
        this(type, title, description);
        this.score = score;
    }

    public RecommendationItem(String type, String title, String description, String reason, Double score) {
        this(type, title, description, score);
        this.reason = reason;
    }

    // Getters et Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "RecommendationItem{" +
                "type='" + type + '\'' +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", score=" + score +
                '}';
    }
}