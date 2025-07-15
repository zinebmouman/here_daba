package com.gestionstocks.service;

import com.gestionstocks.client.BoutiqueServiceClient;
import com.gestionstocks.client.ProduitServiceClient;
import com.gestionstocks.config.RabbitMQConfig;
import com.gestionstocks.dto.ProduitDTO;
import com.gestionstocks.dto.StockTransactionDTO;
import com.gestionstocks.model.Stock;
import com.gestionstocks.model.StockTransaction;
import com.gestionstocks.model.StockTransaction.TransactionType;
import com.gestionstocks.repository.StockRepository;
import com.gestionstocks.repository.StockTransactionRepository;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class StockTransactionService {
    private static final Logger logger = LoggerFactory.getLogger(StockTransactionService.class);

    private final ProduitServiceClient produitClient;
    private final BoutiqueServiceClient boutiqueClient;
    private final StockTransactionRepository transactionRepository;
    private final StockRepository stockRepository;
    private final RabbitTemplate rabbitTemplate;
    private final StockService stockService;
    @Autowired
    private BoutiqueServiceClient boutiqueServiceClient;
    @Autowired
    public StockTransactionService(
            ProduitServiceClient produitClient,
            BoutiqueServiceClient boutiqueClient,
            StockTransactionRepository transactionRepository,
            StockRepository stockRepository,
            RabbitTemplate rabbitTemplate,
            StockService stockService
    ) {
        this.produitClient = produitClient;
        this.boutiqueClient = boutiqueClient;
        this.transactionRepository = transactionRepository;
        this.stockRepository = stockRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.stockService = stockService;
    }

    /**
     * Récupère l'ID du vendeur pour un stock donné
     */
    private String recupererVendeurPourStock(Long stockId) {
        try {
            Stock stock = stockRepository.findById(stockId)
                    .orElseThrow(() -> new EntityNotFoundException("Stock non trouvé"));

            // Utilisation directe sans ResponseEntity
            String vendeurId = boutiqueServiceClient.getVendeurIdByBoutiqueId(stock.getIdBoutique());

            return vendeurId != null ? vendeurId : "vendeur-inconnu";
        } catch (Exception e) {
            logger.error("Impossible de récupérer le vendeur pour le stock {}", stockId, e);
            return "vendeur-inconnu";
        }
    }

    /**
     * Crée une nouvelle transaction de stock
     */
    @Transactional
    public StockTransactionDTO createTransaction(
            Long productId,
            Long stockId,
            TransactionType type,
            Integer quantity,
            String notes,
            BigDecimal prixFourni
    ) {
        try {
            // Validation initiale
            validateTransactionInput(productId, stockId, type, quantity);

            // Vérifier que le stock existe
            Stock stock = stockRepository.findById(stockId)
                    .orElseThrow(() -> new EntityNotFoundException("Stock non trouvé avec l'ID: " + stockId));

            // Récupérer le produit avant la transaction
            ProduitDTO produitAvant = getProduitPourTransaction(productId);

            // Vérifier l'appartenance du produit au stock
            validateProduitStock(produitAvant, stockId);

            // Déterminer le prix unitaire
            BigDecimal prixUnitaire = determinerPrixUnitaire(prixFourni, produitAvant);

            // Créer et sauvegarder la transaction
            StockTransaction transaction = creerTransaction(
                    productId, stockId, type, quantity, notes, prixUnitaire);

            // Ajuster les stocks
            ajusterStockProduit(productId, type, quantity);
            ajusterStockGeneral(stockId, type, quantity);

            // Vérifier et envoyer une notification de stock critique si nécessaire
            verifierStockCritique(productId, stockId, type);

            logger.info("Transaction créée avec succès - ID: {}", transaction.getId());
            return convertToDTO(transaction);

         } catch (Exception e) {
        logger.error("Erreur lors de la création de la transaction", e);
        TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        throw new RuntimeException("Impossible de créer la transaction : " + e.getMessage(), e);
    }
    }

    /**
     * Validation des paramètres d'entrée de la transaction
     */
    private void validateTransactionInput(Long productId, Long stockId, TransactionType type, Integer quantity) {
        if (productId == null || stockId == null) {
            throw new IllegalArgumentException("Les identifiants de produit et de stock sont requis");
        }
        if (type == null) {
            throw new IllegalArgumentException("Le type de transaction est requis");
        }
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("La quantité doit être positive");
        }
    }

    /**
     * Récupérer le produit pour la transaction
     */
    private ProduitDTO getProduitPourTransaction(Long productId) {
        ProduitDTO produit = produitClient.getProduitById(productId);
        if (produit == null) {
            logger.error("Produit non trouvé: ID {}", productId);
            throw new EntityNotFoundException("Produit non trouvé avec l'ID: " + productId);
        }
        logger.info("Produit récupéré - ID: {}, Nom: {}, Stock ID: {}",
                produit.getId(), produit.getNomProduit(), produit.getIdStock());
        return produit;
    }

    /**
     * Validation de l'appartenance du produit au stock
     */
    private void validateProduitStock(ProduitDTO produit, Long stockId) {
        if (produit.getIdStock() == null || !produit.getIdStock().equals(stockId)) {
            logger.warn("Produit {} appartient au stock {} différent du stock demandé {}",
                    produit.getId(), produit.getIdStock(), stockId);
            // Commenté car vous voulez désactiver temporairement cette validation
            // throw new IllegalArgumentException("Le produit n'appartient pas à ce stock");
        }
    }

    /**
     * Déterminer le prix unitaire à utiliser
     */
    private BigDecimal determinerPrixUnitaire(BigDecimal prixFourni, ProduitDTO produit) {
        if (prixFourni != null && prixFourni.compareTo(BigDecimal.ZERO) > 0) {
            logger.info("Utilisation du prix fourni: {}", prixFourni);
            return prixFourni;
        }

        if (produit.getPrix() != null) {
            logger.info("Utilisation du prix du produit: {}", produit.getPrix());
            return produit.getPrix();
        }

        logger.warn("Aucun prix disponible, utilisation de ZERO");
        return BigDecimal.ZERO;
    }

    /**
     * Créer et sauvegarder la transaction
     */
    private StockTransaction creerTransaction(
            Long productId,
            Long stockId,
            TransactionType type,
            Integer quantity,
            String notes,
            BigDecimal prixUnitaire
    ) {
        StockTransaction transaction = new StockTransaction();
        transaction.setProductId(productId);
        transaction.setStockId(stockId);
        transaction.setType(type);
        transaction.setQuantity(quantity);
        transaction.setTransactionDate(LocalDateTime.now());
        transaction.setPrixUnitaire(prixUnitaire);
        transaction.setNotes(notes);

        // Calculer le revenu total pour les transactions de sortie
        if (type == TransactionType.REMOVE) {
            BigDecimal revenuTotal = prixUnitaire.multiply(BigDecimal.valueOf(quantity))
                    .setScale(2, RoundingMode.HALF_UP);
            transaction.setRevenuTotal(revenuTotal);
        } else {
            transaction.setRevenuTotal(BigDecimal.ZERO);
        }

        return transactionRepository.save(transaction);
    }

    /**
     * Ajuster le stock du produit
     */
    private void ajusterStockProduit(Long productId, TransactionType type, Integer quantity) {
        try {
            int adjustmentValue = type == TransactionType.ADD ? quantity : -quantity;
            produitClient.ajusterStock(productId, adjustmentValue);
            logger.info("Stock du produit {} ajusté de {} ({})", productId, adjustmentValue, type);
        } catch (Exception e) {
            logger.error("Erreur lors de l'ajustement du stock du produit {}", productId, e);
            throw new RuntimeException("Impossible d'ajuster le stock du produit", e);
        }
    }

    /**
     * Ajuster le stock général
     */
    private void ajusterStockGeneral(Long stockId, TransactionType type, Integer quantity) {
        try {
            int stockAdjustment = type == TransactionType.ADD ? quantity : -quantity;

            // Vérifier si c'est une opération de retrait (REMOVE)
            if (type == TransactionType.REMOVE) {
                Stock stock = stockRepository.findById(stockId)
                        .orElseThrow(() -> new EntityNotFoundException("Stock non trouvé avec l'ID: " + stockId));

                // Si la quantité disponible sera strictement négative après le retrait
                if (stock.getQuantiteStockDisponible() < quantity) {
                    logger.error("Quantité insuffisante en stock: {} unités disponibles, tentative de retrait de {} unités",
                            stock.getQuantiteStockDisponible(), quantity);
                    throw new IllegalArgumentException(
                            "Impossible de retirer " + quantity + " unités du stock. Quantité disponible: "
                                    + stock.getQuantiteStockDisponible());
                }

                // Si le stock sera exactement à zéro, on permet l'opération
                logger.info("Opération de retrait validée: stock disponible={}, quantité à retirer={}",
                        stock.getQuantiteStockDisponible(), quantity);
            }

            stockService.adjustStockQuantity(stockId, stockAdjustment);
            logger.info("Stock général {} ajusté de {} ({})", stockId, stockAdjustment, type);
        } catch (Exception e) {
            logger.error("Erreur lors de l'ajustement de la quantité du stock {}: {}", stockId, e.getMessage(), e);
            throw new RuntimeException("Impossible d'ajuster la quantité du stock", e);
        }
    }

    /**
     * Vérifier et envoyer une notification de stock critique
     */
    private void verifierStockCritique(Long productId, Long stockId, TransactionType type) {
        try {
            if (type == TransactionType.REMOVE) {
                ProduitDTO produitApres = produitClient.getProduitById(productId);
                if (produitApres != null && produitApres.getQuantite() <= produitApres.getSeuilCritique()) {
                    String vendeurId = recupererVendeurPourStock(stockId);

                    Map<String, Object> messageStock = new HashMap<>();
                    messageStock.put("type", "CRITICAL_STOCK");
                    messageStock.put("productId", productId);
                    messageStock.put("productName", produitApres.getNomProduit());
                    messageStock.put("currentStock", produitApres.getQuantite().toString());
                    messageStock.put("seuilCritique", produitApres.getSeuilCritique().toString());
                    messageStock.put("vendeurId", vendeurId);

                    rabbitTemplate.convertAndSend(
                            RabbitMQConfig.EXCHANGE_NAME,
                            RabbitMQConfig.ROUTING_KEY_CRITICAL_STOCK,
                            messageStock
                    );
                    logger.info("Notification de stock critique envoyée pour le produit: {}", produitApres.getNomProduit());
                }
            }
        } catch (Exception e) {
            logger.warn("Erreur lors de l'envoi de la notification de stock critique", e);
        }
    }

    // Méthodes existantes pour récupérer les transactions

    public List<StockTransactionDTO> getAllTransactions() {
        List<StockTransaction> transactions = transactionRepository.findAll();
        logger.info("Récupération de toutes les transactions: {} transactions trouvées", transactions.size());
        return transactions.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public Optional<StockTransactionDTO> getTransactionById(Long id) {
        logger.info("Recherche de la transaction avec l'ID: {}", id);
        return transactionRepository.findById(id)
                .map(this::convertToDTO);
    }

    public List<StockTransactionDTO> getTransactionsByProductId(Long productId) {
        logger.info("Recherche des transactions pour le produit ID: {}", productId);

        List<StockTransaction> transactions = transactionRepository.findByProductId(productId);

        logger.info("Nombre de transactions trouvées : {}", transactions.size());

        return transactions.stream()
                .map(this::convertToDTO)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public List<StockTransactionDTO> getTransactionsByStockId(Long stockId) {
        logger.info("Recherche des transactions pour le stock ID: {}", stockId);
        return transactionRepository.findByStockId(stockId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<StockTransactionDTO> getTransactionsByProductAndStock(Long productId, Long stockId) {
        logger.info("Recherche des transactions pour le produit ID: {} et le stock ID: {}", productId, stockId);
        return transactionRepository.findByProductIdAndStockId(productId, stockId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // Méthodes de calcul de revenu

    public Map<LocalDate, BigDecimal> getDailyRevenueForStock(Long stockId) {
        logger.info("Calcul du revenu quotidien pour le stock ID: {}", stockId);

        List<Object[]> rawData = transactionRepository.getDailyRevenueForStock(stockId);

        Map<LocalDate, BigDecimal> result = new HashMap<>();
        for (Object[] row : rawData) {
            LocalDate date = (LocalDate) row[0];
            BigDecimal revenue = (BigDecimal) row[1];
            result.put(date, revenue);
        }

        result.forEach((date, revenue) ->
                logger.info("Revenu pour le {}: {}", date, revenue)
        );

        return result;
    }

    public BigDecimal calculateTotalRevenueForProduct(Long productId) {
        BigDecimal revenue = transactionRepository.calculateTotalRevenueForProduct(productId);
        return revenue != null ? revenue : BigDecimal.ZERO;
    }

    public BigDecimal calculateTotalRevenueForStock(Long stockId) {
        BigDecimal revenue = transactionRepository.calculateTotalRevenueForStock(stockId);
        return revenue != null ? revenue : BigDecimal.ZERO;
    }

    // Méthodes de conversion

    private StockTransactionDTO convertToDTO(StockTransaction transaction) {
        return StockTransactionDTO.builder()
                .id(transaction.getId())
                .productId(transaction.getProductId())
                .stockId(transaction.getStockId())
                .type(transaction.getType())
                .quantity(transaction.getQuantity())
                .transactionDate(transaction.getTransactionDate())
                .prixUnitaire(transaction.getPrixUnitaire())
                .revenuTotal(transaction.getRevenuTotal())
                .notes(transaction.getNotes())
                .build();
    }

    private StockTransaction convertToEntity(StockTransactionDTO dto) {
        StockTransaction transaction = new StockTransaction();
        transaction.setId(dto.getId());
        transaction.setProductId(dto.getProductId());
        transaction.setStockId(dto.getStockId());
        transaction.setType(dto.getType());
        transaction.setQuantity(dto.getQuantity());
        transaction.setTransactionDate(dto.getTransactionDate());
        transaction.setPrixUnitaire(dto.getPrixUnitaire());
        transaction.setRevenuTotal(dto.getRevenuTotal());
        transaction.setNotes(dto.getNotes());
        return transaction;
    }
}