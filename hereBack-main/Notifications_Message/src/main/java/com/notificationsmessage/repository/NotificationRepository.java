package com.notificationsmessage.repository;

import com.notificationsmessage.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    /**
     * Trouve toutes les notifications pour un vendeur spécifique
     */
    List<Notification> findByVendeurId(String vendeurId);

    // Nouvelle méthode pour supprimer toutes les notifications d'un vendeur
    void deleteAllByVendeurId(String vendeurId);
    /**
     * Trouve les notifications avec un statut spécifique pour un vendeur
     */
    List<Notification> findByVendeurIdAndStatus(String vendeurId, String status);


    // Nouvelle méthode
    @Query("SELECT DISTINCT n.vendeurId FROM Notification n WHERE n.status = 'NON_LU'")
    List<String> findDistinctVendeurIdWithUnreadNotifications();

    /**
     * Compte les notifications avec un statut spécifique pour un vendeur
     */
    int countByVendeurIdAndStatus(String vendeurId, String status);

    /**
     * Trie les notifications par date d'envoi (les plus récentes d'abord)
     */
    List<Notification> findByVendeurIdOrderByDateEnvoiDesc(String vendeurId);
}