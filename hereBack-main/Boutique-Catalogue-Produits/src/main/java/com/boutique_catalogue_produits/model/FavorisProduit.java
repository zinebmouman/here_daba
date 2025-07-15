package com.boutique_catalogue_produits.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "favoris_produit",
        uniqueConstraints = @UniqueConstraint(columnNames = {"id_client", "id_produit"}))
public class FavorisProduit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_client", nullable = false)
    private String idClient;  // Utilise l'ID Firebase de l'utilisateur

    @Column(name = "id_produit", nullable = false)
    private Long idProduit;

    @Column(name = "date_ajout")
    private LocalDateTime dateAjout;

    // Constructeurs
    public FavorisProduit() {
        this.dateAjout = LocalDateTime.now();
    }

    public FavorisProduit(String idClient, Long idProduit) {
        this.idClient = idClient;
        this.idProduit = idProduit;
        this.dateAjout = LocalDateTime.now();
    }

    // Getters et Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getIdClient() {
        return idClient;
    }

    public void setIdClient(String idClient) {
        this.idClient = idClient;
    }

    public Long getIdProduit() {
        return idProduit;
    }

    public void setIdProduit(Long idProduit) {
        this.idProduit = idProduit;
    }

    public LocalDateTime getDateAjout() {
        return dateAjout;
    }

    public void setDateAjout(LocalDateTime dateAjout) {
        this.dateAjout = dateAjout;
    }
}