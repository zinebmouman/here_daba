package com.gestionstocks.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoutiqueDTO {
    private Long id;
    private String nom;
    private String idVendeur;
    private String adresse;
    private String telephone;
    private String email;
    private Boolean actif;
}