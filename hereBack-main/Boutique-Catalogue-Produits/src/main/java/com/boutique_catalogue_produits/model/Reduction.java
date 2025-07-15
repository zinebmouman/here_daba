package com.boutique_catalogue_produits.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;
@Entity
@Table(name = "reductions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Reduction implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public void setPourcentage_reduction(BigDecimal pourcentage_reduction) {
        this.pourcentage_reduction = pourcentage_reduction;
    }

    public void setActif(Boolean actif) {
        this.actif = actif;
    }

    public void setPeriode_debut(LocalDate periode_debut) {
        this.periode_debut = periode_debut;
    }

    public void setPeriode_fin(LocalDate periode_fin) {
        this.periode_fin = periode_fin;
    }

    public String getNom() {
        return nom;
    }

    public BigDecimal getPourcentage_reduction() {
        return pourcentage_reduction;
    }

    public Boolean getActif() {
        return actif;
    }

    public LocalDate getPeriode_debut() {
        return periode_debut;
    }

    public LocalDate getPeriode_fin() {
        return periode_fin;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Le nom de la réduction est obligatoire")
    @Size(max = 50, message = "Le nom ne doit pas dépasser 50 caractères")
    @Column(name = "nom", nullable = false, length = 50)
    private String nom;

    @NotNull(message = "Le pourcentage de réduction est obligatoire")
    @DecimalMin(value = "0.01", message = "Le pourcentage doit être supérieur à 0")
    @DecimalMax(value = "100.00", message = "Le pourcentage ne peut pas dépasser 100")
    @Column(name = "pourcentage_reduction", nullable = false, precision = 5, scale = 2)
    private BigDecimal pourcentage_reduction;

    @Column(name = "actif", nullable = false)
    private Boolean actif = true;

    @NotNull(message = "La date de début est obligatoire")
    @Column(name = "periode_debut", nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "UTC")
    private LocalDate periode_debut;

    @NotNull(message = "La date de fin est obligatoire")
    @Column(name = "periode_fin", nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "UTC")
    private LocalDate periode_fin;
}