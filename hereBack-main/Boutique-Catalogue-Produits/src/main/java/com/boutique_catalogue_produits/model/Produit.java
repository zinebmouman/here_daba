package com.boutique_catalogue_produits.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "produits")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Produit {
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

    public void setIdStock(Long idStock) {
        this.idStock = idStock;
    }

    public void setIdCategorie(String idCategorie) {
        this.idCategorie = idCategorie;
    }

    public void setIdReduction(Long idReduction) {
        this.idReduction = idReduction;
    }

    public void setImages(Set<ProduitImage> images) {
        this.images = images;
    }

    public void setCategories(Set<Categorie> categories) {
        this.categories = categories;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nom_produit", nullable = false)
    private String nomProduit;

    @Column(name = "quantite")
    private Integer quantite;

    @Column(name = "description")
    private String description;

    @Column(name = "detail", columnDefinition = "TEXT")
    private String detail;

    @Column(name = "seuil_critique")
    private Double seuilCritique;

    @Column(name = "prix", nullable = false)
    private BigDecimal prix;

    @Column(name = "date_expiration")
    private LocalDate dateExpiration;

    @Column(name = "id_stock")
    private Long idStock;

    @Column(name = "id_categorie")
    private String idCategorie;

    @Column(name = "id_reduction")
    private Long idReduction;

    // Utilisation de Set au lieu de List pour éviter les doublons
    @OneToMany(mappedBy = "produit", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<ProduitImage> images = new HashSet<>();

    @ManyToMany(mappedBy = "produits", fetch = FetchType.LAZY)
    private Set<Categorie> categories = new HashSet<>();

    // Méthodes utilitaires pour la gestion des images
    public void addImage(ProduitImage image) {
        images.add(image);
        image.setProduit(this);
    }

    public void removeImage(ProduitImage image) {
        images.remove(image);
        image.setProduit(null);
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

    public Long getIdStock() {
        return idStock;
    }

    public String getIdCategorie() {
        return idCategorie;
    }

    public Long getIdReduction() {
        return idReduction;
    }

    public Set<ProduitImage> getImages() {
        return images;
    }

    public Set<Categorie> getCategories() {
        return categories;
    }
}