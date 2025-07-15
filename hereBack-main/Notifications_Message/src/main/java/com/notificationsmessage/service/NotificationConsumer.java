package com.notificationsmessage.service;

import com.notificationsmessage.client.BoutiqueServiceClient;
import com.notificationsmessage.config.RabbitMQConfig;
import com.notificationsmessage.dto.NotificationMessage;
import com.notificationsmessage.dto.VendeurDTO;
import com.notificationsmessage.model.Notification;
import com.notificationsmessage.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class NotificationConsumer {
    private static final Logger logger = LoggerFactory.getLogger(NotificationConsumer.class);

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private BoutiqueServiceClient boutiqueServiceClient;

    @RabbitListener(queues = {
            RabbitMQConfig.QUEUE_CRITICAL_STOCK,
            RabbitMQConfig.QUEUE_PRODUCT_EXPIRATION
    })
    public void receiveNotification(@Payload NotificationMessage message) {
        try {
            logger.info("Message de notification reçu: {}", message);
            logger.info("Type de message: {}", message.getType());

            if ("PRODUCT_EXPIRATION".equals(message.getType())) {
                logger.info("Traitement d'une notification d'expiration de produit: {}", message.getProductName());
                logger.info("Date d'expiration: {}", message.getDateExpiration());
                logger.info("Jours restants: {}", message.getJoursRestants());
                logger.info("Message temps: {}", message.getMessageTemps());
            }

            // Validation du message
            validateMessage(message);

            // Création de la notification
            Notification notification = createNotificationFromMessage(message);
            logger.info("Notification créée: {}", notification.getMessage());

            // Traitement et envoi de la notification
            processNotification(notification);

        } catch (Exception e) {
            logger.error("Erreur lors du traitement de la notification", e);
        }
    }

    private String generateMessage(NotificationMessage message) {
        String type = message.getType();
        String productName = message.getProductName();

        switch (type) {
            case "CRITICAL_STOCK":
                return String.format(
                        "Le stock du produit %s est critique. Niveau actuel: %s, Seuil d'alerte: %s",
                        productName,
                        message.getCurrentStock(),
                        message.getSeuilCritique()
                );
            case "PRODUCT_EXPIRATION":
                String messageTemps = message.getMessageTemps() != null ?
                        message.getMessageTemps() : "dans moins d'une semaine";

                return String.format(
                        "Le produit %s expire bientôt (%s). Date d'expiration: %s",
                        productName,
                        messageTemps,
                        message.getDateExpiration()
                );
            default:
                return "Notification générique pour le produit " + productName;
        }
    }

    private String construireObjetEmail(String type) {
        switch (type) {
            case "CRITICAL_STOCK":
                return "URGENT : Stock critique";
            case "PRODUCT_EXPIRATION":
                return "ATTENTION : Produit proche de l'expiration";
            default:
                return "Nouvelle notification";
        }
    }
    private void sendEmailNotification(VendeurDTO vendeur, Notification notification) {
        if (vendeur == null || vendeur.getEmail() == null) {
            logger.warn("Impossible d'envoyer l'email - Vendeur introuvable");
            return;
        }

        // Vérification de l'email
        logger.info("Tentative d'envoi d'email à {} pour notification type {}",
                vendeur.getEmail(), notification.getType());

        if (vendeur.getEmail() != null && !vendeur.getEmail().trim().isEmpty()) {
            try {
                // Utiliser la méthode envoyerNotificationPersonnalisee au lieu de envoyerNotificationParEmail
                notificationService.envoyerNotificationPersonnalisee(
                        vendeur.getEmail(),
                        notification.getType(),
                        notification.getMessage()
                );
                logger.info("Notification envoyée par email à {}", vendeur.getEmail());
            } catch (Exception e) {
                logger.error("Erreur lors de l'envoi de l'email à {}: {}", vendeur.getEmail(), e.getMessage(), e);
            }
        } else {
            logger.warn("Aucun email trouvé pour le vendeur {}", notification.getVendeurId());
        }
    }
    private String construireCorpsEmail(Notification notification) {
        StringBuilder body = new StringBuilder();
        body.append("Détails de la notification :\n\n");

        switch (notification.getType()) {
            case "CRITICAL_STOCK":
                body.append("Type : Alerte Stock Critique\n")
                        .append("Message : ").append(notification.getMessage()).append("\n")
                        .append("Date : ").append(notification.getDateEnvoi());
                break;
            case "PRODUCT_EXPIRATION":
                body.append("Type : Expiration de Produit\n")
                        .append("Message : ").append(notification.getMessage()).append("\n")
                        .append("Date : ").append(notification.getDateEnvoi());
                break;
            default:
                body.append("Type : Notification Générique\n")
                        .append("Message : ").append(notification.getMessage()).append("\n")
                        .append("Date : ").append(notification.getDateEnvoi());
        }

        return body.toString();
    }
    /**
     * Valide que le message contient toutes les informations nécessaires
     */
    private void validateMessage(NotificationMessage message) {
        if (message == null) {
            throw new IllegalArgumentException("Le message ne peut pas être null");
        }

        if (message.getType() == null) {
            throw new IllegalArgumentException("Le message doit contenir un type");
        }

        if (message.getVendeurId() == null) {
            throw new IllegalArgumentException("Le message doit contenir un vendeurId");
        }

        if (message.getProductName() == null) {
            throw new IllegalArgumentException("Le message doit contenir un productName");
        }
    }

    /**
     * Crée un objet Notification à partir du message reçu
     */
    private Notification createNotificationFromMessage(NotificationMessage message) {
        Notification notification = new Notification();
        notification.setType(message.getType());
        notification.setMessage(generateMessage(message));
        notification.setDateEnvoi(LocalDateTime.now());
        notification.setProduitId(message.getProductId());
        notification.setVendeurId(message.getVendeurId());
        notification.setStatus("NON_LU"); // Assurez-vous que c'est non lu par défaut

        return notification;
    }

    /**
     * Traite la notification et l'envoie
     */
    private void processNotification(Notification notification) {
        try {
            // Sauvegarde de la notification
            Notification savedNotification = notificationRepository.save(notification);
            logger.info("Notification sauvegardée avec succès, ID: {}, Type: {}",
                    savedNotification.getId(), savedNotification.getType());

            // Récupération des détails du vendeur
            VendeurDTO vendeur = findVendeurDetails(notification.getVendeurId());

            if (vendeur != null) {
                logger.info("Vendeur trouvé: {} ({})", vendeur.getNom(), vendeur.getEmail());
            } else {
                logger.warn("Vendeur non trouvé pour ID: {}", notification.getVendeurId());
            }

            // Envoi de la notification par email si possible
            sendEmailNotification(vendeur, notification);

        } catch (Exception e) {
            logger.error("Erreur lors du traitement de la notification pour le vendeur {}",
                    notification.getVendeurId(), e);
        }
    }

    /**
     * Trouve les détails du vendeur
     */
    private VendeurDTO findVendeurDetails(String vendeurId) {
        try {
            VendeurDTO vendeur = boutiqueServiceClient.getVendeurById(vendeurId);

            if (vendeur == null) {
                logger.warn("Aucun vendeur trouvé pour l'ID: {}", vendeurId);
            }

            return vendeur;
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération des détails du vendeur {}", vendeurId, e);
            return null;
        }
    }
}