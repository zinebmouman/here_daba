package com.boutique_catalogue_produits.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProduitDTO {
    private Long id;
    private String nomProduit;
    private Integer quantite;
    private String description;
    private String detail;
    private Double seuilCritique;
    private BigDecimal prix;
    private LocalDate dateExpiration;
    private Long idStock;
    private String idCategorie;
    private Long idReduction;
    private String imageUrl;

    public String getImageUrl() {
        return imageUrl;
    }

    // Ajouter cette propriété
    private List<ProduitImageDTO> images = new ArrayList<>();

    public Long getIdStock() {
        return idStock;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setNomProduit(String nomProduit) {
        this.nomProduit = nomProduit;
    }

    public void setQuantite(Integer quantite) {
        this.quantite = quantite;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public void setSeuilCritique(Double seuilCritique) {
        this.seuilCritique = seuilCritique;
    }

    public void setPrix(BigDecimal prix) {
        this.prix = prix;
    }

    public void setDateExpiration(LocalDate dateExpiration) {
        this.dateExpiration = dateExpiration;
    }

    public void setIdCategorie(String idCategorie) {
        this.idCategorie = idCategorie;
    }

    public void setIdReduction(Long idReduction) {
        this.idReduction = idReduction;
    }

    public void setImages(List<ProduitImageDTO> images) {
        this.images = images;
    }

    public Long getId() {
        return id;
    }

    public String getNomProduit() {
        return nomProduit;
    }

    public Integer getQuantite() {
        return quantite;
    }

    public String getDescription() {
        return description;
    }

    public String getDetail() {
        return detail;
    }

    public Double getSeuilCritique() {
        return seuilCritique;
    }

    public BigDecimal getPrix() {
        return prix;
    }

    public LocalDate getDateExpiration() {
        return dateExpiration;
    }

    public String getIdCategorie() {
        return idCategorie;
    }

    public Long getIdReduction() {
        return idReduction;
    }

    public List<ProduitImageDTO> getImages() {
        return images;
    }

    public void setIdStock(Long idStock) {
        this.idStock = idStock;
    }

}