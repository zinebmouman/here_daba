package com.gestionstocks.Controller;

import com.gestionstocks.dto.StockTransactionDTO;
import com.gestionstocks.model.StockTransaction;
import com.gestionstocks.model.StockTransaction.TransactionType;
import com.gestionstocks.service.StockTransactionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/stock-transactions")
public class StockTransactionController {
    private static final Logger logger = LoggerFactory.getLogger(StockTransactionController.class);

    @Autowired
    private StockTransactionService transactionService;

    /**
     * Récupère toutes les transactions de stock
     */
    @GetMapping
    public ResponseEntity<List<StockTransactionDTO>> getAllTransactions() {
        logger.info("GET /api/stock-transactions - Récupération de toutes les transactions");
        List<StockTransactionDTO> transactions = transactionService.getAllTransactions();
        return ResponseEntity.ok(transactions);
    }

    /**
     * Récupère une transaction par son ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<StockTransactionDTO> getTransactionById(@PathVariable Long id) {
        logger.info("GET /api/stock-transactions/{} - Récupération transaction par ID", id);
        return transactionService.getTransactionById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Récupère les transactions pour un produit
     */
    @GetMapping("/produit/{productId}")
    public ResponseEntity<List<StockTransactionDTO>> getTransactionsByProductId(@PathVariable Long productId) {
        logger.info("Recherche des transactions pour le produit ID: {}", productId);
        List<StockTransactionDTO> transactions = transactionService.getTransactionsByProductId(productId);
        return ResponseEntity.ok(transactions);
    }
    /**
     * Récupère les transactions pour un stock
     */
    @GetMapping("/stock/{stockId}")
    public ResponseEntity<List<StockTransactionDTO>> getTransactionsByStockId(@PathVariable Long stockId) {
        logger.info("GET /api/stock-transactions/stock/{} - Récupération transactions par stock", stockId);
        List<StockTransactionDTO> transactions = transactionService.getTransactionsByStockId(stockId);
        return ResponseEntity.ok(transactions);
    }

    /**
     * Récupère les transactions pour un produit dans un stock spécifique
     */
    @GetMapping("/produit/{productId}/stock/{stockId}")
    public ResponseEntity<List<StockTransactionDTO>> getTransactionsByProductAndStock(
            @PathVariable Long productId,
            @PathVariable Long stockId) {
        logger.info("GET /api/stock-transactions/produit/{}/stock/{} - Récupération transactions par produit et stock",
                productId, stockId);
        List<StockTransactionDTO> transactions = transactionService.getTransactionsByProductAndStock(productId, stockId);
        return ResponseEntity.ok(transactions);
    }

    /**
     * Crée une nouvelle transaction de stock
     */
    @PostMapping
    public ResponseEntity<?> createTransaction(@RequestBody Map<String, Object> requestBody) {
        try {
            // Validation des paramètres obligatoires
            Long productId = parseAsLong(requestBody.get("productId"), "productId");
            Long stockId = parseAsLong(requestBody.get("stockId"), "stockId");
            String typeStr = requestBody.get("type").toString();
            Integer quantity = parseAsInteger(requestBody.get("quantity"), "quantity");

            // Valider le type de transaction
            TransactionType type = TransactionType.valueOf(typeStr.toUpperCase());

            // Extraction des paramètres optionnels
            String notes = Optional.ofNullable(requestBody.get("notes"))
                    .map(Object::toString)
                    .orElse(null);

            BigDecimal prix = null;
            if (requestBody.containsKey("prix") && requestBody.get("prix") != null) {
                try {
                    prix = new BigDecimal(requestBody.get("prix").toString());
                } catch (Exception e) {
                    logger.warn("Format de prix invalide, utilisation du prix par défaut");
                }
            }

            // Créer la transaction
            StockTransactionDTO transaction = transactionService.createTransaction(
                    productId, stockId, type, quantity, notes, prix);

            return ResponseEntity.status(HttpStatus.CREATED).body(transaction);

        } catch (IllegalArgumentException e) {
            logger.error("Erreur de validation: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "error", "Données invalides",
                            "message", e.getMessage()
                    ));
        } catch (RuntimeException e) {
            logger.error("Erreur lors de la création de la transaction", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "Erreur serveur",
                            "message", e.getMessage()
                    ));
        }
    }
    // Méthodes de parsing sécurisées
    private Long parseAsLong(Object value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " ne peut pas être null");
        }
        try {
            // Gérer différents types d'entrée potentiels
            if (value instanceof Long) {
                return (Long) value;
            } else if (value instanceof Integer) {
                return ((Integer) value).longValue();
            } else if (value instanceof String) {
                return Long.parseLong((String) value);
            }
            throw new IllegalArgumentException("Format invalide pour " + fieldName);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Format invalide pour " + fieldName);
        }
    }

    private Integer parseAsInteger(Object value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " ne peut pas être null");
        }
        try {
            // Gérer différents types d'entrée potentiels
            if (value instanceof Integer) {
                return (Integer) value;
            } else if (value instanceof Long) {
                return ((Long) value).intValue();
            } else if (value instanceof String) {
                return Integer.parseInt((String) value);
            }
            throw new IllegalArgumentException("Format invalide pour " + fieldName);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Format invalide pour " + fieldName);
        }
    }  /**
     * Récupère le revenu total par jour pour un stock
     */
    @GetMapping("/stock/{stockId}/daily-revenue")
    public ResponseEntity<Map<LocalDate, BigDecimal>> getDailyRevenueForStock(
            @PathVariable Long stockId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        logger.info("GET /api/stock-transactions/stock/{}/daily-revenue - Récupération revenu quotidien", stockId);
        Map<LocalDate, BigDecimal> dailyRevenue = transactionService.getDailyRevenueForStock(stockId);
        return ResponseEntity.ok(dailyRevenue);
    }

    /**
     * Calcule le revenu total pour un produit
     */
    @GetMapping("/produit/{productId}/revenue")
    public ResponseEntity<BigDecimal> calculateTotalRevenueForProduct(@PathVariable Long productId) {
        logger.info("GET /api/stock-transactions/produit/{}/revenue - Calcul du revenu total", productId);
        BigDecimal totalRevenue = transactionService.calculateTotalRevenueForProduct(productId);
        return ResponseEntity.ok(totalRevenue);
    }

    /**
     * Calcule le revenu total pour un stock
     */
    @GetMapping("/stock/{stockId}/revenue")
    public ResponseEntity<BigDecimal> calculateTotalRevenueForStock(@PathVariable Long stockId) {
        logger.info("GET /api/stock-transactions/stock/{}/revenue - Calcul du revenu total", stockId);
        BigDecimal totalRevenue = transactionService.calculateTotalRevenueForStock(stockId);
        return ResponseEntity.ok(totalRevenue);
    }
}