package com.gestionstocks.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true) // Ceci est crucial
public class ProduitDTO {
    private Long id;
    private String nomProduit;
    private String description;
    private String detail;
    private BigDecimal prix;
    private Integer quantite;
    private Double seuilCritique; // Changé de Integer à Double
    private Long idStock;
    private LocalDate dateExpiration;
    private String idCategorie;
    private Long idReduction;
    private List<ProduitImageDTO> images = new ArrayList<>();
}