package com.boutique_catalogue_produits.client;

import com.boutique_catalogue_produits.dto.StockDTO;
import com.boutique_catalogue_produits.dto.StockTransactionDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class StockServiceClientFallback implements StockServiceClient {
    private static final Logger logger = LoggerFactory.getLogger(StockServiceClientFallback.class);

    // Méthodes fallback existantes
    @Override
    public StockDTO getStockById(Long id) {
        return null;
    }
    @Override
    public Long getBoutiqueIdByStockId(Long stockId) {
        logger.warn("Fallback: Impossible de récupérer la boutique pour le stock {}", stockId);
        return null;
    }
    @Override
    public String getVendeurIdByStockId(Long stockId) {
        logger.warn("Fallback: Impossible de récupérer le vendeur pour le stock {}", stockId);
        return "default-vendeur-id"; // Retourne un ID de vendeur par défaut
    }
    @Override
    public void envoyerNotificationStockCritique(Map<String, Object> notificationData) {
        logger.error("Fallback: Impossible d'envoyer la notification de stock critique pour le produit {}",
                notificationData.get("productId"));
    }

    // Ajout de la méthode manquante
    @Override
    public void envoyerNotificationExpiration(Map<String, Object> notificationData) {
        logger.error("Fallback: Impossible d'envoyer la notification d'expiration pour le produit {}",
                notificationData.get("productId"));
    }

    @Override
    public List<StockDTO> getStocksByBoutiqueId(Integer idBoutique) {
        return List.of();
    }

    @Override
    public StockTransactionDTO createTransaction(Map<String, Object> transaction) {
        return null;
    }
}