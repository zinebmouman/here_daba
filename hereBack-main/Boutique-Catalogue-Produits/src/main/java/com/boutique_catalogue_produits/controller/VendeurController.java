package com.boutique_catalogue_produits.controller;

import com.boutique_catalogue_produits.client.StockServiceClient;
import com.boutique_catalogue_produits.dto.StockDTO;
import com.boutique_catalogue_produits.dto.VendeurDTO;
import com.boutique_catalogue_produits.model.Boutique;
import com.boutique_catalogue_produits.model.Produit;
import com.boutique_catalogue_produits.repository.BoutiqueRepository;
import com.boutique_catalogue_produits.repository.ProduitRepository;
import com.boutique_catalogue_produits.service.AuthServiceClient;
import com.boutique_catalogue_produits.service.BoutiqueService;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/vendeurs")
public class VendeurController {
    private static final Logger logger = LoggerFactory.getLogger(VendeurController.class);

    @Autowired
    private ProduitRepository produitRepository;

    @Autowired
    private StockServiceClient stockServiceClient;

    @Autowired
    private BoutiqueRepository boutiqueRepository;
    @Autowired
    private BoutiqueService boutiqueService;
    @Autowired
    private AuthServiceClient authServiceClient;
    @GetMapping("/{vendeurId}/boutiques")
    public ResponseEntity<List<Long>> getBoutiqueIdsByVendeurId(@PathVariable String vendeurId) {
        try {
            logger.info("Récupération des IDs de boutiques pour le vendeur: {}", vendeurId);

            // Récupérer les boutiques via le service BoutiqueService
            List<Boutique> boutiques = boutiqueService.getBoutiquesByVendeur(vendeurId);

            // Extraire uniquement les IDs des boutiques
            List<Long> boutiqueIds = boutiques.stream()
                    .map(boutique -> Long.valueOf(boutique.getId_boutique()))
                    .collect(Collectors.toList());

            return ResponseEntity.ok(boutiqueIds);
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération des IDs de boutiques pour le vendeur {}", vendeurId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.emptyList());
        }
    }
    @GetMapping("/produit/{produitId}")
    public ResponseEntity<VendeurDTO> getVendeurByProduit(@PathVariable Long produitId) {
        try {
            // 1. Trouver le produit
            Produit produit = produitRepository.findById(produitId)
                    .orElseThrow(() -> {
                        logger.warn("Produit non trouvé: {}", produitId);
                        return new EntityNotFoundException("Produit non trouvé");
                    });

            // 2. Vérifier si le stock existe
            if (produit.getIdStock() == null) {
                logger.warn("Produit {} sans stock associé", produitId);
                return ResponseEntity.ok(new VendeurDTO("1", "VENDEUR", "Vendeur Défaut", "admin@exemple.com"));
            }

            // 3. Récupérer les informations du stock
            StockDTO stockDTO = stockServiceClient.getStockById(produit.getIdStock());
            if (stockDTO == null || stockDTO.getIdBoutique() == null) {
                logger.warn("Stock non trouvé ou sans boutique pour le produit {}", produitId);
                return ResponseEntity.ok(new VendeurDTO("1", "VENDEUR", "Vendeur Défaut", "admin@exemple.com"));
            }

            // 4. Trouver la boutique
            Boutique boutique = boutiqueRepository.findById(stockDTO.getIdBoutique())
                    .orElseThrow(() -> {
                        logger.warn("Boutique non trouvée pour ID: {}", stockDTO.getIdBoutique());
                        return new EntityNotFoundException("Boutique non trouvée");
                    });

            // 5. Vérifier l'ID du vendeur
            if (boutique.getVendeurId() == null) {
                logger.warn("Boutique {} sans vendeur associé", boutique.getVendeurId());
                return ResponseEntity.ok(new VendeurDTO("1", "VENDEUR", "Vendeur Défaut", "admin@exemple.com"));
            }

            // 6. Récupérer les informations du vendeur
            VendeurDTO vendeur = authServiceClient.getVendeurById(boutique.getVendeurId());

            // Fallback si le vendeur n'est pas trouvé
            if (vendeur == null) {
                logger.warn("Vendeur non trouvé pour ID: {}", boutique.getVendeurId());
                return ResponseEntity.ok(new VendeurDTO("1", "VENDEUR", "Vendeur Défaut", "admin@exemple.com"));
            }

            return ResponseEntity.ok(vendeur);

        } catch (Exception e) {
            logger.error("Erreur lors de la recherche du vendeur pour le produit {}", produitId, e);
            return ResponseEntity.ok(new VendeurDTO("1", "VENDEUR", "Vendeur Défaut", "admin@exemple.com"));
        }
    }

    @GetMapping("/{vendeurId}")
    public ResponseEntity<VendeurDTO> getVendeurById(@PathVariable String vendeurId) {
        try {
            // Utiliser AuthServiceClient pour récupérer le vendeur
            VendeurDTO vendeur = authServiceClient.getVendeurById(vendeurId);

            // Vérifier si le vendeur existe
            if (vendeur == null) {
                logger.warn("Vendeur non trouvé pour ID: {}", vendeurId);
                return ResponseEntity.notFound().build();
            }

            // Retourner une réponse complète
            return ResponseEntity.ok(vendeur);

        } catch (Exception e) {
            logger.error("Erreur lors de la recherche du vendeur avec ID {}", vendeurId, e);
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{vendeurId}/email")
    public ResponseEntity<String> getVendeurEmail(@PathVariable String vendeurId) {
        try {
            VendeurDTO vendeur = authServiceClient.getVendeurById(vendeurId);

            if (vendeur == null || vendeur.getEmail() == null) {
                logger.warn("Email non trouvé pour le vendeur: {}", vendeurId);
                return ResponseEntity.ok("admin@exemple.com");
            }

            return ResponseEntity.ok(vendeur.getEmail());

        } catch (Exception e) {
            logger.error("Erreur lors de la récupération de l'email du vendeur", e);
            return ResponseEntity.ok("admin@exemple.com");
        }
    }
}