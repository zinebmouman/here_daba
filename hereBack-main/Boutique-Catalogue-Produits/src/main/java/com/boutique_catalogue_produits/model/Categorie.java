package com.boutique_catalogue_produits.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Data
@Table(name = "categorie")
public class Categorie {

    public String getIdCategorie() {
        return idCategorie;
    }

    public String getNom() {
        return nom;
    }

    public void setIdCategorie(String idCategorie) {
        this.idCategorie = idCategorie;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public void setCustomIcon(String customIcon) {
        this.customIcon = customIcon;
    }

    public void setProduits(Set<Produit> produits) {
        this.produits = produits;
    }

    public String getDescription() {
        return description;
    }

    public String getIcon() {
        return icon;
    }

    public String getCustomIcon() {
        return customIcon;
    }

    public Set<Produit> getProduits() {
        return produits;
    }

    @Id
    @Column(name = "id_categorie")
    private String idCategorie;

    @Column(name = "nom", nullable = false, length = 50)
    private String nom;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "icon", length = 50)
    private String icon;

    @Column(name = "custom_icon", columnDefinition = "TEXT")
    private String customIcon;

    // Modifiez cette ligne pour permettre null temporairement
    // pendant que vous migrez les données
    // Cette relation many-to-many semble redondante avec la colonne id_boutique
    // mais vous pouvez la garder pour la compatibilité

    // La relation avec les produits reste inchangée
    @JsonIgnore
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "categorie_produit",
            joinColumns = @JoinColumn(name = "id_categorie"),
            inverseJoinColumns = @JoinColumn(name = "id_produit")
    )
    private Set<Produit> produits = new HashSet<>();

    // Méthodes utilitaires pour gérer la relation avec Produit
    public void addProduit(Produit produit) {
        this.produits.add(produit);
        produit.getCategories().add(this);
    }

    public void removeProduit(Produit produit) {
        this.produits.remove(produit);
        produit.getCategories().remove(this);
    }
    @JsonIgnore
    @ManyToMany(mappedBy = "categories")
    private Set<Boutique> boutiques = new HashSet<>();


    public Set<Boutique> getBoutiques() {
        return boutiques;
    }

    public void setBoutiques(Set<Boutique> boutiques) {
        this.boutiques = boutiques;
    }

}