package com.gestionstocks.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "stock_transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockTransaction {
    public enum TransactionType {
        ADD,    // Ajout de stock
        REMOVE  // Retrait de stock
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "stock_id", nullable = false)
    private Long stockId;


    @Enumerated(EnumType.STRING)
    @Column(name = "type") // Au lieu de transaction_type
    private TransactionType type;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "transaction_date", nullable = false)
    private LocalDateTime transactionDate;

    @Column(name = "prix_unitaire", precision = 10, scale = 2)
    private BigDecimal prixUnitaire;

    @Column(name = "revenu_total", precision = 10, scale = 2)
    private BigDecimal revenuTotal;

    @Column(name = "notes")
    private String notes;
}