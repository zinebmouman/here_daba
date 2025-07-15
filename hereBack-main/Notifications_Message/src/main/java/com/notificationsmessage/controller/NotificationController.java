package com.notificationsmessage.controller;

import com.notificationsmessage.dto.NotificationDTO;
import com.notificationsmessage.service.NotificationService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private static final Logger logger = LoggerFactory.getLogger(NotificationController.class);

    @Autowired
    private NotificationService notificationService;

    /**
     * Récupère les notifications pour un vendeur spécifique
     */
    @GetMapping("/vendeur/{vendeurId}")
    public ResponseEntity<List<NotificationDTO>> getNotificationsForVendeur(
            @PathVariable String vendeurId) {
        logger.info("Récupération des notifications pour le vendeur: {}", vendeurId);
        List<NotificationDTO> notifications = notificationService.getNotificationsForVendeur(vendeurId);
        return ResponseEntity.ok(notifications);
    }

    /**
     * Marque une notification comme lue
     */
    @PutMapping("/{id}/markAsRead")
    public ResponseEntity<NotificationDTO> markAsRead(@PathVariable Long id) {
        logger.info("Marquage de la notification {} comme lue", id);
        NotificationDTO notification = notificationService.markAsRead(id);
        return ResponseEntity.ok(notification);
    }

    /**
     * Marque toutes les notifications d'un vendeur comme lues
     */
    @PutMapping("/markAllAsRead")
    public ResponseEntity<List<NotificationDTO>> markAllAsRead(
            @RequestParam String vendeurId) {
        logger.info("Marquage de toutes les notifications comme lues pour le vendeur: {}", vendeurId);
        List<NotificationDTO> notifications = notificationService.markAllAsRead(vendeurId);
        return ResponseEntity.ok(notifications);
    }

    /**
     * Compte le nombre de notifications non lues pour un vendeur
     */

    @GetMapping("/vendeur/{vendeurId}/count")
    public ResponseEntity<Integer> getUnreadNotificationsCount(@PathVariable String vendeurId) {
        // Log détaillé
        logger.info("Requête de comptage des notifications pour vendeurId: {}", vendeurId);

        // Comptage des notifications
        int count = notificationService.countUnreadNotifications(vendeurId);

        logger.info("Nombre de notifications non lues trouvées: {}", count);

        // Retourner le nombre avec un log supplémentaire
        return ResponseEntity.ok(count);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteNotification(@PathVariable Long id, HttpServletRequest request) {
        logger.info("================== DELETE REQUEST ==================");
        logger.info("Received DELETE request for notification: " + id);
        logger.info("Request URI: " + request.getRequestURI());
        logger.info("Remote Address: " + request.getRemoteAddr());
        logger.info("Headers:");
        Collections.list(request.getHeaderNames()).forEach(headerName ->
                logger.info(headerName + ": " + request.getHeader(headerName))
        );
        logger.info("=====================================");

        try {
            notificationService.deleteNotification(id);
            logger.info("Notification {} successfully deleted", id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Error deleting notification {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body("Erreur lors de la suppression de la notification");
        }
    }
}