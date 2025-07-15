package com.example.authentification.controller;

import com.example.authentification.service.UserSyncService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Contrôleur pour synchroniser les utilisateurs entre Firestore et PostgreSQL
 */
@RestController
@RequestMapping("/api/users/sync")
public class UserSyncController {
    @Autowired
    private UserSyncService userSyncService;
    /**
     * Synchronise tous les utilisateurs depuis Firestore vers PostgreSQL
     * @return Rapport complet de synchronisation
     */
    @GetMapping("/all")
    public ResponseEntity<Map<String, Object>> syncAllUsers() throws Exception {
        Map<String, Object> syncReport = userSyncService.syncAllUsers();
        return ResponseEntity.ok(syncReport);
    }
    @PostMapping("/sync-new-user")
    public ResponseEntity<Map<String, Object>> syncNewUser(
            @RequestBody Map<String, String> userData,
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        try {
            System.out.println("📱 Requête de synchronisation depuis le frontend reçue");
            System.out.println("📊 Données reçues: " + userData);

            // Log pour le token (sécurisé)
            if (authHeader != null) {
                System.out.println("🔑 Token d'autorisation reçu: " +
                        (authHeader.length() > 20 ?
                                authHeader.substring(0, 10) + "..." + authHeader.substring(authHeader.length() - 5) :
                                "Token manquant ou invalide"));
            } else {
                System.out.println("⚠️ Aucun token d'autorisation reçu");
            }

            // Vérifier la présence des champs obligatoires
            if (!userData.containsKey("uid")) {
                System.out.println("❌ Erreur: champ uid manquant");
                return ResponseEntity.badRequest().body(Map.of(
                        "error", true,
                        "message", "Le champ uid est obligatoire"
                ));
            }

            // Ajout d'un displayName par défaut si absent
            if (!userData.containsKey("displayName") && userData.containsKey("email")) {
                System.out.println("ℹ️ Pas de displayName, utilisation de l'email comme fallback");
                userData.put("displayName", userData.get("email").split("@")[0]);
            }

            System.out.println("🔄 Début de la synchronisation avec le service...");
            Map<String, Object> syncReport = userSyncService.syncNewUser(userData);
            System.out.println("✅ Synchronisation réussie: " + syncReport);

            // Ajouter des informations supplémentaires
            syncReport.put("receivedData", userData);
            syncReport.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(syncReport);
        } catch (Exception e) {
            System.out.println("❌ Erreur de synchronisation: " + e.getMessage());
            e.printStackTrace();

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", true);
            errorResponse.put("message", e.getMessage());
            errorResponse.put("cause", e.getCause() != null ? e.getCause().getMessage() : "Unknown");
            errorResponse.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    /**
     * Vérifie si un utilisateur existe déjà dans PostgreSQL
     */
    @GetMapping("/check/{uid}")
    public ResponseEntity<Map<String, Object>> checkUserExists(@PathVariable String uid) {
        Map<String, Object> response = new HashMap<>();
        try {
            boolean exists = userSyncService.checkUserExists(uid);
            response.put("exists", exists);
            response.put("uid", uid);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("error", true);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    /**
     * Synchronise uniquement les vendeurs
     * @return Rapport de synchronisation des vendeurs
     */
    @GetMapping("/vendeurs")
    public ResponseEntity<Map<String, Object>> syncVendeurs() throws Exception {
        Map<String, Object> syncReport = userSyncService.syncVendeurs();
        return ResponseEntity.ok(syncReport);
    }
    /**
     * Synchronise uniquement les livreurs
     * @return Rapport de synchronisation des livreurs
     */
    @GetMapping("/livreurs")
    public ResponseEntity<Map<String, Object>> syncLivreurs() throws Exception {
        Map<String, Object> syncReport = userSyncService.syncLivreurs();
        return ResponseEntity.ok(syncReport);
    }
}

