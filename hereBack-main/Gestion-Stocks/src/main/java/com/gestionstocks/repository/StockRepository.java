package com.gestionstocks.repository;

import com.gestionstocks.model.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockRepository extends JpaRepository<Stock, Long> {

    /**
     * Trouve tous les stocks appartenant à une boutique
     */
    List<Stock> findByIdBoutique(Long idBoutique);

    /**
     * Trouve tous les stocks avec un statut critique ou en rupture
     */
    List<Stock> findByStatutIn(List<Stock.StatutStock> statuts);

    /**
     * Vérifie si un stock existe pour une boutique spécifique
     */
    boolean existsByIdBoutique(Long idBoutique);

    /**
     * Trouve les stocks pour une liste de boutiques
     */
    List<Stock> findByIdBoutiqueIn(List<Long> idBoutiques);
}