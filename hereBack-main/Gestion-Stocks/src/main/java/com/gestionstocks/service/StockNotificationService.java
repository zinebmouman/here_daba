package com.gestionstocks.service;

import com.gestionstocks.client.BoutiqueServiceClient;
import com.gestionstocks.client.ProduitServiceClient;
import com.gestionstocks.config.RabbitMQConfig;
import com.gestionstocks.dto.ProduitDTO;
import com.gestionstocks.model.Stock;
import com.gestionstocks.repository.StockRepository;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StockNotificationService {
    private static final Logger logger = LoggerFactory.getLogger(StockNotificationService.class);

    @Autowired
    private ProduitServiceClient produitClient;
    @Autowired
    private BoutiqueServiceClient boutiqueServiceClient;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private StockRepository stockRepository;

    /**
     * Vérifie si un stock est critique et envoie une notification si nécessaire
     */

    public void checkAndNotifyLowStock(Stock stock) {
        logger.info("Vérification de l'état critique du stock ID: {}", stock.getId());

        if (stock.getStatut() == Stock.StatutStock.CRITIQUE || stock.getStatut() == Stock.StatutStock.RUPTURE) {
            try {
                // Récupérer tous les produits de ce stock
                List<ProduitDTO> produits = produitClient.getProduitsByStock(stock.getId());

                // Récupérer l'ID du vendeur de la boutique
                String vendeurId = recupererVendeurPourStock(stock.getIdBoutique());

                // Vérifier chaque produit
                for (ProduitDTO produit : produits) {
                    if (produit.getQuantite() <= produit.getSeuilCritique()) {
                        // Envoyer une notification pour ce produit
                        sendLowStockNotification(produit, vendeurId);
                    }
                }
            } catch (Exception e) {
                logger.error("Erreur lors de la vérification du stock critique ID: {}", stock.getId(), e);
            }
        }
    }

    // Méthode pour récupérer le vendeur du stock
    private String recupererVendeurPourStock(Long stockId) {
        try {
            Stock stock = stockRepository.findById(stockId)
                    .orElseThrow(() -> new EntityNotFoundException("Stock non trouvé"));

            // Utilisation directe de la méthode sans ResponseEntity
            String vendeurId = boutiqueServiceClient.getVendeurIdByBoutiqueId(stock.getIdBoutique());

            return vendeurId != null ? vendeurId : "vendeur-inconnu";
        } catch (Exception e) {
            logger.error("Impossible de récupérer le vendeur pour le stock {}", stockId, e);
            return "vendeur-inconnu";
        }
    }
    /**
     * Envoie une notification pour un produit avec un stock critique
     */
    private void sendLowStockNotification(ProduitDTO produit, String vendeurId) {
        logger.info("Envoi d'une notification de stock critique pour le produit: {}", produit.getId());

        try {
            Map<String, Object> notificationData = new HashMap<>();
            notificationData.put("type", "CRITICAL_STOCK");
            notificationData.put("vendeurId", vendeurId);
            notificationData.put("productId", produit.getId());
            notificationData.put("productName", produit.getNomProduit());
            notificationData.put("currentStock", produit.getQuantite().toString());
            notificationData.put("seuilCritique", produit.getSeuilCritique().toString());

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE_NAME,
                    RabbitMQConfig.ROUTING_KEY_CRITICAL_STOCK,
                    notificationData
            );

            logger.info("Notification de stock critique envoyée pour le produit: {}", produit.getNomProduit());
        } catch (Exception e) {
            logger.error("Erreur lors de l'envoi de la notification de stock critique pour le produit: {}", produit.getId(), e);
        }
    }

    /**
     * Envoie une notification pour un produit proche de l'expiration
     */
    private void sendExpirationNotification(ProduitDTO produit, String vendeurId) {
        logger.info("Envoi d'une notification d'expiration pour le produit: {}", produit.getId());

        try {
            // Calculer le nombre de jours restants
            LocalDate now = LocalDate.now();
            LocalDate expiration = produit.getDateExpiration();
            long joursRestants = ChronoUnit.DAYS.between(now, expiration);

            String messageTemps;
            if (joursRestants <= 1) {
                messageTemps = "URGENT: Expire demain";
            } else if (joursRestants <= 3) {
                messageTemps = "Très urgent: " + joursRestants + " jours";
            } else {
                messageTemps = "Urgent: " + joursRestants + " jours";
            }

            Map<String, Object> notificationData = new HashMap<>();
            notificationData.put("type", "PRODUCT_EXPIRATION");
            notificationData.put("vendeurId", vendeurId);
            notificationData.put("productId", produit.getId());
            notificationData.put("productName", produit.getNomProduit());
            notificationData.put("dateExpiration", produit.getDateExpiration().format(DateTimeFormatter.ISO_DATE));
            notificationData.put("joursRestants", joursRestants);
            notificationData.put("messageTemps", messageTemps);

            // Log détaillé pour debug
            logger.info("Envoi notification d'expiration: {}", notificationData);

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE_NAME,
                    RabbitMQConfig.ROUTING_KEY_PRODUCT_EXPIRATION,
                    notificationData
            );

            logger.info("Notification d'expiration envoyée pour le produit: {}", produit.getNomProduit());
        } catch (Exception e) {
            logger.error("Erreur lors de l'envoi de la notification d'expiration pour le produit: {}", produit.getId(), e);
        }
    }
    /**
     * Tâche planifiée pour vérifier les stocks critiques et les produits proche de l'expiration
     * Exécutée toutes les heures
     */
    @Scheduled(fixedRate = 3600000) // 1 heure
    public void scheduledStockCheck() {
        logger.info("===== DÉBUT VÉRIFICATION PLANIFIÉE DES STOCKS ET EXPIRATIONS =====");

        try {
            // Vérifier les stocks critiques
            List<ProduitDTO> produitsEnAlerte = produitClient.getProduitsEnAlerte();
            logger.info("Produits en alerte trouvés: {}", produitsEnAlerte.size());

            for (ProduitDTO produit : produitsEnAlerte) {
                // Vérifier que nous avons toutes les informations nécessaires
                if (produit.getIdStock() != null) {
                    // Récupérer le véritable ID du vendeur pour ce produit
                    String vendeurId = recupererVendeurPourStock(produit.getIdStock());
                    logger.info("ID vendeur récupéré pour produit en alerte {}: {}", produit.getId(), vendeurId);

                    sendLowStockNotification(produit, vendeurId);
                }
            }

            // Vérifier les produits proches de l'expiration
            List<ProduitDTO> produitsProcheExpiration = produitClient.getProduitsProcheExpiration();
            logger.info("Produits proches de l'expiration trouvés: {}", produitsProcheExpiration.size());

            for (ProduitDTO produit : produitsProcheExpiration) {
                if (produit.getIdStock() != null) {
                    // Récupérer le véritable ID du vendeur pour ce produit
                    String vendeurId = recupererVendeurPourStock(produit.getIdStock());
                    logger.info("ID vendeur récupéré pour produit proche expiration {}: {}", produit.getId(), vendeurId);

                    sendExpirationNotification(produit, vendeurId);
                }
            }

            logger.info("===== FIN VÉRIFICATION PLANIFIÉE DES STOCKS ET EXPIRATIONS =====");
        } catch (Exception e) {
            logger.error("ERREUR GLOBALE VÉRIFICATION STOCKS", e);
        }
    }



}