package com.boutique_catalogue_produits.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProduitImageDTO {
    private Long id;
    private String cheminFichier;
    private String url;
    private Boolean imagePrincipale;
    private LocalDateTime dateCreation;
    private Long produitId;

    public void setDateCreation(LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }

    public void setProduitId(Long produitId) {
        this.produitId = produitId;
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public Long getProduitId() {
        return produitId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setCheminFichier(String cheminFichier) {
        this.cheminFichier = cheminFichier;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setImagePrincipale(Boolean imagePrincipale) {
        this.imagePrincipale = imagePrincipale;
    }

    public String getCheminFichier() {
        return cheminFichier;
    }

    public String getUrl() {
        return url;
    }

    public Boolean getImagePrincipale() {
        return imagePrincipale;
    }


}