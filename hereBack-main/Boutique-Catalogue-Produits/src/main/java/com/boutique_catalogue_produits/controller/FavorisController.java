package com.boutique_catalogue_produits.controller;

import com.boutique_catalogue_produits.service.FavorisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/favoris")
public class FavorisController {
    // Déclaration du logger en haut de la classe
    private static final Logger logger = LoggerFactory.getLogger(FavorisController.class);

    @Autowired
    private FavorisService favorisService;
    @GetMapping("/check/{productId}/user/{userId}")
    public ResponseEntity<?> checkFavorite(@PathVariable Long productId, @PathVariable String userId) {
        logger.info("Vérification si le produit {} est dans les favoris de l'utilisateur {}", productId, userId);
        try {
            // Utiliser estFavori au lieu de isProductInFavorites
            boolean isFavorite = favorisService.estFavori(userId, productId);
            logger.info("Résultat de la vérification: {}", isFavorite);
            return ResponseEntity.ok(Map.of("isFavorite", isFavorite));
        } catch (Exception e) {
            logger.error("Erreur lors de la vérification des favoris", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Récupérer tous les favoris d'un utilisateur
     */
    @GetMapping("/user/{idUser}")
    public ResponseEntity<?> getFavorisByUser(@PathVariable String idUser) {
        try {
            List<Map<String, Object>> favoris = favorisService.getFavorisByUser(idUser);
            return ResponseEntity.ok(favoris);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de la récupération des favoris: " + e.getMessage());
        }
    }

    /**
     * Ajouter un produit aux favoris
     */
    @PostMapping
    public ResponseEntity<?> ajouterFavori(@RequestBody Map<String, Object> favorisData) {
        try {
            // Extraire les données de la requête
            Object idUserObj = favorisData.get("idUser");
            String idUser = String.valueOf(idUserObj);

            Object idProduitObj = favorisData.get("idProduit");

            // Conversion de l'ID produit selon son type
            Long idProduit;
            if (idProduitObj instanceof Integer) {
                idProduit = Long.valueOf((Integer) idProduitObj);
            } else if (idProduitObj instanceof String) {
                idProduit = Long.valueOf((String) idProduitObj);
            } else if (idProduitObj instanceof Long) {
                idProduit = (Long) idProduitObj;
            } else {
                return ResponseEntity.badRequest().body("Format d'ID produit invalide");
            }

            System.out.println("Ajout du produit " + idProduit + " aux favoris de l'utilisateur " + idUser);

            Map<String, Object> favori = favorisService.ajouterFavori(idUser, idProduit);
            return ResponseEntity.status(HttpStatus.CREATED).body(favori);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de l'ajout aux favoris: " + e.getMessage());
        }
    }

    /**
     * Supprimer un produit des favoris
     */
    @DeleteMapping("/produit/{idProduit}/user/{idUser}")
    public ResponseEntity<?> supprimerFavori(
            @PathVariable String idUser,
            @PathVariable Long idProduit) {
        try {
            favorisService.supprimerFavori(idUser, idProduit);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Produit supprimé des favoris avec succès");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de la suppression des favoris: " + e.getMessage());
        }
    }

    /**
     * Vérifier si un produit est dans les favoris d'un utilisateur
     */
    @GetMapping("/produit/{idProduit}/user/{idUser}")
    public ResponseEntity<?> estFavori(
            @PathVariable String idUser,
            @PathVariable Long idProduit) {
        try {
            boolean estFavori = favorisService.estFavori(idUser, idProduit);

            Map<String, Object> response = new HashMap<>();
            response.put("favori", estFavori);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de la vérification du favori: " + e.getMessage());
        }
    }

    /**
     * Obtenir les statistiques des favoris pour un produit
     */
    @GetMapping("/stats/produit/{idProduit}")
    public ResponseEntity<?> getStatsProduit(@PathVariable Long idProduit) {
        try {
            Map<String, Object> stats = favorisService.getStatsProduit(idProduit);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de la récupération des statistiques: " + e.getMessage());
        }
    }
}