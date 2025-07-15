package com.boutique_catalogue_produits.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "produit_images")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProduitImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chemin_fichier", nullable = false)
    private String cheminFichier;

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

    public void setDateCreation(LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }

    public void setProduit(Produit produit) {
        this.produit = produit;
    }

    public Long getId() {
        return id;
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

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public Produit getProduit() {
        return produit;
    }

    @Column(name = "url", nullable = false)
    private String url;

    @Column(name = "image_principale")
    private Boolean imagePrincipale = false;

    @Column(name = "date_creation")
    private LocalDateTime dateCreation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produit_id")
    private Produit produit;

    // Constructeur personnalisé pour initialiser la date de création
    @PrePersist
    protected void onCreate() {
        if (dateCreation == null) {
            dateCreation = LocalDateTime.now();
        }
        if (imagePrincipale == null) {
            imagePrincipale = false;
        }
    }
}