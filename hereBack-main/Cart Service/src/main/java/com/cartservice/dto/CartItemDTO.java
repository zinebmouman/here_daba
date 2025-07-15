package com.cartservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartItemDTO {
    private Long id;
    private String nomProduit;
    private Double prix;
    private String imageUrl;
    private Integer quantite;
    private String categorie;
    private Long productId;
}