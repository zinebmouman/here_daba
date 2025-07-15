package com.boutique_catalogue_produits.service;

import com.boutique_catalogue_produits.client.StockServiceClient;
import com.boutique_catalogue_produits.config.RabbitMQConfig;
import com.boutique_catalogue_produits.model.Produit;
import com.boutique_catalogue_produits.repository.ProduitRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
@Service
public class ProductExpirationChecker {
    private static final Logger logger = LoggerFactory.getLogger(ProductExpirationChecker.class);

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private ProduitRepository produitRepository;

    @Autowired
    private StockServiceClient stockServiceClient;

    @Scheduled(cron = "0 */5 * * * *") // Tous les 5 minutes pour le test (au lieu de tous les jours)
    public void checkProductsNearExpiration() {
        try {
            logger.info("DIAGNOSTIC: Début vérification des produits proche expiration et stock critique");

            LocalDate now = LocalDate.now();
            LocalDate oneWeekLater = now.plusWeeks(1);

            logger.info("DIAGNOSTIC: Période de vérification: {} - {}", now, oneWeekLater);

            // Produits en alerte de stock
            List<Produit> produitsEnAlerte = produitRepository.findProduitsEnAlerte();
            logger.info("DIAGNOSTIC: Nombre de produits en alerte de stock trouvés: {}", produitsEnAlerte.size());

            for (Produit produit : produitsEnAlerte) {
                logger.info("DIAGNOSTIC: Traitement produit en alerte: {} (ID: {})",
                        produit.getNomProduit(), produit.getId());
                envoyerNotificationStockCritique(produit);
            }

            // Produits proches de l'expiration
            List<Produit> produitsProcheExpiration = produitRepository.findProduitsProcheExpiration(now, oneWeekLater);
            logger.info("DIAGNOSTIC: Nombre de produits proches de l'expiration trouvés: {}", produitsProcheExpiration.size());

            for (Produit produit : produitsProcheExpiration) {
                logger.info("DIAGNOSTIC: Traitement produit proche expiration: {} (ID: {}, date expiration: {})",
                        produit.getNomProduit(), produit.getId(), produit.getDateExpiration());
                envoyerNotificationExpiration(produit);
            }

            logger.info("DIAGNOSTIC: Fin vérification des produits proche expiration et stock critique");
        } catch (Exception e) {
            logger.error("DIAGNOSTIC: Erreur globale lors de la vérification des produits: {}", e.getMessage(), e);
        }
    }

    private void envoyerNotificationStockCritique(Produit produit) {
        try {
            logger.info("DIAGNOSTIC: Envoi de notification stock critique pour produit: {}, ID: {}, Quantité: {}, Seuil: {}",
                    produit.getNomProduit(), produit.getId(), produit.getQuantite(), produit.getSeuilCritique());

            // Vérifier si le produit a un stock associé
            if (produit.getIdStock() == null) {
                logger.warn("DIAGNOSTIC: Aucun stock associé au produit {}", produit.getId());
                return;
            }

            String vendeurId = stockServiceClient.getVendeurIdByStockId(produit.getIdStock());
            logger.info("DIAGNOSTIC: Vendeur ID récupéré pour stock critique: {}", vendeurId);

            // Vérifier si un vendeur a été trouvé
            if (vendeurId == null || vendeurId.isEmpty() || "default-vendeur-id".equals(vendeurId)) {
                logger.warn("DIAGNOSTIC: Aucun vendeur valide trouvé pour le stock {}", produit.getIdStock());
                return;
            }

            Map<String, Object> notificationData = new HashMap<>();
            notificationData.put("type", "CRITICAL_STOCK");
            notificationData.put("vendeurId", vendeurId);
            notificationData.put("productId", produit.getId());
            notificationData.put("productName", produit.getNomProduit());
            notificationData.put("currentStock", produit.getQuantite().toString());
            notificationData.put("seuilCritique", produit.getSeuilCritique().toString());

            logger.info("DIAGNOSTIC: Données de notification stock critique: {}", notificationData);

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE_NAME,
                    RabbitMQConfig.ROUTING_KEY_CRITICAL_STOCK,
                    notificationData
            );

            logger.info("DIAGNOSTIC: Notification de stock critique envoyée avec succès pour le produit: {}", produit.getNomProduit());
        } catch (Exception e) {
            logger.error("DIAGNOSTIC: Erreur lors de l'envoi de notification de stock critique pour le produit {}: {}",
                    produit.getId(), e.getMessage(), e);
            e.printStackTrace();
        }
    }
    private void envoyerNotificationExpiration(Produit produit) {
        try {
            // Log détaillé pour le diagnostic
            logger.info("DIAGNOSTIC: Envoi de notification d'expiration pour produit: {}, ID: {}, Date expiration: {}",
                    produit.getNomProduit(), produit.getId(), produit.getDateExpiration());

            // Vérifier si le produit a un stock associé
            if (produit.getIdStock() == null) {
                logger.warn("DIAGNOSTIC: Aucun stock associé au produit {}", produit.getId());
                return;
            }

            // Récupérer l'ID du vendeur via le stock avec log de diagnostic
            logger.info("DIAGNOSTIC: Tentative de récupération du vendeur pour le stock {}", produit.getIdStock());
            String vendeurId = stockServiceClient.getVendeurIdByStockId(produit.getIdStock());
            logger.info("DIAGNOSTIC: Vendeur ID récupéré: {}", vendeurId);

            // Vérifier si un vendeur a été trouvé
            if (vendeurId == null || vendeurId.isEmpty() || "default-vendeur-id".equals(vendeurId)) {
                logger.warn("DIAGNOSTIC: Aucun vendeur valide trouvé pour le stock {}", produit.getIdStock());
                return;
            }

            // Calculer le nombre de jours restants
            long joursRestants = ChronoUnit.DAYS.between(LocalDate.now(), produit.getDateExpiration());
            logger.info("DIAGNOSTIC: Jours restants avant expiration: {}", joursRestants);

            String messageTemps = joursRestants <= 1
                    ? "demain"
                    : joursRestants + " jours";

            Map<String, Object> notificationData = new HashMap<>();
            notificationData.put("type", "PRODUCT_EXPIRATION");
            notificationData.put("vendeurId", vendeurId);
            notificationData.put("productId", produit.getId());
            notificationData.put("productName", produit.getNomProduit());
            notificationData.put("dateExpiration", produit.getDateExpiration().toString());
            notificationData.put("joursRestants", joursRestants);
            notificationData.put("messageTemps", messageTemps);

            // Log complet des données de notification
            logger.info("DIAGNOSTIC: Données de notification: {}", notificationData);

            // Envoyer la notification via RabbitMQ
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE_NAME,
                    RabbitMQConfig.ROUTING_KEY_PRODUCT_EXPIRATION,
                    notificationData
            );

            logger.info("DIAGNOSTIC: Notification d'expiration envoyée avec succès pour le produit: {}", produit.getNomProduit());
        } catch (Exception e) {
            logger.error("DIAGNOSTIC: Erreur détaillée lors de l'envoi de notification d'expiration pour le produit {}: {}",
                    produit.getId(), e.getMessage(), e);
            e.printStackTrace(); // Affiche la stack trace complète
        }
    }
}