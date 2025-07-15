package com.boutique_catalogue_produits.dto;

public class SearchCriteria {
    // Critères PRODUITS
    private String keywords;
    private Double prixMin;
    private Double prixMax;
    private String categorie;
    private Integer quantiteMin;
    private String description;

    // Critères BOUTIQUES
    private String boutiqueName;
    private Long boutiqueId;
    private String vendeurId;
    private String vendeurName;

    // Critères GÉOLOCALISATION
    private String ville;
    private String quartier;
    private String adresse;
    private String pays;
    private Double latitude;
    private Double longitude;
    private Integer rayonKm;

    // Critères GÉNÉRAUX
    private String searchType; // "PRODUIT", "BOUTIQUE", "MIXED"
    private Boolean enStock;
    private String marque;

    // Constructeur
    public SearchCriteria() {}

    // Getters et Setters PRODUITS
    public String getKeywords() { return keywords; }
    public void setKeywords(String keywords) { this.keywords = keywords; }

    public Double getPrixMin() { return prixMin; }
    public void setPrixMin(Double prixMin) { this.prixMin = prixMin; }

    public Double getPrixMax() { return prixMax; }
    public void setPrixMax(Double prixMax) { this.prixMax = prixMax; }

    public String getCategorie() { return categorie; }
    public void setCategorie(String categorie) { this.categorie = categorie; }

    public Integer getQuantiteMin() { return quantiteMin; }
    public void setQuantiteMin(Integer quantiteMin) { this.quantiteMin = quantiteMin; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    // Getters et Setters BOUTIQUES
    public String getBoutiqueName() { return boutiqueName; }
    public void setBoutiqueName(String boutiqueName) { this.boutiqueName = boutiqueName; }

    public Long getBoutiqueId() { return boutiqueId; }
    public void setBoutiqueId(Long boutiqueId) { this.boutiqueId = boutiqueId; }

    public String getVendeurId() { return vendeurId; }
    public void setVendeurId(String vendeurId) { this.vendeurId = vendeurId; }

    public String getVendeurName() { return vendeurName; }
    public void setVendeurName(String vendeurName) { this.vendeurName = vendeurName; }

    // Getters et Setters GÉOLOCALISATION
    public String getVille() { return ville; }
    public void setVille(String ville) { this.ville = ville; }

    public String getQuartier() { return quartier; }
    public void setQuartier(String quartier) { this.quartier = quartier; }

    public String getAdresse() { return adresse; }
    public void setAdresse(String adresse) { this.adresse = adresse; }

    public String getPays() { return pays; }
    public void setPays(String pays) { this.pays = pays; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public Integer getRayonKm() { return rayonKm; }
    public void setRayonKm(Integer rayonKm) { this.rayonKm = rayonKm; }

    // Getters et Setters GÉNÉRAUX
    public String getSearchType() { return searchType; }
    public void setSearchType(String searchType) { this.searchType = searchType; }

    public Boolean getEnStock() { return enStock; }
    public void setEnStock(Boolean enStock) { this.enStock = enStock; }

    public String getMarque() { return marque; }
    public void setMarque(String marque) { this.marque = marque; }

    @Override
    public String toString() {
        return "SearchCriteria{" +
                "keywords='" + keywords + '\'' +
                ", prixMin=" + prixMin +
                ", prixMax=" + prixMax +
                ", categorie='" + categorie + '\'' +
                ", boutiqueName='" + boutiqueName + '\'' +
                ", ville='" + ville + '\'' +
                ", searchType='" + searchType + '\'' +
                ", enStock=" + enStock +
                '}';
    }
}