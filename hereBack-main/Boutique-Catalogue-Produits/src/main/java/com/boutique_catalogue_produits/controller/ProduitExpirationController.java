package com.boutique_catalogue_produits.controller;

import com.boutique_catalogue_produits.dto.ProduitDTO;
import com.boutique_catalogue_produits.service.ProductExpirationChecker;
import com.boutique_catalogue_produits.service.ProduitService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/produits/expiration")
public class ProduitExpirationController {
    private static final Logger logger = LoggerFactory.getLogger(ProduitExpirationController.class);

    @Autowired
    private ProductExpirationChecker expirationChecker;

    @Autowired
    private ProduitService produitService;

    /**
     * Endpoint pour récupérer tous les produits proches de l'expiration
     */
    @GetMapping
    public ResponseEntity<List<ProduitDTO>> getProduitsExpirants() {
        logger.info("Requête pour obtenir les produits proches de l'expiration");
        List<ProduitDTO> produits = produitService.getProduitsProcheExpiration();
        return ResponseEntity.ok(produits);
    }

    /**
     * Endpoint pour déclencher manuellement la vérification des produits expirants
     * Utile pour tester la fonctionnalité sans attendre la planification
     */
    @PostMapping("/check")
    public ResponseEntity<String> triggerExpirationCheck() {
        try {
            logger.info("Déclenchement manuel de la vérification des produits expirants");
            expirationChecker.checkProductsNearExpiration();
            return ResponseEntity.ok("Vérification des expirations déclenchée avec succès");
        } catch (Exception e) {
            logger.error("Erreur lors de la vérification manuelle des expirations", e);
            return ResponseEntity.internalServerError()
                    .body("Erreur lors de la vérification des expirations: " + e.getMessage());
        }
    }
}