package com.gestionstocks.model;

import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "stocks")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Stock implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "capacite_maximale_stock", nullable = false)
    private Integer capaciteMaximaleStock;

    @Column(name = "id_boutique", nullable = false)
    private Long idBoutique;

    @Column(name = "location")
    private String location;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "quantite_stock_disponible")
    private Integer quantiteStockDisponible = 0;

    @Column(name = "date_creation")
    private LocalDateTime dateCreation;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut")
    private StatutStock statut;

    public enum StatutStock {
        DISPONIBLE,    // Stock avec suffisamment de produits
        CRITIQUE,      // Stock proche de la rupture
        RUPTURE        // Stock épuisé
    }

    // Méthode pour mettre à jour le statut automatiquement
    public void updateStatut() {
        if (quantiteStockDisponible <= 0) {
            this.statut = StatutStock.RUPTURE;
        } else if (quantiteStockDisponible <= (capaciteMaximaleStock * 0.1)) { // 10% ou moins de la capacité
            this.statut = StatutStock.CRITIQUE;
        } else {
            this.statut = StatutStock.DISPONIBLE;
        }
    }
}