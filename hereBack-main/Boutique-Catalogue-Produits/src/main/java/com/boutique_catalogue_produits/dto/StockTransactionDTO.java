package com.boutique_catalogue_produits.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockTransactionDTO {
    private Long id;
    private Long productId;
    private Long stockId;
    private String type;
    private Integer quantity;
    private LocalDateTime transactionDate;
    private String notes;
}
