package com.boutique_catalogue_produits.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

public class VendeurDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String vendeurId;
    private String role;
    private String nom;
    private String email;

    // Constructeur par défaut
    public VendeurDTO() {
    }

    // Constructeur avec tous les paramètres et annotations Jackson
    @JsonCreator
    public VendeurDTO(
            @JsonProperty("vendeurId") String vendeurId,
            @JsonProperty("role") String role,
            @JsonProperty("nom") String nom,
            @JsonProperty("email") String email
    ) {
        this.vendeurId = vendeurId;
        this.role = role;
        this.nom = nom;
        this.email = email;
    }

    // Getters avec JsonProperty pour la désérialisation
    @JsonProperty("vendeurId")
    public String getVendeurId() {
        return vendeurId;
    }

    @JsonProperty("role")
    public String getRole() {
        return role;
    }

    @JsonProperty("nom")
    public String getNom() {
        return nom;
    }

    @JsonProperty("email")
    public String getEmail() {
        return email;
    }

    // Setters
    public void setVendeurId(String vendeurId) {
        this.vendeurId = vendeurId;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}