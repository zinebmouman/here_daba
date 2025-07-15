package com.example.authentification.model;
import jakarta.persistence.*;
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;  // Identifiant technique auto-généré pour PostgreSQL
    @Column(unique = true, nullable = false)
    private String idUser;  // Firebase UID
    private String nom;     // Nom de l'utilisateur
    private String role;    // Rôle principal de l'utilisateur
    // Flags pour les rôles multiples
    private boolean isVendeur = false;
    private boolean isLivreur = false;
    private boolean isClient = true;  // Par défaut, tous les utilisateurs sont des clients
    // Getters et Setters
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public String getIdUser() {
        return idUser;
    }
    public void setIdUser(String idUser) {
        this.idUser = idUser;
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
    public boolean isVendeur() {
        return isVendeur;
    }
    public void setVendeur(boolean vendeur) {
        isVendeur = vendeur;
    }
    public boolean isLivreur() {
        return isLivreur;
    }
    public void setLivreur(boolean livreur) {
        isLivreur = livreur;
    }
    public boolean isClient() {
        return isClient;
    }
    public void setClient(boolean client) {
        isClient = client;
    }
}