package com.notificationsmessage.dto;

import java.io.Serializable;

public class VendeurDTO implements Serializable {
    private Long id;
    private String idVendeur;
    private String role;
    private String nom;
    private String email;

    // Constructeurs
    public VendeurDTO() {}

    public VendeurDTO(String idVendeur, String role) {
        this.idVendeur = idVendeur;
        this.role = role;
    }

    public VendeurDTO(String idVendeur, String role, String nom) {
        this.idVendeur = idVendeur;
        this.role = role;
        this.nom = nom;
    }

    public VendeurDTO(String idVendeur, String role, String nom, String email) {
        this.idVendeur = idVendeur;
        this.role = role;
        this.nom = nom;
        this.email = email;
    }

    // Getters et setters
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

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public String toString() {
        return "VendeurDTO{" +
                "id=" + id +
                ", idVendeur='" + idVendeur + '\'' +
                ", role='" + role + '\'' +
                ", nom='" + nom + '\'' +
                ", email='" + email + '\'' +
                '}';
    }
}