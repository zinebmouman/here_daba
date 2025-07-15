package com.gestionstocks.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gestionstocks.dto.ProduitDTO;
import com.gestionstocks.exception.ResourceNotFoundException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@FeignClient(name = "BOUTIQUE-CATALOGUE-PRODUITS")
public interface ProduitServiceClient {
    // Ajouter un logger statique
    Logger logger = LoggerFactory.getLogger(ProduitServiceClient.class);

    @GetMapping("/api/produits/stock/{idStock}")
    @CircuitBreaker(name = "produitService", fallbackMethod = "getProduitsByStockFallback")
    List<ProduitDTO> getProduitsByStock(@PathVariable("idStock") Long idStock);

    default List<ProduitDTO> getProduitsByStockFallback(Long idStock, Exception e) {
        logger.error("Impossible de récupérer les produits du stock {}: {}", idStock, e.getMessage());
        // Retourner une liste vide au lieu de lever une exception
        return new ArrayList<>();
    }

    @GetMapping("/api/produits/en-alerte")
    @CircuitBreaker(name = "produitService", fallbackMethod = "getProduitsEnAlerteFallback")
    List<ProduitDTO> getProduitsEnAlerte();

    default List<ProduitDTO> getProduitsEnAlerteFallback(Exception e) {
        logger.error("Erreur lors de la récupération des produits en alerte: {}", e.getMessage());
        // Retourner une liste vide
        return new ArrayList<>();
    }

    @GetMapping("/api/produits/proche-expiration")
    @CircuitBreaker(name = "produitService", fallbackMethod = "getProduitsProcheExpirationFallback")
    List<ProduitDTO> getProduitsProcheExpiration();

    default List<ProduitDTO> getProduitsProcheExpirationFallback(Exception e) {
        logger.error("Erreur lors de la récupération des produits proches de l'expiration: {}", e.getMessage());
        // Retourner une liste vide
        return new ArrayList<>();
    }



    // Autres méthodes similaires...
    @PutMapping("/api/produits/{id}/ajuster-stock")
    @CircuitBreaker(name = "produitService", fallbackMethod = "ajusterStockFallback")
    ProduitDTO ajusterStock(@PathVariable("id") Long id, @RequestParam("quantiteAjustement") Integer quantiteAjustement);

    default ProduitDTO ajusterStockFallback(Long id, Integer quantiteAjustement, Exception e) {
        logger.error("Erreur lors de l'ajustement du stock pour le produit {}: {}", id, e.getMessage());
        throw new ResourceNotFoundException("Impossible d'ajuster le stock du produit: " + id);
    }

    @GetMapping("/api/produits/{id}")
    @CircuitBreaker(name = "produitService", fallbackMethod = "getProduitByIdFallback")
    ProduitDTO getProduitById(@PathVariable("id") Long id);

    default ProduitDTO getProduitByIdFallback(Long id, Exception e) {
        logger.error("Impossible de récupérer le produit avec l'ID: {} - Erreur: {}", id, e.getMessage());

        ProduitDTO fallbackProduit = new ProduitDTO();
        fallbackProduit.setId(id);
        fallbackProduit.setNomProduit("Produit Inconnu");
        fallbackProduit.setQuantite(0);
        fallbackProduit.setPrix(BigDecimal.ZERO);
        fallbackProduit.setSeuilCritique(0.0);
        fallbackProduit.setDescription("Produit non disponible");
        fallbackProduit.setIdStock(null);

        return fallbackProduit;
    }
    @DeleteMapping("/api/produits/{id}")
    @CircuitBreaker(name = "produitService", fallbackMethod = "deleteProduitFallback")
    void deleteProduit(@PathVariable("id") Long id);
    @PutMapping("/api/produits/{id}/stock")
    void updateProduitStock(@PathVariable Long id, @RequestParam Long newStockId);
    default void deleteProduitFallback(Long id, Exception e) {
        logger.error("Impossible de supprimer le produit {}: {}", id, e.getMessage());
        throw new RuntimeException("Impossible de supprimer le produit " + id);}
}