package com.example.authentification.model;
import jakarta.persistence.*;
@Entity
@Table(name = "livreurs")
public class Livreur {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;  // Identifiant technique auto-généré pour PostgreSQL
    @Column(unique = true, nullable = false)
    private String idLivreur;  // Firebase UID
    private String nom;        // Nom du livreur
    private String role;       // Rôle de l'utilisateur, ex: "livreur"
    // Getters et Setters
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public String getIdLivreur() {
        return idLivreur;
    }
    public void setIdLivreur(String idLivreur) {
        this.idLivreur = idLivreur;
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