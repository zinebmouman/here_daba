package com.gestionstocks.dto;

import com.gestionstocks.model.Stock.StatutStock;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@NoArgsConstructor // Ajoutez cette annotation
@AllArgsConstructor
@Data
@Builder
public class StockDTO {
    private Long id;

    @NotBlank(message = "Le nom du stock ne peut pas être vide")
    private String name;

    private String location;

    @NotNull(message = "La quantité de stock disponible est requise")

    private Integer quantiteStockDisponible;

    @NotNull(message = "La capacité maximale du stock est requise")
    @Positive(message = "La capacité maximale du stock doit être positive")
    private Integer capaciteMaximaleStock;

    @NotNull(message = "L'ID de la boutique est requis")
    @Positive(message = "L'ID de la boutique doit être positif")
    private Long idBoutique;

    private LocalDateTime dateCreation;

    private StatutStock statut;

    // Méthode de validation supplémentaire
    public void validate() {
        if (quantiteStockDisponible == null || quantiteStockDisponible < 0) {
            throw new IllegalArgumentException("La quantité de stock disponible ne peut pas être négative");
        }
        if (capaciteMaximaleStock == null || capaciteMaximaleStock <= 0) {
            throw new IllegalArgumentException("La capacité maximale du stock doit être positive");
        }
        if (idBoutique == null || idBoutique <= 0) {
            throw new IllegalArgumentException("L'ID de la boutique est invalide");
        }
    }
}