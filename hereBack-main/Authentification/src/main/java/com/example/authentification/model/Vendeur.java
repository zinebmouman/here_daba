package com.example.authentification.model;

import jakarta.persistence.*;
@Entity
@Table(name = "vendeurs")
public class Vendeur {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;  // Identifiant technique auto-généré pour PostgreSQL
    @Column(unique = true, nullable = false)
    private String idVendeur;  // Firebase UID
    private String nom;        // Nom du vendeur
    private String role;
    // Nouveau champ email
    @Column(nullable = true)  // Pour permettre des valeurs null
    private String email;

    // Ajouter les getters et setters
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }// Rôle de l'utilisateur, ex: "vendeur"
    // Getters et Setters
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public String getIdVendeur() {
        return idVendeur;
    }
    public void setIdVendeur(String idVendeur) {
        this.idVendeur = idVendeur;
    }
    public String getNom() {
        return nom;
    }
    public void setNom(String nom) {
        this.nom = nom;
    }
    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}