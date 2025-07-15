package com.notificationsmessage.service;

import com.notificationsmessage.client.BoutiqueServiceClient;
import com.notificationsmessage.dto.VendeurDTO;
import com.notificationsmessage.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class NotificationScheduler {
    private static final Logger logger = LoggerFactory.getLogger(NotificationScheduler.class);

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private BoutiqueServiceClient boutiqueServiceClient;

    @Autowired
    private NotificationRepository notificationRepository;

    @Scheduled(fixedRate = 172800000)
    public void checkAndSendNotifications() {
        logger.info("Début de la vérification et de l'envoi des notifications");

        try {
            // Récupérer les vendeurs actifs
            List<String> vendeurIds = recupererVendeursActifs();
            logger.info("Vendeurs avec notifications non lues : {}", vendeurIds);

            for (String vendeurId : vendeurIds) {
                int unreadCount = notificationService.countUnreadNotifications(vendeurId);
                logger.info("Vendeur {} a {} notifications non lues", vendeurId, unreadCount);

                if (unreadCount > 0) {
                    try {
                        VendeurDTO vendeur = boutiqueServiceClient.getVendeurById(vendeurId);

                        if (vendeur != null && vendeur.getEmail() != null) {
                            String message = String.format(
                                    "Vous avez %d notification(s) non lue(s). Connectez-vous à votre interface pour les consulter.",
                                    unreadCount
                            );

                            notificationService.envoyerNotificationPersonnalisee(
                                    vendeur.getEmail(),
                                    "SUMMARY",
                                    message
                            );

                            logger.info("Notification de résumé envoyée à {}", vendeur.getEmail());
                        } else {
                            logger.warn("Aucun email trouvé pour le vendeur {}", vendeurId);
                        }
                    } catch (Exception e) {
                        logger.error("Erreur lors du traitement du vendeur {}: {}", vendeurId, e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Erreur lors de la vérification globale des notifications", e);
        }
    }

    // Méthode pour récupérer les vendeurs avec des notifications non lues
    private List<String> recupererVendeursActifs() {
        try {
            List<String> vendeurIds = notificationRepository.findDistinctVendeurIdWithUnreadNotifications();

            if (vendeurIds.isEmpty()) {
                logger.info("Aucun vendeur avec des notifications non lues");
                return Collections.emptyList();
            }

            return vendeurIds;
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération des vendeurs actifs", e);
            return Collections.emptyList();
        }
    }
}