package com.example.authentification.dto;

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

    // Constructeur avec tous les paramètres
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

    // Getters et Setters
    public String getVendeurId() {
        return vendeurId;
    }

    public void setVendeurId(String vendeurId) {
        this.vendeurId = vendeurId;
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
}