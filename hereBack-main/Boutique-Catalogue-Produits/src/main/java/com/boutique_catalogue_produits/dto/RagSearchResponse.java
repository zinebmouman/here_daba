package com.boutique_catalogue_produits.dto;

import com.boutique_catalogue_produits.model.Boutique;
import java.util.Date;
import java.util.List;

public class RagSearchResponse {

    // Métadonnées de la recherche
    private String query;
    private String searchType; // PRODUIT, BOUTIQUE, MIXED
    private boolean ragEnhanced;
    private Date timestamp;
    private Integer totalResults;

    // Analyse et recommandations IA
    private String analysis;
    private String summary;
    private List<String> alternatives;
    private List<String> tips;
    private double confidenceScore;

    // Résultats de la recherche
    private List<ProduitDTO> produits;
    private List<Boutique> boutiques;
    private Integer nombreProduits;
    private Integer nombreBoutiques;

    // Recommandations intelligentes
    private List<RecommendationItem> recommendations;

    // Contexte géographique
    private String ville;
    private Integer rayonKm;

    // Informations de performance
    private long processingTimeMs;
    private String aiModel;

    // Constructeurs
    public RagSearchResponse() {}

    public RagSearchResponse(String query, String searchType) {
        this.query = query;
        this.searchType = searchType;
        this.timestamp = new Date();
        this.ragEnhanced = true;
    }

    // Getters et Setters
    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }

    public String getSearchType() { return searchType; }
    public void setSearchType(String searchType) { this.searchType = searchType; }

    public boolean isRagEnhanced() { return ragEnhanced; }
    public void setRagEnhanced(boolean ragEnhanced) { this.ragEnhanced = ragEnhanced; }

    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }

    public Integer getTotalResults() { return totalResults; }
    public void setTotalResults(int totalResults) { this.totalResults = totalResults; }

    public String getAnalysis() { return analysis; }
    public void setAnalysis(String analysis) { this.analysis = analysis; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public List<String> getAlternatives() { return alternatives; }
    public void setAlternatives(List<String> alternatives) { this.alternatives = alternatives; }

    public List<String> getTips() { return tips; }
    public void setTips(List<String> tips) { this.tips = tips; }

    public double getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(double confidenceScore) { this.confidenceScore = confidenceScore; }

    public List<ProduitDTO> getProduits() { return produits; }
    public void setProduits(List<ProduitDTO> produits) { this.produits = produits; }

    public List<Boutique> getBoutiques() { return boutiques; }
    public void setBoutiques(List<Boutique> boutiques) { this.boutiques = boutiques; }

    public Integer getNombreProduits() { return nombreProduits; }
    public void setNombreProduits(int nombreProduits) { this.nombreProduits = nombreProduits; }

    public Integer getNombreBoutiques() { return nombreBoutiques; }
    public void setNombreBoutiques(int nombreBoutiques) { this.nombreBoutiques = nombreBoutiques; }

    public List<RecommendationItem> getRecommendations() { return recommendations; }
    public void setRecommendations(List<RecommendationItem> recommendations) { this.recommendations = recommendations; }

    public String getVille() { return ville; }
    public void setVille(String ville) { this.ville = ville; }

    public Integer getRayonKm() { return rayonKm; }
    public void setRayonKm(Integer rayonKm) { this.rayonKm = rayonKm; }

    public long getProcessingTimeMs() { return processingTimeMs; }
    public void setProcessingTimeMs(long processingTimeMs) { this.processingTimeMs = processingTimeMs; }

    public String getAiModel() { return aiModel; }
    public void setAiModel(String aiModel) { this.aiModel = aiModel; }

    @Override
    public String toString() {
        return "RagSearchResponse{" +
                "query='" + query + '\'' +
                ", searchType='" + searchType + '\'' +
                ", ragEnhanced=" + ragEnhanced +
                ", totalResults=" + totalResults +
                ", nombreProduits=" + nombreProduits +
                ", nombreBoutiques=" + nombreBoutiques +
                '}';
    }
}


