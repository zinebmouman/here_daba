package com.boutique_catalogue_produits.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReductionDto {
    private Long idReduction;
    private String nom;
    private BigDecimal pourcentageReduction;
    private Boolean actif;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private String periode;

    // Champ optionnel pour lier à un produit spécifique dans l'interface
    private String idProduit;
}