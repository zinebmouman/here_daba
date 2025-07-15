package com.example.authentification.controller;
import com.example.authentification.model.User;
import com.example.authentification.service.AuthService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import com.example.authentification.dto.RegisterRequest;
import com.example.authentification.dto.LoginRequest;
import com.example.authentification.dto.AuthResponse;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired
    private AuthService authService;
    // Nouvel endpoint de test avec GET
    @GetMapping("/test")
    public ResponseEntity<String> test() {
        System.out.println("📢 Test API appelée !");
        return ResponseEntity.ok("✅ API d'authentification fonctionne correctement! 👍");
    }
    @GetMapping("/test-api")
    public ResponseEntity<Map<String, Object>> testApi() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "API d'authentification fonctionne correctement");
        response.put("timestamp", System.currentTimeMillis());

        System.out.println("🔍 Test API appelé et répondu avec succès");

        return ResponseEntity.ok(response);
    }
    // Version avec chemin complet pour plus de compatibilité
    @GetMapping("/api/auth/test")
    public ResponseEntity<String> testWithFullPath() {
        System.out.println("📢 Test API avec chemin complet appelée !");
        return ResponseEntity.ok("✅ API d'authentification fonctionne correctement avec chemin complet! 👍");
    }
    // Endpoint racine pour le test direct
    @GetMapping("")
    public ResponseEntity<String> root() {
        System.out.println("📢 Root API appelée !");
        return ResponseEntity.ok("✅ API d'authentification racine fonctionne! 👍");
    }
    @PostMapping("/verify")
    public ResponseEntity<String> verifyUser(@RequestBody String idToken) {
        try {
            System.out.println("📢 Requête reçue sur /api/auth/verify");
            System.out.println("📢 Token brut reçu: " + idToken);
            // Supprimer les guillemets éventuels si le token est envoyé au format JSON
            if (idToken.startsWith("\"") && idToken.endsWith("\"")) {
                idToken = idToken.substring(1, idToken.length() - 1);
            }
            // Nettoyer les caractères spéciaux potentiels comme \n ou \r
            idToken = idToken.trim();

            try {
                boolean isVendeur = authService.isVendeur(idToken);
                if (isVendeur) {
                    return ResponseEntity.ok("✅ Utilisateur est un vendeur.");
                } else {
                    return ResponseEntity.status(403).body("⛔ Accès refusé. Non vendeur.");
                }
            } catch (Exception e) {
                System.out.println("🚨 Erreur spécifique dans la vérification du vendeur: " + e.getMessage());
                // Gérer les erreurs spécifiques ici
                return ResponseEntity.status(500).body("❌ Erreur technique: " + e.getMessage());
            }
        } catch (Exception e) {
            System.out.println("🚨 Erreur générale dans verifyUser: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body("❌ Erreur d'authentification: " + e.getMessage());
        }
    }
    // Endpoint pour tester la réception du token
    @PostMapping("/test-token")
    public ResponseEntity<String> testToken(@RequestBody String token) {
        System.out.println("📢 Test Token API appelée !");
        System.out.println("📢 Token reçu: " + token);
        return ResponseEntity.ok("✅ Token reçu avec succès: " + token.substring(0, Math.min(token.length(), 20)) + "...");
    }
    // Route de debug
    @PostMapping("/debug-token")
    public ResponseEntity<String> debugToken(@RequestBody String idToken) {
        try {
            System.out.println("📢 Debug Token API appelée !");
            System.out.println("📢 Token brut reçu: " + idToken);
            // Supprimer les guillemets éventuels
            if (idToken.startsWith("\"") && idToken.endsWith("\"")) {
                idToken = idToken.substring(1, idToken.length() - 1);
            }
            // Nettoyer les caractères spéciaux potentiels
            idToken = idToken.trim();
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
            return ResponseEntity.ok("✅ UID: " + decodedToken.getUid());
        } catch (Exception e) {
            System.out.println("🚨 Erreur de décodage du token: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body("❌ Erreur Firebase Auth: " + e.getMessage());
        }
    }
    // Endpoint pour vérifier le rôle depuis Firestore
    @PostMapping("/check-firestore-role")
    public ResponseEntity<String> checkFirestoreRole(@RequestBody String idToken) {
        try {
            System.out.println("📢 Vérification du rôle Firestore appelée !");

            // Supprimer les guillemets éventuels
            if (idToken.startsWith("\"") && idToken.endsWith("\"")) {
                idToken = idToken.substring(1, idToken.length() - 1);
            }

            // Nettoyer les caractères spéciaux potentiels
            idToken = idToken.trim();

            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
            String uid = decodedToken.getUid();

            boolean isVendeur = authService.isVendeur(idToken);
            return ResponseEntity.ok("📊 Utilisateur " + uid + " est vendeur: " + isVendeur);
        } catch (Exception e) {
            System.out.println("🚨 Erreur lors de la vérification du rôle: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body("❌ Erreur: " + e.getMessage());
        }
    }


    /**
     * Inscription d'un nouvel utilisateur (toujours en tant que client)
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> registerUser(@RequestBody RegisterRequest request) {
        try {
            System.out.println("📝 Requête d'inscription reçue: " + request);

            // Validation des données
            if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new AuthResponse(false, "Email requis"));
            }

            if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new AuthResponse(false, "Mot de passe requis"));
            }

            if (request.getDisplayName() == null || request.getDisplayName().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new AuthResponse(false, "Nom d'affichage requis"));
            }

            // ✅ CHANGEMENT : Forcer le rôle à "client" - plus de choix de rôle
            String role = "client";

            System.out.println("✅ Validation réussie, création de l'utilisateur client...");

            // Appeler le service d'inscription avec rôle forcé "client"
            Map<String, Object> result = authService.registerUser(
                    request.getEmail().trim(),
                    request.getPassword(),
                    request.getDisplayName().trim(),
                    role
            );

            if ((Boolean) result.get("success")) {
                // Succès
                AuthResponse response = new AuthResponse(true, (String) result.get("message"));
                response.setUid((String) result.get("uid"));
                response.setUserId((Long) result.get("userId"));
                response.setEmail((String) result.get("email"));
                response.setDisplayName((String) result.get("displayName"));
                response.setRole((String) result.get("role"));

                System.out.println("✅ Inscription réussie pour: " + request.getEmail());
                return ResponseEntity.ok(response);
            } else {
                // Échec
                System.out.println("❌ Échec de l'inscription: " + result.get("message"));
                return ResponseEntity.badRequest()
                        .body(new AuthResponse(false, (String) result.get("message")));
            }

        } catch (Exception e) {
            System.out.println("🚨 Erreur lors de l'inscription: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new AuthResponse(false, "Erreur interne du serveur"));
        }
    }

    /**
     * ✅ NOUVELLE MÉTHODE : Inscription via Google
     */
    @PostMapping("/register/google")
    public ResponseEntity<AuthResponse> registerGoogleUser(@RequestBody Map<String, String> googleData) {
        try {
            System.out.println("📝 Requête d'inscription Google reçue");

            // Validation des données Google
            String email = googleData.get("email");
            String displayName = googleData.get("displayName");
            String uid = googleData.get("uid");

            if (email == null || email.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new AuthResponse(false, "Email Google requis"));
            }

            if (displayName == null || displayName.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new AuthResponse(false, "Nom d'affichage Google requis"));
            }

            if (uid == null || uid.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new AuthResponse(false, "UID Google requis"));
            }

            System.out.println("✅ Validation Google réussie, création de l'utilisateur client...");

            // Créer l'utilisateur directement avec les données Google
            Map<String, Object> result = authService.registerGoogleUser(email, displayName, uid);

            if ((Boolean) result.get("success")) {
                // Succès
                AuthResponse response = new AuthResponse(true, (String) result.get("message"));
                response.setUid((String) result.get("uid"));
                response.setUserId((Long) result.get("userId"));
                response.setEmail((String) result.get("email"));
                response.setDisplayName((String) result.get("displayName"));
                response.setRole((String) result.get("role"));

                // Récupérer les rôles
                Map<String, Boolean> roles = new HashMap<>();
                roles.put("client", true);
                roles.put("vendeur", false);
                response.setRoles(roles);

                System.out.println("✅ Inscription Google réussie pour: " + email);
                return ResponseEntity.ok(response);
            } else {
                // Échec
                System.out.println("❌ Échec de l'inscription Google: " + result.get("message"));
                return ResponseEntity.badRequest()
                        .body(new AuthResponse(false, (String) result.get("message")));
            }

        } catch (Exception e) {
            System.out.println("🚨 Erreur lors de l'inscription Google: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new AuthResponse(false, "Erreur interne du serveur"));
        }
    }

    /**
     * ✅ NOUVELLE MÉTHODE : Devenir vendeur (upgrade du rôle client vers vendeur)
     */
    @PostMapping("/become-vendeur")
    public ResponseEntity<AuthResponse> becomeVendeur(@RequestBody String idToken) {
        try {
            System.out.println("🔄 Requête pour devenir vendeur reçue");

            // Nettoyer le token
            if (idToken.startsWith("\"") && idToken.endsWith("\"")) {
                idToken = idToken.substring(1, idToken.length() - 1);
            }
            idToken = idToken.trim();

            // Mettre à jour le rôle vers vendeur
            Map<String, Object> result = authService.upgradeToVendeur(idToken);

            if ((Boolean) result.get("success")) {
                AuthResponse response = new AuthResponse(true, (String) result.get("message"));
                response.setUid((String) result.get("uid"));
                response.setRole("vendeur");

                // Mettre à jour les rôles
                Map<String, Boolean> roles = new HashMap<>();
                roles.put("client", true);
                roles.put("vendeur", true);
                response.setRoles(roles);

                System.out.println("✅ Utilisateur devenu vendeur avec succès");
                return ResponseEntity.ok(response);
            } else {
                System.out.println("❌ Échec de la mise à jour vers vendeur: " + result.get("message"));
                return ResponseEntity.badRequest()
                        .body(new AuthResponse(false, (String) result.get("message")));
            }

        } catch (Exception e) {
            System.out.println("🚨 Erreur lors de la mise à jour vers vendeur: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new AuthResponse(false, "Erreur lors de la mise à jour du rôle"));
        }
    }



    /**
     * Connexion d'un utilisateur
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> loginUser(@RequestBody LoginRequest request) {
        try {
            System.out.println("🔐 Requête de connexion reçue pour: " + request.getEmail());

            // Validation des données
            if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new AuthResponse(false, "Email requis"));
            }

            if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new AuthResponse(false, "Mot de passe requis"));
            }

            System.out.println("✅ Validation réussie, tentative de connexion...");

            // Appeler le service de connexion
            Map<String, Object> result = authService.loginUser(
                    request.getEmail().trim(),
                    request.getPassword()
            );

            if ((Boolean) result.get("success")) {
                // Succès
                AuthResponse response = new AuthResponse(true, (String) result.get("message"));
                response.setUid((String) result.get("uid"));
                response.setUserId((Long) result.get("userId"));
                response.setEmail((String) result.get("email"));
                response.setDisplayName((String) result.get("displayName"));
                response.setRole((String) result.get("role"));
                response.setIdToken((String) result.get("idToken"));
                response.setRoles((Map<String, Boolean>) result.get("roles"));

                System.out.println("✅ Connexion réussie pour: " + request.getEmail());
                return ResponseEntity.ok(response);
            } else {
                // Échec
                System.out.println("❌ Échec de la connexion: " + result.get("message"));
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new AuthResponse(false, (String) result.get("message")));
            }

        } catch (Exception e) {
            System.out.println("🚨 Erreur lors de la connexion: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new AuthResponse(false, "Erreur interne du serveur"));
        }
    }

    /**
     * Endpoint pour récupérer le profil d'un utilisateur connecté
     */
    @PostMapping("/profile")
    public ResponseEntity<AuthResponse> getUserProfile(@RequestBody String idToken) {
        try {
            System.out.println("👤 Requête de profil utilisateur reçue");

            // Nettoyer le token
            if (idToken.startsWith("\"") && idToken.endsWith("\"")) {
                idToken = idToken.substring(1, idToken.length() - 1);
            }
            idToken = idToken.trim();

            // Récupérer l'utilisateur avec tous ses rôles
            User user = authService.getUserWithRoles(idToken);

            if (user != null) {
                AuthResponse response = new AuthResponse(true, "Profil récupéré avec succès");
                response.setUid(user.getIdUser());
                response.setUserId(user.getId());
                response.setDisplayName(user.getNom());
                response.setRole(user.getRole());

                // Récupérer tous les rôles
                Map<String, Boolean> roles = new HashMap<>();
                roles.put("client", user.isClient());
                roles.put("vendeur", user.isVendeur());
                roles.put("livreur", user.isLivreur());
                response.setRoles(roles);

                System.out.println("✅ Profil récupéré pour l'utilisateur: " + user.getIdUser());
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new AuthResponse(false, "Utilisateur non trouvé"));
            }

        } catch (Exception e) {
            System.out.println("🚨 Erreur lors de la récupération du profil: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new AuthResponse(false, "Erreur lors de la récupération du profil"));
        }
    }

    /**
     * Endpoint pour changer le rôle d'un utilisateur
     */
    @PostMapping("/change-role")
    public ResponseEntity<AuthResponse> changeUserRole(
            @RequestParam String uid,
            @RequestParam String newRole,
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        try {
            System.out.println("🔄 Requête de changement de rôle pour UID: " + uid + " vers: " + newRole);

            // Validation du rôle
            if (!isValidRole(newRole)) {
                return ResponseEntity.badRequest()
                        .body(new AuthResponse(false, "Rôle invalide"));
            }

            // TODO: Ajouter une vérification d'autorisation ici
            // Par exemple, vérifier que l'utilisateur qui fait la demande est admin

            // Changer le rôle
            authService.updateUserRoleStatus(uid, createMockFirestoreDocument(uid, newRole));

            System.out.println("✅ Rôle changé avec succès pour: " + uid);
            return ResponseEntity.ok(new AuthResponse(true, "Rôle changé avec succès"));

        } catch (Exception e) {
            System.out.println("🚨 Erreur lors du changement de rôle: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new AuthResponse(false, "Erreur lors du changement de rôle"));
        }
    }

    /**
     * Endpoint de logout (côté serveur - optionnel)
     */
    @PostMapping("/logout")
    public ResponseEntity<AuthResponse> logoutUser(@RequestBody String idToken) {
        try {
            System.out.println("🚪 Requête de déconnexion reçue");

            // Dans un vrai système, vous pourriez invalider le token côté serveur
            // Ici nous simulons juste un logout réussi

            System.out.println("✅ Déconnexion réussie");
            return ResponseEntity.ok(new AuthResponse(true, "Déconnexion réussie"));

        } catch (Exception e) {
            System.out.println("🚨 Erreur lors de la déconnexion: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new AuthResponse(false, "Erreur lors de la déconnexion"));
        }
    }

    /**
     * ✅ MODIFIÉ : Validation des rôles (suppression du livreur)
     */
    private boolean isValidRole(String role) {
        return role != null && (role.equals("client") || role.equals("vendeur"));
    }

    /**
     * Crée un document Firestore simulé pour les tests
     */
    private com.google.cloud.firestore.DocumentSnapshot createMockFirestoreDocument(String uid, String role) {
        // Cette méthode devrait être implémentée selon vos besoins
        // Pour l'instant, retournons null et gérons dans le service
        return null;
    }

}