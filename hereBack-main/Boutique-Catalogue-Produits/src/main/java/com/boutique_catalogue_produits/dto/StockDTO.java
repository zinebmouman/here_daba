package com.boutique_catalogue_produits.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockDTO {
    private Long id;
    private Integer quantiteStockDisponible;
    private Integer capaciteMaximaleStock;
    private Integer idBoutique;
    private String name;
    private String location;

    public void setId(Long id) {
        this.id = id;
    }

    public void setQuantiteStockDisponible(Integer quantiteStockDisponible) {
        this.quantiteStockDisponible = quantiteStockDisponible;
    }

    public void setCapaciteMaximaleStock(Integer capaciteMaximaleStock) {
        this.capaciteMaximaleStock = capaciteMaximaleStock;
    }

    public void setIdBoutique(Integer idBoutique) {
        this.idBoutique = idBoutique;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Long getId() {
        return id;
    }

    public Integer getQuantiteStockDisponible() {
        return quantiteStockDisponible;
    }

    public Integer getCapaciteMaximaleStock() {
        return capaciteMaximaleStock;
    }

    public Integer getIdBoutique() {
        return idBoutique;
    }

    public String getName() {
        return name;
    }

    public String getLocation() {
        return location;
    }
}