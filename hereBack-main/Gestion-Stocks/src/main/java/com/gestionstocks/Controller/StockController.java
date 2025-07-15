package com.gestionstocks.Controller;

import com.gestionstocks.dto.StockDTO;
import com.gestionstocks.model.Stock.StatutStock;
import com.gestionstocks.service.StockService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/stocks")
public class StockController {
    private static final Logger logger = LoggerFactory.getLogger(StockController.class);
    @Autowired
    private final StockService stockService;
  ;
    @Autowired
    public StockController(StockService stockService) {
        this.stockService = stockService;
    }
    /**
     * Récupère l'ID du vendeur associé à un stock
     */
    @GetMapping("/{stockId}/vendeur")
    public ResponseEntity<String> getVendeurIdByStockId(@PathVariable Long stockId) {
        logger.info("GET /api/stocks/{}/vendeur - Récupération de l'ID vendeur", stockId);

        try {
            String vendeurId = stockService.getVendeurIdByStockId(stockId);
            logger.info("ID vendeur trouvé pour le stock {}: {}", stockId, vendeurId);
            return ResponseEntity.ok(vendeurId);
        } catch (EntityNotFoundException e) {
            logger.error("Stock non trouvé avec l'ID: {}", stockId);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération du vendeur pour le stock {}: {}",
                    stockId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur serveur: " + e.getMessage());
        }
    }
    /**
     * Récupère tous les stocks
     */
    @GetMapping
    public ResponseEntity<List<StockDTO>> getAllStocks() {
        logger.info("GET /api/stocks - Récupération de tous les stocks");
        try {
            List<StockDTO> stocks = stockService.getAllStocks();
            logger.info("Nombre de stocks récupérés: {}", stocks.size());
            return ResponseEntity.ok(stocks);
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération de tous les stocks: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.emptyList());
        }
    }
    /**
     * Récupère un stock par son ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<StockDTO> getStockById(@PathVariable Long id) {
        logger.info("GET /api/stocks/{} - Récupération du stock par ID", id);
        try {
            Optional<StockDTO> stock = stockService.getStockById(id);
            return stock.map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération du stock {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Récupère les stocks d'une boutique
     */
    @GetMapping("/boutique/{idBoutique}")
    public ResponseEntity<List<StockDTO>> getStocksByBoutique(@PathVariable Long idBoutique) {
        logger.info("GET /api/stocks/boutique/{} - Récupération des stocks par boutique", idBoutique);
        try {
            List<StockDTO> stocks = stockService.getStocksByBoutique(idBoutique);
            logger.info("Nombre de stocks trouvés pour la boutique {}: {}", idBoutique, stocks.size());
            return ResponseEntity.ok(stocks);
        } catch (EntityNotFoundException e) {
            logger.error("Boutique non trouvée: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Collections.emptyList());
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération des stocks pour la boutique {}: {}",
                    idBoutique, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.emptyList());
        }
    }
    /**
     * Récupère les stocks d'un vendeur
     */
    @GetMapping("/vendeur/{idVendeur}")
    public ResponseEntity<List<StockDTO>> getStocksByVendeur(@PathVariable String idVendeur) {
        try {
            logger.info("=== DÉBUT GET /api/stocks/vendeur/{} - Récupération des stocks par vendeur ===", idVendeur);

            if (idVendeur == null || idVendeur.trim().isEmpty()) {
                logger.warn("ID vendeur invalide fourni");
                return ResponseEntity.badRequest().body(Collections.emptyList());
            }

            List<StockDTO> stocks = stockService.getStocksByVendeur(idVendeur);
            logger.info("Nombre de stocks récupérés pour le vendeur {}: {}", idVendeur, stocks.size());

            logger.info("=== FIN GET /api/stocks/vendeur/{} ===", idVendeur);
            return ResponseEntity.ok(stocks);
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération des stocks pour le vendeur {}: {}",
                    idVendeur, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.emptyList());
        }
    }
    /**
     * Crée un nouveau stock
     */
    @PostMapping
    public ResponseEntity<?> createStock(@RequestBody StockDTO stockDTO) {
        logger.info("POST /api/stocks - Création d'un nouveau stock: {}", stockDTO);
        try {
            StockDTO createdStock = stockService.createStock(stockDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdStock);
        } catch (IllegalArgumentException e) {
            logger.error("Données invalides: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Erreur lors de la création du stock: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Une erreur interne est survenue lors de la création du stock"));
        }
    }
    /**
     * Met à jour un stock existant
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateStock(@PathVariable Long id, @RequestBody StockDTO stockDTO) {
        logger.info("PUT /api/stocks/{} - Mise à jour du stock: {}", id, stockDTO);
        try {
            stockDTO.setId(id); // Assurer la cohérence de l'ID
            StockDTO updatedStock = stockService.updateStock(id, stockDTO);
            return ResponseEntity.ok(updatedStock);
        } catch (EntityNotFoundException e) {
            logger.error("Stock non trouvé: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Erreur lors de la mise à jour du stock: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Une erreur interne est survenue lors de la mise à jour du stock"));
        }
    }
    /**
     * Supprime un stock
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStock(@PathVariable Long id) {
        logger.info("DELETE /api/stocks/{} - Suppression du stock", id);
        try {
            stockService.deleteStock(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            logger.error("Erreur lors de la suppression du stock: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    /**
     * Ajuste la quantité de stock
     */
    @PatchMapping("/{id}/ajuster")
    public ResponseEntity<StockDTO> adjustStockQuantity(
            @PathVariable Long id,
            @RequestParam Integer quantite) {
        logger.info("PATCH /api/stocks/{}/ajuster - Ajustement de la quantité", id);
        StockDTO updatedStock = stockService.adjustStockQuantity(id, quantite);
        return ResponseEntity.ok(updatedStock);
    }

    /**
     * Recherche de stocks avec filtres
     */
    @GetMapping("/recherche")
    public ResponseEntity<List<StockDTO>> rechercherStocks(
            @RequestParam String idVendeur,
            @RequestParam(required = false) Integer minQuantite,
            @RequestParam(required = false) Integer maxQuantite,
            @RequestParam(required = false) StatutStock statut) {
        logger.info("GET /api/stocks/recherche - Recherche de stocks");
        List<StockDTO> stocks = stockService.rechercerStocks(idVendeur, minQuantite, maxQuantite, statut);
        return ResponseEntity.ok(stocks);
    }

    /**
     * Vérifie les stocks critiques
     */
    @GetMapping("/critiques")
    public ResponseEntity<List<StockDTO>> checkLowStocks() {
        logger.info("GET /api/stocks/critiques - Vérification des stocks critiques");
        List<StockDTO> lowStocks = stockService.checkLowStocks();
        return ResponseEntity.ok(lowStocks);
    }

    /**
     * Récupère les détails complets d'un stock
     */
    @GetMapping("/{id}/details")
    public ResponseEntity<StockDTO> getFullStockDetails(@PathVariable Long id) {
        logger.info("GET /api/stocks/{}/details - Récupération des détails complets", id);
        StockDTO stockDetails = stockService.getFullStockDetails(id);
        return ResponseEntity.ok(stockDetails);
    }
}