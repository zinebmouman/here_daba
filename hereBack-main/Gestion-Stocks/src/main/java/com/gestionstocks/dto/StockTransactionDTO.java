package com.gestionstocks.dto;

import com.gestionstocks.model.StockTransaction.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockTransactionDTO {
    private Long id;
    private Long productId;
    private Long stockId;
    private TransactionType type;
    private Integer quantity;
    private LocalDateTime transactionDate;
    private BigDecimal prixUnitaire;
    private BigDecimal revenuTotal;
    private String notes;
}