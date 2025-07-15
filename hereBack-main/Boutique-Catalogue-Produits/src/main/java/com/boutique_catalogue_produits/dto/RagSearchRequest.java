package com.boutique_catalogue_produits.dto;

public class RagSearchRequest {
    private String query;
    private String searchType;
    private boolean enableRAG;
    private String ville;
    private Integer rayonKm;
    private Double prixMin;
    private Double prixMax;
    private String categorie;

    public RagSearchRequest() {}

    public RagSearchRequest(String query, String searchType) {
        this.query = query;
        this.searchType = searchType;
        this.enableRAG = true;
    }

    // Getters et Setters
    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }

    public String getSearchType() { return searchType; }
    public void setSearchType(String searchType) { this.searchType = searchType; }

    public boolean isEnableRAG() { return enableRAG; }
    public void setEnableRAG(boolean enableRAG) { this.enableRAG = enableRAG; }

    public String getVille() { return ville; }
    public void setVille(String ville) { this.ville = ville; }

    public Integer getRayonKm() { return rayonKm; }
    public void setRayonKm(Integer rayonKm) { this.rayonKm = rayonKm; }

    public Double getPrixMin() { return prixMin; }
    public void setPrixMin(Double prixMin) { this.prixMin = prixMin; }

    public Double getPrixMax() { return prixMax; }
    public void setPrixMax(Double prixMax) { this.prixMax = prixMax; }

    public String getCategorie() { return categorie; }
    public void setCategorie(String categorie) { this.categorie = categorie; }
}
