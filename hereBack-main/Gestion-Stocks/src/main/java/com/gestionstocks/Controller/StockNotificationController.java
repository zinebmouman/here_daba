package com.gestionstocks.Controller;

import com.gestionstocks.client.BoutiqueServiceClient;
import com.gestionstocks.model.Stock;
import com.gestionstocks.repository.StockRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.amqp.rabbit.core.RabbitTemplate;


import java.util.Map;

@RestController
@RequestMapping("/api/stock-notifications")
public class StockNotificationController {

    private static final Logger logger = LoggerFactory.getLogger(StockNotificationController.class);

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private BoutiqueServiceClient boutiqueServiceClient; // Client pour interagir avec le service boutique

    @PostMapping("/expiration")
    public ResponseEntity<String> envoyerNotificationExpiration(@RequestBody Map<String, Object> notificationData) {
        try {
            // 1. Récupérer l'ID du stock
            Long stockId = Long.valueOf(notificationData.get("stockId").toString());

            // 2. Récupérer le vendeur associé au stock via la boutique
            String vendeurId = getVendeurIdFromStock(stockId);

            // 3. Ajouter le vendeurId aux données de notification
            notificationData.put("vendeurId", vendeurId);

            // 4. Envoyer via RabbitMQ
            rabbitTemplate.convertAndSend(
                    "stock-notifications",
                    "product.expiration",
                    notificationData);

            logger.info("Notification d'expiration envoyée avec succès pour le produit: {}",
                    notificationData.get("productName"));

            return ResponseEntity.ok("Notification envoyée avec succès");
        } catch (Exception e) {
            logger.error("Erreur lors de l'envoi de la notification d'expiration: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de l'envoi de la notification: " + e.getMessage());
        }
    }

    /**
     * Récupère l'ID du vendeur associé au stock
     */
    private String getVendeurIdFromStock(Long stockId) {
        try {
            // Récupérer le stock
            Stock stock = stockRepository.findById(stockId).orElse(null);
            if (stock == null) {
                logger.warn("Stock non trouvé pour l'ID: {}", stockId);
                return "default-vendeur-id";
            }

            // Récupérer l'ID de la boutique du stock
            // Vérifier si c'est Integer ou Long (adapter selon votre modèle)
            Object idBoutiqueObj = stock.getIdBoutique();
            Long idBoutique = null;

            if (idBoutiqueObj instanceof Integer) {
                idBoutique = ((Integer) idBoutiqueObj).longValue();
            } else if (idBoutiqueObj instanceof Long) {
                idBoutique = (Long) idBoutiqueObj;
            }

            if (idBoutique == null) {
                logger.warn("Le stock {} n'a pas de boutique associée", stockId);
                return "default-vendeur-id";
            }

            // Récupérer l'ID du vendeur de la boutique via le client
            String vendeurId = boutiqueServiceClient.getVendeurIdByBoutiqueId(idBoutique);
            if (vendeurId == null || vendeurId.isEmpty()) {
                logger.warn("Aucun vendeur trouvé pour la boutique {}", idBoutique);
                return "default-vendeur-id";
            }

            logger.info("Vendeur {} trouvé pour le stock {} via la boutique {}",
                    vendeurId, stockId, idBoutique);
            return vendeurId;
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération du vendeur pour le stock {}: {}",
                    stockId, e.getMessage(), e);
            return "default-vendeur-id";
        }
    }
}