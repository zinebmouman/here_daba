package com.notificationsmessage.service;

import com.notificationsmessage.dto.NotificationDTO;
import com.notificationsmessage.model.Notification;
import com.notificationsmessage.repository.NotificationRepository;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class NotificationService {
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    @Autowired
    private EmailService emailService;

    @Autowired
    private NotificationRepository notificationRepository;

    /**
     * Récupère toutes les notifications pour un vendeur
     */
    public List<NotificationDTO> getNotificationsForVendeur(String vendeurId) {
        try {
            List<Notification> notifications = notificationRepository.findByVendeurIdOrderByDateEnvoiDesc(vendeurId);

            if (notifications.isEmpty()) {
                logger.warn("Aucune notification trouvée pour le vendeur: {}", vendeurId);
                return Collections.emptyList();
            }

            return notifications.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération des notifications pour le vendeur {}", vendeurId, e);
            throw new RuntimeException("Impossible de récupérer les notifications", e);
        }
    }

    /**
     * Marque une notification comme lue
     */
    @Transactional
    public NotificationDTO markAsRead(Long id) {
        logger.info("Marquage de la notification {} comme lue", id);

        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Notification non trouvée avec l'ID: " + id));

        notification.setStatus("LU");
        notification = notificationRepository.save(notification);

        return convertToDTO(notification);
    }

    /**
     * Marque toutes les notifications d'un vendeur comme lues
     */
    @Transactional
    public List<NotificationDTO> markAllAsRead(String vendeurId) {
        logger.info("Marquage de toutes les notifications comme lues pour le vendeur: {}", vendeurId);

        List<Notification> notifications = notificationRepository.findByVendeurIdAndStatus(vendeurId, "NON_LU");

        for (Notification notification : notifications) {
            notification.setStatus("LU");
            notificationRepository.save(notification);
        }

        return notifications.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Compte le nombre de notifications non lues pour un vendeur
     */
    public int countUnreadNotifications(String vendeurId) {
        logger.info("Comptage des notifications non lues pour le vendeur: {}", vendeurId);
        return notificationRepository.countByVendeurIdAndStatus(vendeurId, "NON_LU");
    }

    /**
     * Supprime une notification
     */
    @Transactional
    public void deleteNotification(Long id) {
        logger.info("Suppression de la notification: {}", id);

        if (notificationRepository.existsById(id)) {
            notificationRepository.deleteById(id);
        } else {
            throw new EntityNotFoundException("Notification non trouvée avec l'ID: " + id);
        }
    }

    /**
     * Envoie une notification par email
     */
    public void envoyerNotificationParEmail(String email, String message) {
        if (email == null || email.trim().isEmpty()) {
            logger.warn("Tentative d'envoi d'email à une adresse vide");
            return;
        }

        String sujet = "Nouvelle notification de votre boutique";
        String corps = createHtmlTemplate("Notification importante", message);
        emailService.sendEmail(email, sujet, corps, true);
    }
    private String createExpirationTemplate(String message) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; margin: 0; padding: 0; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #ffc107; color: #000; padding: 20px; text-align: center; }
                    .content { background-color: #ffffff; padding: 20px; border: 1px solid #ddd; }
                    .alert-box { background-color: #fff3cd; border: 1px solid #ffeeba; color: #856404; padding: 15px; border-radius: 5px; margin: 20px 0; }
                    .action-needed { background-color: #f8f9fa; padding: 15px; border-left: 4px solid #ffc107; margin: 20px 0; }
                    .button { background-color: #007bff; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px; display: inline-block; margin-top: 20px; }
                    .footer { text-align: center; margin-top: 20px; font-size: 12px; color: #666; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>⏰ ALERTE DATE D'EXPIRATION</h1>
                    </div>
                    <div class="content">
                        <p>Cher vendeur,</p>
                        
                        <div class="alert-box">
                            <strong>Important !</strong> %s
                        </div>
                        
                        <div class="action-needed">
                            <h3>Actions recommandées :</h3>
                            <ul>
                                <li>Vérifiez les produits concernés dans votre stock</li>
                                <li>Envisagez une promotion pour écouler rapidement ces produits</li>
                                <li>Mettez à jour les informations sur votre boutique</li>
                                <li>Préparez le retrait des produits expirés</li>
                            </ul>
                        </div>
                        
                        <p>Agir rapidement vous permettra de minimiser les pertes et de garantir la qualité des produits vendus à vos clients.</p>
                        
                        <a href="#" class="button">Voir les produits concernés</a>
                        
                        <p>Cordialement,<br>
                        L'équipe de gestion des stocks</p>
                    </div>
                    <div class="footer">
                        <p>Cet email a été envoyé automatiquement. Pour toute question, contactez notre support.</p>
                        <p>© 2025 Votre Boutique en Ligne - Tous droits réservés</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(message);
    }

    private String createHtmlTemplate(String title, String message) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; margin: 0; padding: 0; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #007bff; color: white; padding: 20px; text-align: center; }
                    .content { background-color: #ffffff; padding: 20px; border: 1px solid #ddd; }
                    .message-box { background-color: #f8f9fa; padding: 15px; border-radius: 5px; margin: 20px 0; }
                    .footer { text-align: center; margin-top: 20px; font-size: 12px; color: #666; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>%s</h1>
                    </div>
                    <div class="content">
                        <p>Cher vendeur,</p>
                        
                        <div class="message-box">
                            <p>%s</p>
                        </div>
                        
                        <p>Merci de votre attention.</p>
                        
                        <p>Cordialement,<br>
                        L'équipe de votre boutique en ligne</p>
                    </div>
                    <div class="footer">
                        <p>Cet email a été envoyé automatiquement. Pour toute question, contactez notre support.</p>
                        <p>© 2025 Votre Boutique en Ligne - Tous droits réservés</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(title, message);
    }
    private String createStockCritiqueTemplate(String message) {
        // Extraction des informations du message
        // Supposons que le message soit au format "Le stock du produit X est critique (Actuel: Y, Seuil: Z)"
        String productInfo = message;

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; margin: 0; padding: 0; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #dc3545; color: white; padding: 20px; text-align: center; }
                    .content { background-color: #ffffff; padding: 20px; border: 1px solid #ddd; }
                    .alert-box { background-color: #fff3cd; border: 1px solid #ffeeba; color: #856404; padding: 15px; border-radius: 5px; margin: 20px 0; }
                    .action-needed { background-color: #f8f9fa; padding: 15px; border-left: 4px solid #dc3545; margin: 20px 0; }
                    .button { background-color: #28a745; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px; display: inline-block; margin-top: 20px; }
                    .footer { text-align: center; margin-top: 20px; font-size: 12px; color: #666; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>⚠️ ALERTE STOCK CRITIQUE</h1>
                    </div>
                    <div class="content">
                        <p>Cher vendeur,</p>
                        
                        <div class="alert-box">
                            <strong>Attention !</strong> %s
                        </div>
                        
                        <div class="action-needed">
                            <h3>Actions recommandées :</h3>
                            <ul>
                                <li>Vérifiez immédiatement le niveau de stock réel du produit</li>
                                <li>Passez une commande de réapprovisionnement d'urgence</li>
                                <li>Mettez à jour la disponibilité du produit sur votre boutique</li>
                                <li>Contactez vos fournisseurs pour accélérer la livraison</li>
                            </ul>
                        </div>
                        
                        <p>Il est crucial d'agir rapidement pour éviter une rupture de stock qui pourrait affecter vos ventes et la satisfaction de vos clients.</p>
                        
                        <a href="#" class="button">Gérer mon stock maintenant</a>
                        
                        <p>Cordialement,<br>
                        L'équipe de gestion des stocks</p>
                    </div>
                    <div class="footer">
                        <p>Cet email a été envoyé automatiquement. Pour toute question, contactez notre support.</p>
                        <p>© 2025 Votre Boutique en Ligne - Tous droits réservés</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(productInfo);
    }
    public void envoyerNotificationPersonnalisee(String email, String type, String message) {
        String sujet = "";
        String corps = "";

        switch (type) {
            case "CRITICAL_STOCK":
                sujet = "⚠️ URGENT : Stock critique détecté";
                corps = createStockCritiqueTemplate(message);
                break;
            case "PRODUCT_EXPIRATION":
                sujet = "⏰ ATTENTION : Produit proche de l'expiration";
                corps = createExpirationTemplate(message);
                break;
            default:
                sujet = "Nouvelle notification de votre boutique";
                corps = createHtmlTemplate("Notification", message);
        }

        emailService.sendEmail(email, sujet, corps, true);
    }


    @Transactional
    public void deleteAllNotificationsByVendeur(String vendeurId) {
        notificationRepository.deleteAllByVendeurId(vendeurId);
    }


    /**
     * Convertit une entité Notification en DTO
     */
    private NotificationDTO convertToDTO(Notification notification) {
        NotificationDTO dto = new NotificationDTO();
        dto.setId(notification.getId());
        dto.setType(notification.getType());
        dto.setMessage(notification.getMessage());
        dto.setDateEnvoi(notification.getDateEnvoi());
        dto.setVendeurId(notification.getVendeurId());
        dto.setProduitId(notification.getProduitId());
        dto.setStatus(notification.getStatus());
        return dto;
    }
}