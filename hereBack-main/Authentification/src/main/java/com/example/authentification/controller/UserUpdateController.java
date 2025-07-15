package com.example.authentification.controller;

import com.example.authentification.service.UserSyncService;
import com.google.cloud.firestore.Firestore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/update")
@RequiredArgsConstructor
public class UserUpdateController {
    @Autowired
    private UserSyncService userSyncService;

    @Autowired
    private Firestore firestore;
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(UserUpdateController.class);



    @PostMapping("/role")
    public ResponseEntity<String> updateUserRole(
            @RequestParam String uid,
            @RequestParam String newRole
    ) {
        try {
            log.info("🔄 Tentative de mise à jour du rôle pour l'utilisateur: {} vers {}", uid, newRole);

            // Validation des paramètres
            if (uid == null || uid.isEmpty()) {
                log.warn("❌ Identifiant utilisateur invalide");
                return ResponseEntity.badRequest().body("Identifiant utilisateur requis");
            }

            if (newRole == null || newRole.isEmpty()) {
                log.warn("❌ Rôle invalide");
                return ResponseEntity.badRequest().body("Rôle invalide");
            }

            // Mise à jour du rôle
            userSyncService.updateUserRole(uid, newRole);

            log.info("✅ Rôle mis à jour avec succès pour l'utilisateur: {}", uid);
            return ResponseEntity.ok("Rôle mis à jour avec succès");
        } catch (Exception e) {
            log.error("❌ Erreur lors de la mise à jour du rôle", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de la mise à jour du rôle : " + e.getMessage());
        }
    }

    @PostMapping("/role-conflict")
    public ResponseEntity<String> handleRoleConflict(
            @RequestParam String uid,
            @RequestParam String oldRole,
            @RequestParam String newRole
    ) {
        try {
            log.info("🔄 Gestion du conflit de rôle pour l'utilisateur: {} de {} vers {}",
                    uid, oldRole, newRole);

            userSyncService.handleRoleConflict(uid, oldRole, newRole);

            log.info("✅ Conflit de rôle résolu pour l'utilisateur: {}", uid);
            return ResponseEntity.ok("Conflit de rôle résolu avec succès");
        } catch (Exception e) {
            log.error("❌ Erreur lors de la résolution du conflit de rôle", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de la résolution du conflit : " + e.getMessage());
        }
    }
}