package com.gestionstocks.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProduitImageDTO {
    private Long id;
    private String cheminFichier;
    private String url;
    private Boolean imagePrincipale;
}