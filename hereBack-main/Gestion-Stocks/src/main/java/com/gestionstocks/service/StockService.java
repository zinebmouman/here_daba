package com.gestionstocks.service;

import com.gestionstocks.client.BoutiqueServiceClient;
import com.gestionstocks.client.ProduitServiceClient;
import com.gestionstocks.dto.BoutiqueDTO;
import com.gestionstocks.dto.ProduitDTO;
import com.gestionstocks.dto.StockDTO;
import com.gestionstocks.model.Stock;
import com.gestionstocks.model.Stock.StatutStock;
import com.gestionstocks.repository.StockRepository;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class StockService {
    private static final Logger logger = LoggerFactory.getLogger(StockService.class);

    private final BoutiqueServiceClient boutiqueClient;
    private final StockRepository stockRepository;
    private final ProduitServiceClient produitClient;
    private final StockNotificationService notificationService;
    @Autowired
    private BoutiqueServiceClient boutiqueServiceClient; // Ajoutez cette ligne
    @Autowired
    public StockService(
            BoutiqueServiceClient boutiqueClient,
            StockRepository stockRepository,
            ProduitServiceClient produitClient,
            StockNotificationService notificationService
    ) {
        this.boutiqueClient = boutiqueClient;
        this.stockRepository = stockRepository;
        this.produitClient = produitClient;
        this.notificationService = notificationService;
    }

    /**
     * Valide l'existence et la propriété de la boutique
     * @param idBoutique ID de la boutique à valider
     * @return L'ID du vendeur propriétaire
     */
    private String validateBoutique(Long idBoutique) {
        if (idBoutique == null || idBoutique <= 0) {
            throw new IllegalArgumentException("L'ID de la boutique est invalide");
        }

        try {
            // Modification ici : utilisez directement la méthode de boutiqueServiceClient
            String vendeurId = boutiqueServiceClient.getVendeurIdByBoutiqueId(idBoutique);

            if (vendeurId != null && !vendeurId.isEmpty()) {
                return vendeurId;
            } else {
                throw new EntityNotFoundException("Boutique non trouvée avec l'ID: " + idBoutique);
            }
        } catch (Exception e) {
            logger.error("Erreur lors de la validation de la boutique", e);
            throw new IllegalArgumentException("Impossible de valider la boutique : " + e.getMessage());
        }
    }
    /**
     * Récupère tous les stocks
     */
    public List<StockDTO> getAllStocks() {
        logger.info("Récupération de tous les stocks");
        return stockRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Récupère un stock par son ID
     */
    public Optional<StockDTO> getStockById(Long id) {
        logger.info("Récupération du stock avec l'ID: {}", id);
        return stockRepository.findById(id)
                .map(this::convertToDTO);
    }

    /**
     * Récupère les stocks d'une boutique
     */
    public List<StockDTO> getStocksByBoutique(Long idBoutique) {
        logger.info("Récupération des stocks pour la boutique ID: {}", idBoutique);
        // Valider la boutique avant de récupérer les stocks
        validateBoutique(idBoutique);

        return stockRepository.findByIdBoutique(idBoutique).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Récupère les stocks d'un vendeur
     */
    @Transactional(readOnly = true)
    public List<StockDTO> getStocksByVendeur(String idVendeur) {
        try {
            // Vérification robuste
            if (idVendeur == null || idVendeur.trim().isEmpty()) {
                logger.warn("ID vendeur null ou vide fourni à getStocksByVendeur");
                return Collections.emptyList();
            }
            logger.info("=== DÉBUT RÉCUPÉRATION DES STOCKS DU VENDEUR {} ===", idVendeur);
            // Récupérer les boutiques du vendeur
            List<Long> boutiqueIds = boutiqueClient.getBoutiqueIdsByVendeurId(idVendeur);
            logger.info("IDs de boutiques récupérés pour le vendeur {}: {}",
                    idVendeur, boutiqueIds);
            if (boutiqueIds.isEmpty()) {
                logger.warn("Aucune boutique trouvée pour le vendeur {}", idVendeur);
                return Collections.emptyList();
            }

            // Récupérer les stocks pour ces boutiques
            List<Stock> stocks = stockRepository.findByIdBoutiqueIn(boutiqueIds);
            logger.info("Nombre de stocks trouvés pour les boutiques du vendeur {}: {}",
                    idVendeur, stocks.size());
            if (stocks.isEmpty()) {
                logger.info("Aucun stock trouvé pour les boutiques du vendeur {}", idVendeur);
                return Collections.emptyList();
            }

            // Convertir les stocks en DTOs
            List<StockDTO> stockDTOs = stocks.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

            logger.info("=== FIN RÉCUPÉRATION DES STOCKS DU VENDEUR {} : {} stocks trouvés ===",
                    idVendeur, stockDTOs.size());

            return stockDTOs;

        } catch (Exception e) {
            logger.error("Erreur lors de la récupération des stocks pour le vendeur {}: {}",
                    idVendeur, e.getMessage(), e);
            return Collections.emptyList();
        }
    }
    /**
     * Crée un nouveau stock
     */
    @Transactional
    public StockDTO createStock(StockDTO stockDTO) {
        logger.info("Création d'un nouveau stock: {}", stockDTO);

        // Valider la boutique et récupérer l'ID du vendeur
        validateBoutique(stockDTO.getIdBoutique());

        // Convertir le DTO en entité
        Stock stock = convertToEntity(stockDTO);

        // Définir la date de création
        stock.setDateCreation(LocalDateTime.now());

        // Définir la quantité de stock disponible
        stock.setQuantiteStockDisponible(
                stockDTO.getQuantiteStockDisponible() != null
                        ? stockDTO.getQuantiteStockDisponible()
                        : 0
        );

        // Mettre à jour le statut du stock
        stock.updateStatut();

        // Sauvegarder le stock
        Stock savedStock = stockRepository.save(stock);
        logger.info("Stock créé avec succès, ID: {}", savedStock.getId());

        return convertToDTO(savedStock);
    }
    /**
     * Récupère l'ID du vendeur associé à un stock
     */
    public String getVendeurIdByStockId(Long stockId) {
        try {
            logger.info("Récupération du vendeur pour le stock ID: {}", stockId);

            // Récupérer le stock
            Stock stock = stockRepository.findById(stockId)
                    .orElseThrow(() -> {
                        logger.error("Stock non trouvé avec l'ID: {}", stockId);
                        return new jakarta.persistence.EntityNotFoundException("Stock non trouvé avec l'ID: " + stockId);
                    });

            // Récupérer l'ID de la boutique du stock
            Long idBoutique = stock.getIdBoutique();
            logger.info("Boutique ID trouvé pour le stock {}: {}", stockId, idBoutique);

            // Récupérer l'ID du vendeur de la boutique via le client Feign
            String vendeurId = boutiqueClient.getVendeurIdByBoutiqueId(idBoutique);

            if (vendeurId == null || vendeurId.isEmpty()) {
                logger.warn("Vendeur non trouvé pour la boutique {}", idBoutique);
                return "default-vendeur-id";
            }

            logger.info("Vendeur ID trouvé pour le stock {}: {}", stockId, vendeurId);
            return vendeurId;

        } catch (Exception e) {
            logger.error("Erreur lors de la récupération du vendeur pour le stock {}: {}",
                    stockId, e.getMessage(), e);
            return "default-vendeur-id";
        }
    }

    /**
     * Met à jour un stock existant
     */
    @Transactional
    public StockDTO updateStock(Long id, StockDTO stockDTO) {
        logger.info("Mise à jour du stock ID: {} avec données: {}", id, stockDTO);

        return stockRepository.findById(id)
                .map(existingStock -> {
                    // Valider la boutique
                    validateBoutique(stockDTO.getIdBoutique());

                    // Mise à jour des champs
                    updateStockFields(existingStock, stockDTO);

                    Stock updatedStock = stockRepository.save(existingStock);
                    logger.info("Stock mis à jour avec succès, ID: {}", updatedStock.getId());

                    // Vérification du seuil critique après mise à jour
                    checkCriticalStockThreshold(updatedStock);

                    return convertToDTO(updatedStock);
                })
                .orElseThrow(() -> {
                    logger.error("Stock non trouvé avec l'ID: {}", id);
                    return new EntityNotFoundException("Stock non trouvé avec l'ID: " + id);
                });
    }

    /**
     * Met à jour les champs d'un stock
     */
    private void updateStockFields(Stock existingStock, StockDTO stockDTO) {
        // Mise à jour du nom si fourni
        if (StringUtils.hasText(stockDTO.getName())) {
            existingStock.setName(stockDTO.getName());
        }

        // Mise à jour de l'emplacement si fourni
        if (StringUtils.hasText(stockDTO.getLocation())) {
            existingStock.setLocation(stockDTO.getLocation());
        }

        // Mise à jour de la capacité maximale
        if (stockDTO.getCapaciteMaximaleStock() != null && stockDTO.getCapaciteMaximaleStock() > 0) {
            existingStock.setCapaciteMaximaleStock(stockDTO.getCapaciteMaximaleStock());
            existingStock.updateStatut(); // Mettre à jour le statut après changement de capacité
        }

        // Mise à jour de la quantité de stock
        if (stockDTO.getQuantiteStockDisponible() != null
                && !stockDTO.getQuantiteStockDisponible().equals(existingStock.getQuantiteStockDisponible())) {
            existingStock.setQuantiteStockDisponible(stockDTO.getQuantiteStockDisponible());
            existingStock.updateStatut(); // Mettre à jour le statut après changement de quantité
        }
    }

    /**
     * Vérifie et notifie si le stock est critique
     */
    private void checkCriticalStockThreshold(Stock stock) {
        if (stock.getStatut() == StatutStock.CRITIQUE || stock.getStatut() == StatutStock.RUPTURE) {
            notificationService.checkAndNotifyLowStock(stock);
        }
    }

    /**
     * Supprime un stock
     */
    /**
     * Supprime un stock et tous ses produits associés
     */
    @Transactional
    public void deleteStock(Long id) {
        logger.info("Suppression du stock ID: {} et de tous ses produits", id);

        // Vérifier si le stock existe
        Stock stock = stockRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("Stock non trouvé avec l'ID: {}", id);
                    return new EntityNotFoundException("Stock non trouvé avec l'ID: " + id);
                });

        try {
            // 1. Récupérer les produits du stock
            List<ProduitDTO> produits = produitClient.getProduitsByStock(id);
            logger.info("Nombre de produits à supprimer: {}", produits.size());

            // 2. Supprimer tous les produits associés
            // (Cette ligne dépend de la façon dont votre ProduitServiceClient est implémenté)
            for (ProduitDTO produit : produits) {
                try {
                    // Appeler l'endpoint de suppression de produit
                    // (à implémenter dans ProduitServiceClient si ce n'est pas déjà fait)
                    produitClient.deleteProduit(produit.getId());
                    logger.info("Produit supprimé: {}", produit.getId());
                } catch (Exception e) {
                    logger.error("Erreur lors de la suppression du produit {}: {}", produit.getId(), e.getMessage());
                    // Continuer avec les autres produits même en cas d'erreur
                }
            }

            // 3. Supprimer le stock
            stockRepository.delete(stock);
            logger.info("Stock supprimé avec succès, ID: {}", id);

        } catch (Exception e) {
            logger.error("Erreur lors de la suppression du stock {}: {}", id, e.getMessage());
            throw new RuntimeException("Erreur lors de la suppression du stock: " + e.getMessage(), e);
        }
    }

    /**
     * Ajuste la quantité de stock disponible
     */
    @Transactional
    public StockDTO adjustStockQuantity(Long stockId, Integer quantityAdjustment) {
        logger.info("Ajustement de la quantité du stock ID: {} par {}", stockId, quantityAdjustment);

        Stock stock = stockRepository.findById(stockId)
                .orElseThrow(() -> {
                    logger.error("Stock non trouvé avec l'ID: {}", stockId);
                    return new EntityNotFoundException("Stock non trouvé avec l'ID: " + stockId);
                });

        Integer newQuantity = stock.getQuantiteStockDisponible() + quantityAdjustment;
        if (newQuantity < 0) {
            logger.error("La quantité ajustée serait négative: {}", newQuantity);
            throw new IllegalArgumentException("La quantité du stock ne peut pas être négative");}

        stock.setQuantiteStockDisponible(newQuantity);
        stock.updateStatut();
        Stock updatedStock = stockRepository.save(stock);

        // Vérifier si le stock est critique après ajustement
        checkCriticalStockThreshold(updatedStock);

        logger.info("Stock ajusté avec succès, nouvelle quantité: {}", updatedStock.getQuantiteStockDisponible());
        return convertToDTO(updatedStock);
    }

    /**
     * Vérifie les stocks critiques
     */
    @Transactional(readOnly = true)
    public List<StockDTO> checkLowStocks() {
        logger.info("Vérification des stocks critiques");

        List<Stock.StatutStock> criticalStatuses = List.of(StatutStock.CRITIQUE, StatutStock.RUPTURE);
        List<Stock> lowStocks = stockRepository.findByStatutIn(criticalStatuses);

        // Notification pour chaque stock critique
        lowStocks.forEach(notificationService::checkAndNotifyLowStock);

        return lowStocks.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Convertit une entité Stock en DTO
     */
    private StockDTO convertToDTO(Stock stock) {
        return StockDTO.builder()
                .id(stock.getId())
                .name(stock.getName())
                .location(stock.getLocation())
                .quantiteStockDisponible(stock.getQuantiteStockDisponible())
                .capaciteMaximaleStock(stock.getCapaciteMaximaleStock())
                .idBoutique(stock.getIdBoutique())
                .dateCreation(stock.getDateCreation())
                .statut(stock.getStatut())
                .build();
    }

    /**
     * Convertit un DTO Stock en entité
     */
    private Stock convertToEntity(StockDTO stockDTO) {
        // Validation préalable du DTO
        if (stockDTO == null) {
            throw new IllegalArgumentException("Le DTO de stock ne peut pas être null");
        }

        // Valider la boutique et récupérer son vendeur
        validateBoutique(stockDTO.getIdBoutique());

        Stock stock = new Stock();
        stock.setId(stockDTO.getId());
        stock.setName(stockDTO.getName());
        stock.setLocation(stockDTO.getLocation());
        stock.setQuantiteStockDisponible(stockDTO.getQuantiteStockDisponible());
        stock.setCapaciteMaximaleStock(stockDTO.getCapaciteMaximaleStock());
        stock.setIdBoutique(stockDTO.getIdBoutique());

        stock.setDateCreation(
                stockDTO.getDateCreation() != null
                        ? stockDTO.getDateCreation()
                        : LocalDateTime.now()
        );
        stock.setStatut(stockDTO.getStatut());

        return stock;
    }

    /**
     * Récupère les détails complets d'un stock
     */
    public StockDTO getFullStockDetails(Long stockId) {
        logger.info("Récupération des détails complets pour le stock ID: {}", stockId);

        return stockRepository.findById(stockId)
                .map(stock -> {
                    // Convertir le stock en DTO
                    StockDTO stockDTO = convertToDTO(stock);

                    // Récupérer les produits du stock
                    List<ProduitDTO> produits = produitClient.getProduitsByStock(stockId);

                    // Log des produits trouvés
                    logger.info("Nombre de produits dans le stock {}: {}", stockId, produits.size());

                    return stockDTO;
                })
                .orElseThrow(() -> {
                    logger.error("Stock non trouvé avec l'ID: {}", stockId);
                    return new EntityNotFoundException("Stock non trouvé avec l'ID: " + stockId);
                });
    }

    /**
     * Récupère les stocks pour un vendeur spécifique avec des filtres
     *
     * @param idVendeur ID du vendeur
     * @param minQuantite Quantité minimale de stock (optionnel)
     * @param maxQuantite Quantité maximale de stock (optionnel)
     * @param statut Statut du stock (optionnel)
     */
    public List<StockDTO> rechercerStocks(
            String idVendeur,
            Integer minQuantite,
            Integer maxQuantite,
            StatutStock statut) {

        logger.info("Recherche de stocks pour le vendeur ID: {}", idVendeur);

        // Obtenir les boutiques du vendeur
        List<Long> boutiqueIds = boutiqueClient.getBoutiqueIdsByVendeurId(idVendeur);

        // Récupérer les stocks de ces boutiques
        List<Stock> stocks = stockRepository.findByIdBoutiqueIn(boutiqueIds);

        // Filtrer les stocks selon les critères
        return stocks.stream()
                .filter(stock -> {
                    // Filtre par quantité minimale
                    if (minQuantite != null && stock.getQuantiteStockDisponible() < minQuantite) {
                        return false;
                    }

                    // Filtre par quantité maximale
                    if (maxQuantite != null && stock.getQuantiteStockDisponible() > maxQuantite) {
                        return false;
                    }

                    // Filtre par statut
                    if (statut != null && stock.getStatut() != statut) {
                        return false;
                    }

                    return true;
                })
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
}