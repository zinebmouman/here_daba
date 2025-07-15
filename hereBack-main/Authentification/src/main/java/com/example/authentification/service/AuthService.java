package com.example.authentification.service;
import com.example.authentification.model.Livreur;
import com.example.authentification.model.User;
import com.example.authentification.model.Vendeur;
import com.example.authentification.repository.LivreurRepository;
import com.example.authentification.repository.UserRepository;
import com.example.authentification.repository.VendeurRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.DocumentSnapshot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
@Service
public class AuthService {
    @Autowired
    private VendeurRepository vendeurRepository;
    @Autowired
    private LivreurRepository livreurRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private Firestore firestore;
    /**
     * Vérifie si l'utilisateur est un vendeur
     */
    public boolean isVendeur(String idToken) {
        System.out.println(" Vérification du rôle vendeur");
        try {
            // Nettoyer le token
            idToken = cleanToken(idToken);

            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
            String uid = decodedToken.getUid();
            System.out.println(" UID Firebase: " + uid);

            // Vérifier d'abord dans PostgreSQL par l'idVendeur qui est l'UID Firebase
            Optional<Vendeur> vendeurOptional = vendeurRepository.findByIdVendeur(uid);
            System.out.println(" Recherche dans PostgreSQL (vendeur): " + vendeurOptional);

            if (vendeurOptional.isPresent()) {
                System.out.println("✅ Vendeur trouvé dans PostgreSQL");
                return "vendeur".equals(vendeurOptional.get().getRole());
            }

            // Si non trouvé dans PostgreSQL, vérifier dans Firestore
            try {
                DocumentSnapshot document = firestore.collection("users").document(uid).get().get();
                if (document.exists()) {
                    String role = document.getString("role");
                    String nom = document.getString("displayName");
                    System.out.println("✅ Rôle trouvé dans Firestore: " + role);

                    // Si l'utilisateur est un vendeur, l'enregistrer dans PostgreSQL
                    if ("vendeur".equals(role)) {
                        String email = document.getString("email");
                        saveVendeur(uid, role, nom, email);
                        // ...
                        return true;
                    }
                } else {
                    System.out.println("⚠️ Utilisateur non trouvé dans Firestore");
                }
            } catch (Exception firestoreError) {
                System.out.println("🚨 Erreur d'accès à Firestore: " + firestoreError.getMessage());
                // En cas d'erreur avec Firestore, nous nous replions sur PostgreSQL uniquement
                // Si nous sommes arrivés ici, nous savons déjà qu'il n'est pas vendeur dans PostgreSQL
                return false;
            }

            return false;
        } catch (Exception e) {
            System.out.println("🚨 Erreur dans isVendeur: " + e.getMessage());
            e.printStackTrace();
            return false;  // Retourner false au lieu de propager l'exception
        }
    }

    /**
     * Vérifie si l'utilisateur est un livreur
     */
    public boolean isLivreur(String idToken) throws Exception {
        System.out.println("🔍 Vérification du rôle livreur");
        try {
            // Nettoyer le token
            idToken = cleanToken(idToken);

            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
            String uid = decodedToken.getUid();
            System.out.println("✅ UID Firebase: " + uid);

            // Vérifier d'abord dans PostgreSQL par l'idLivreur qui est l'UID Firebase
            Optional<Livreur> livreurOptional = livreurRepository.findByIdLivreur(uid);
            System.out.println("🔍 Recherche dans PostgreSQL (livreur): " + livreurOptional);

            if (livreurOptional.isPresent()) {
                System.out.println("✅ Livreur trouvé dans PostgreSQL");
                return "livreur".equals(livreurOptional.get().getRole());
            }

            // Si non trouvé dans PostgreSQL, vérifier dans Firestore
            DocumentSnapshot document = firestore.collection("users").document(uid).get().get();

            if (document.exists()) {
                String role = document.getString("role");
                String nom = document.getString("displayName");
                System.out.println("✅ Rôle trouvé dans Firestore: " + role);

                // Si l'utilisateur est un livreur, l'enregistrer dans PostgreSQL
                if ("livreur".equals(role)) {
                    saveLivreur(uid, role, nom);
                    // Mettre également à jour le statut dans la table users
                    updateUserRoleStatus(uid, document);
                    return true;
                }
            } else {
                System.out.println("⚠️ Utilisateur non trouvé dans Firestore");
            }
            return false;
        } catch (Exception e) {
            System.out.println("🚨 Erreur dans isLivreur: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Récupère ou crée l'utilisateur complet avec tous ses rôles
     */
    @Transactional
    public User getUserWithRoles(String idToken) throws Exception {
        try {
            // Nettoyer le token
            idToken = cleanToken(idToken);

            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
            String uid = decodedToken.getUid();

            // Vérifier d'abord dans PostgreSQL par l'idUser qui est l'UID Firebase
            Optional<User> userOptional = userRepository.findByIdUser(uid);

            if (userOptional.isPresent()) {
                return userOptional.get();
            }

            // Si non trouvé dans PostgreSQL, vérifier et créer depuis Firestore
            DocumentSnapshot document = firestore.collection("users").document(uid).get().get();

            if (document.exists()) {
                return createOrUpdateUserFromFirestore(uid, document);
            } else {
                throw new Exception("Utilisateur non trouvé dans Firestore");
            }
        } catch (Exception e) {
            System.out.println("🚨 Erreur dans getUserWithRoles: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Crée ou met à jour un utilisateur avec les données de Firestore
     */
    @Transactional
    public User createOrUpdateUserFromFirestore(String uid, DocumentSnapshot document) {
        Optional<User> userOptional = userRepository.findByIdUser(uid);
        User user;

        if (userOptional.isPresent()) {
            user = userOptional.get();
        } else {
            user = new User();
            user.setIdUser(uid);
        }

        // Informations de base
        user.setNom(document.getString("displayName"));

        // Définir le rôle principal
        String role = document.getString("role");
        if (role == null) {
            role = "client"; // Par défaut
        }
        user.setRole(role);

        // Définir les rôles spécifiques
        user.setClient(true); // Tous les utilisateurs sont des clients
        user.setVendeur("vendeur".equals(role));
        user.setLivreur("livreur".equals(role));

        // Sauvegarder dans PostgreSQL
        return userRepository.save(user);
    }

    /**
     * Met à jour le statut des rôles d'un utilisateur
     */
    @Transactional
    public void updateUserRoleStatus(String uid, DocumentSnapshot document) {
        Optional<User> userOptional = userRepository.findByIdUser(uid);
        User user;

        if (userOptional.isPresent()) {
            user = userOptional.get();
        } else {
            user = new User();
            user.setIdUser(uid);
            user.setNom(document.getString("displayName"));
        }

        String role = document.getString("role");

        // Réinitialiser tous les rôles
        user.setClient(false);
        user.setVendeur(false);
        user.setLivreur(false);

        // Définir le nouveau rôle
        if ("vendeur".equals(role)) {
            user.setVendeur(true);
            user.setClient(true);
            // Récupérer l'email depuis le document Firestore
            String email = document.getString("email");
            saveVendeur(uid, role, user.getNom(), email);
        } else if ("livreur".equals(role)) {
            user.setLivreur(true);
            user.setClient(true);
            // Sauvegarder dans la table livreurs
            saveLivreur(uid, role, user.getNom());
        } else {
            user.setClient(true);
        }

        user.setRole(role);
        userRepository.save(user);

        System.out.println("🔄 Rôle mis à jour pour l'utilisateur : " + uid + " - Nouveau rôle : " + role);
    }

    /**
     * Enregistre un vendeur dans PostgreSQL
     */
    @Transactional
    public Vendeur saveVendeur(String uid, String role, String nom, String email) {
        System.out.println("📝 Tentative d'enregistrement du vendeur avec UID : " + uid);

        // Vérifier si le vendeur existe déjà
        Optional<Vendeur> vendeurExistant = vendeurRepository.findByIdVendeur(uid);

        Vendeur vendeur;
        if (vendeurExistant.isPresent()) {
            vendeur = vendeurExistant.get();
        } else {
            vendeur = new Vendeur();
            vendeur.setIdVendeur(uid);
        }

        vendeur.setRole(role);
        vendeur.setNom(nom);
        vendeur.setEmail(email);  // Stockage de l'email

        Vendeur savedVendeur = vendeurRepository.save(vendeur);
        System.out.println("✅ Vendeur enregistré dans PostgreSQL avec UID : " + savedVendeur.getIdVendeur());
        return savedVendeur;
    }

    /**
     * Enregistre un livreur dans PostgreSQL
     */
    @Transactional
    public Livreur saveLivreur(String uid, String role, String nom) {
        System.out.println("📝 Tentative d'enregistrement du livreur avec UID : " + uid);

        // Vérifier si le livreur existe déjà
        Optional<Livreur> livreurExistant = livreurRepository.findByIdLivreur(uid);

        Livreur livreur;
        if (livreurExistant.isPresent()) {
            livreur = livreurExistant.get();
        } else {
            livreur = new Livreur();
            livreur.setIdLivreur(uid);
        }

        livreur.setRole(role);
        livreur.setNom(nom);

        Livreur savedLivreur = livreurRepository.save(livreur);
        System.out.println("✅ Livreur enregistré dans PostgreSQL avec UID : " + savedLivreur.getIdLivreur());
        return savedLivreur;
    }

    /**
     * Nettoie le token des caractères indésirables
     */
    private String cleanToken(String token) {
        if (token.startsWith("\"") && token.endsWith("\"")) {
            token = token.substring(1, token.length() - 1);
        }
        return token.trim();
    }

    /**
     * Récupère l'ensemble des rôles d'un utilisateur
     */
    public Map<String, Boolean> getUserRoles(String idToken) throws Exception {
        Map<String, Boolean> roles = new HashMap<>();
        roles.put("client", true); // Par défaut

        try {
            // Nettoyer le token
            idToken = cleanToken(idToken);

            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
            String uid = decodedToken.getUid();

            // Vérifier dans la table unifiée des utilisateurs
            Optional<User> userOptional = userRepository.findByIdUser(uid);

            if (userOptional.isPresent()) {
                User user = userOptional.get();
                roles.put("vendeur", user.isVendeur());
                roles.put("livreur", user.isLivreur());
                roles.put("client", user.isClient());
                return roles;
            }

            // Si non trouvé, vérifier dans les tables spécifiques
            Optional<Vendeur> vendeur = vendeurRepository.findByIdVendeur(uid);
            if (vendeur.isPresent()) {
                roles.put("vendeur", true);
            }

            Optional<Livreur> livreur = livreurRepository.findByIdLivreur(uid);
            if (livreur.isPresent()) {
                roles.put("livreur", true);
            }

            return roles;
        } catch (Exception e) {
            System.out.println("🚨 Erreur dans getUserRoles: " + e.getMessage());
            roles.put("error", true);
            return roles;
        }
    }

    // Ajoutez ces méthodes dans votre AuthService existant

    /**
     * Inscrit un nouvel utilisateur avec Firebase Auth et synchronise avec PostgreSQL
     */
    @Transactional
    public Map<String, Object> registerUser(String email, String password, String displayName, String role) {
        Map<String, Object> response = new HashMap<>();

        try {
            System.out.println("📝 Début de l'inscription pour: " + email + " avec le rôle: " + role);

            // 1. Créer l'utilisateur dans Firebase Auth
            Map<String, Object> firebaseUser = createFirebaseUser(email, password, displayName, role);
            String uid = (String) firebaseUser.get("uid");

            System.out.println("✅ Utilisateur créé dans Firebase Auth avec UID: " + uid);

            // 2. Créer l'utilisateur dans PostgreSQL
            User user = createUserInPostgreSQL(uid, displayName, email, role);

            System.out.println("✅ Utilisateur créé dans PostgreSQL avec ID: " + user.getId());

            // 3. Préparer la réponse
            response.put("success", true);
            response.put("message", "Utilisateur créé avec succès");
            response.put("uid", uid);
            response.put("userId", user.getId());
            response.put("email", email);
            response.put("displayName", displayName);
            response.put("role", role);

            return response;

        } catch (Exception e) {
            System.out.println("🚨 Erreur lors de l'inscription: " + e.getMessage());
            e.printStackTrace();

            response.put("success", false);
            response.put("message", "Erreur lors de l'inscription: " + e.getMessage());
            return response;
        }
    }

    /**
     * Connecte un utilisateur avec email/password et retourne les informations
     */
    public Map<String, Object> loginUser(String email, String password) {
        Map<String, Object> response = new HashMap<>();

        try {
            System.out.println("🔐 Tentative de connexion pour: " + email);

            // 1. Vérifier les credentials avec Firebase Auth
            Map<String, Object> firebaseAuth = authenticateWithFirebase(email, password);

            if (!(Boolean) firebaseAuth.get("success")) {
                response.put("success", false);
                response.put("message", "Email ou mot de passe incorrect");
                return response;
            }

            String uid = (String) firebaseAuth.get("uid");
            String idToken = (String) firebaseAuth.get("idToken");

            System.out.println("✅ Authentification Firebase réussie pour UID: " + uid);

            // 2. Récupérer les informations utilisateur depuis PostgreSQL
            Optional<User> userOptional = userRepository.findByIdUser(uid);

            if (userOptional.isEmpty()) {
                // Synchroniser l'utilisateur depuis Firestore si pas trouvé
                User user = syncUserFromFirestore(uid);
                if (user == null) {
                    response.put("success", false);
                    response.put("message", "Utilisateur non trouvé dans le système");
                    return response;
                }
                userOptional = Optional.of(user);
            }

            User user = userOptional.get();

            // 3. Récupérer tous les rôles
            Map<String, Boolean> roles = getUserRolesMap(user);

            // 4. Préparer la réponse
            response.put("success", true);
            response.put("message", "Connexion réussie");
            response.put("uid", uid);
            response.put("userId", user.getId());
            response.put("email", email);
            response.put("displayName", user.getNom());
            response.put("role", user.getRole());
            response.put("idToken", idToken);
            response.put("roles", roles);

            System.out.println("✅ Connexion réussie pour: " + email);
            return response;

        } catch (Exception e) {
            System.out.println("🚨 Erreur lors de la connexion: " + e.getMessage());
            e.printStackTrace();

            response.put("success", false);
            response.put("message", "Erreur lors de la connexion: " + e.getMessage());
            return response;
        }
    }

    /**
     * Crée un utilisateur dans Firebase Auth et Firestore
     */
    private Map<String, Object> createFirebaseUser(String email, String password, String displayName, String role) throws Exception {
        Map<String, Object> response = new HashMap<>();

        try {
            // Créer l'utilisateur dans Firebase Auth via l'API REST
            Map<String, Object> firebaseResponse = createFirebaseAuthUser(email, password, displayName);
            String uid = (String) firebaseResponse.get("localId");

            // Créer le document utilisateur dans Firestore
            Map<String, Object> userData = new HashMap<>();
            userData.put("email", email);
            userData.put("displayName", displayName);
            userData.put("role", role != null ? role : "client");
            userData.put("createdAt", com.google.cloud.Timestamp.now());

            firestore.collection("users").document(uid).set(userData).get();

            response.put("uid", uid);
            response.put("success", true);

            return response;

        } catch (Exception e) {
            throw new Exception("Erreur lors de la création dans Firebase: " + e.getMessage(), e);
        }
    }

    /**
     * Crée un utilisateur dans Firebase Auth via l'API REST
     */
    private Map<String, Object> createFirebaseAuthUser(String email, String password, String displayName) throws Exception {
        // Cette méthode utiliserait l'API REST Firebase Auth
        // Pour simplifier, nous supposons que vous avez configuré Firebase Admin SDK

        com.google.firebase.auth.UserRecord.CreateRequest request = new com.google.firebase.auth.UserRecord.CreateRequest()
                .setEmail(email)
                .setPassword(password)
                .setDisplayName(displayName)
                .setEmailVerified(false);

        com.google.firebase.auth.UserRecord userRecord = FirebaseAuth.getInstance().createUser(request);

        Map<String, Object> response = new HashMap<>();
        response.put("localId", userRecord.getUid());
        response.put("email", userRecord.getEmail());
        response.put("displayName", userRecord.getDisplayName());

        return response;
    }

    /**
     * Authentifie un utilisateur avec Firebase (simulation)
     */
    private Map<String, Object> authenticateWithFirebase(String email, String password) {
        Map<String, Object> response = new HashMap<>();

        try {
            // Dans un vrai système, vous utiliseriez l'API REST Firebase Auth
            // Ici nous simulons en vérifiant si l'utilisateur existe

            // Rechercher l'utilisateur par email dans Firestore
            com.google.cloud.firestore.Query query = firestore.collection("users").whereEqualTo("email", email);
            com.google.cloud.firestore.QuerySnapshot querySnapshot = query.get().get();

            if (querySnapshot.isEmpty()) {
                response.put("success", false);
                response.put("message", "Utilisateur non trouvé");
                return response;
            }

            com.google.cloud.firestore.QueryDocumentSnapshot document = querySnapshot.getDocuments().get(0);
            String uid = document.getId();

            // Dans un vrai système, vous vérifieriez le mot de passe
            // Ici nous simulons la génération d'un token
            String simulatedToken = generateSimulatedToken(uid, email);

            response.put("success", true);
            response.put("uid", uid);
            response.put("idToken", simulatedToken);

            return response;

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Erreur d'authentification: " + e.getMessage());
            return response;
        }
    }

    /**
     * Génère un token simulé (pour les tests)
     */
    private String generateSimulatedToken(String uid, String email) {
        // Dans un vrai système, Firebase Auth génère le token JWT
        return "simulated_token_" + uid + "_" + System.currentTimeMillis();
    }



    /**
     * Synchronise un utilisateur depuis Firestore
     */
    private User syncUserFromFirestore(String uid) {
        try {
            com.google.cloud.firestore.DocumentSnapshot document = firestore.collection("users").document(uid).get().get();

            if (document.exists()) {
                return createOrUpdateUserFromFirestore(uid, document);
            }

            return null;
        } catch (Exception e) {
            System.out.println("🚨 Erreur lors de la synchronisation depuis Firestore: " + e.getMessage());
            return null;
        }
    }

    /**
     * Récupère tous les rôles d'un utilisateur sous forme de Map
     */
    private Map<String, Boolean> getUserRolesMap(User user) {
        Map<String, Boolean> roles = new HashMap<>();
        roles.put("client", user.isClient());
        roles.put("vendeur", user.isVendeur());
        roles.put("livreur", user.isLivreur());
        return roles;
    }

    /**
     * ✅ NOUVELLE MÉTHODE : Inscription via Google (utilisateur déjà créé dans Firebase)
     */
    @Transactional
    public Map<String, Object> registerGoogleUser(String email, String displayName, String uid) {
        Map<String, Object> response = new HashMap<>();

        try {
            System.out.println("📝 Début de l'inscription Google pour: " + email + " avec UID: " + uid);

            // Vérifier si l'utilisateur existe déjà
            Optional<User> existingUser = userRepository.findByIdUser(uid);
            if (existingUser.isPresent()) {
                response.put("success", true);
                response.put("message", "Utilisateur déjà existant");
                response.put("uid", uid);
                response.put("userId", existingUser.get().getId());
                response.put("email", email);
                response.put("displayName", displayName);
                response.put("role", existingUser.get().getRole());
                return response;
            }

            // Créer l'utilisateur dans Firestore (s'il n'existe pas déjà)
            Map<String, Object> userData = new HashMap<>();
            userData.put("email", email);
            userData.put("displayName", displayName);
            userData.put("role", "client"); // Toujours client par défaut
            userData.put("createdAt", com.google.cloud.Timestamp.now());

            try {
                // Vérifier si le document existe déjà dans Firestore
                DocumentSnapshot existingDoc = firestore.collection("users").document(uid).get().get();
                if (!existingDoc.exists()) {
                    firestore.collection("users").document(uid).set(userData).get();
                    System.out.println("✅ Utilisateur créé dans Firestore");
                } else {
                    System.out.println("ℹ️ Utilisateur déjà existant dans Firestore");
                }
            } catch (Exception e) {
                System.out.println("⚠️ Erreur Firestore (non critique): " + e.getMessage());
            }

            // Créer l'utilisateur dans PostgreSQL
            User user = createUserInPostgreSQL(uid, displayName, email, "client");

            System.out.println("✅ Utilisateur Google créé dans PostgreSQL avec ID: " + user.getId());

            // Préparer la réponse
            response.put("success", true);
            response.put("message", "Inscription Google réussie");
            response.put("uid", uid);
            response.put("userId", user.getId());
            response.put("email", email);
            response.put("displayName", displayName);
            response.put("role", "client");

            return response;

        } catch (Exception e) {
            System.out.println("🚨 Erreur lors de l'inscription Google: " + e.getMessage());
            e.printStackTrace();

            response.put("success", false);
            response.put("message", "Erreur lors de l'inscription Google: " + e.getMessage());
            return response;
        }
    }

    /**
     * ✅ NOUVELLE MÉTHODE : Upgrade d'un client vers vendeur
     */
    @Transactional
    public Map<String, Object> upgradeToVendeur(String idToken) {
        Map<String, Object> response = new HashMap<>();

        try {
            // Nettoyer et vérifier le token
            idToken = cleanToken(idToken);
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
            String uid = decodedToken.getUid();

            System.out.println("🔄 Upgrade vers vendeur pour UID: " + uid);

            // Récupérer l'utilisateur existant
            Optional<User> userOptional = userRepository.findByIdUser(uid);
            if (userOptional.isEmpty()) {
                response.put("success", false);
                response.put("message", "Utilisateur non trouvé");
                return response;
            }

            User user = userOptional.get();

            // Vérifier qu'il n'est pas déjà vendeur
            if (user.isVendeur()) {
                response.put("success", true);
                response.put("message", "Utilisateur déjà vendeur");
                response.put("uid", uid);
                return response;
            }

            // Mettre à jour dans Firestore
            try {
                Map<String, Object> updates = new HashMap<>();
                updates.put("role", "vendeur");
                updates.put("updatedAt", com.google.cloud.Timestamp.now());

                firestore.collection("users").document(uid).update(updates).get();
                System.out.println("✅ Rôle mis à jour dans Firestore");
            } catch (Exception e) {
                System.out.println("⚠️ Erreur Firestore (non critique): " + e.getMessage());
            }

            // Mettre à jour dans PostgreSQL
            user.setRole("vendeur");
            user.setVendeur(true);
            user.setClient(true); // Reste aussi client
            userRepository.save(user);

            // Créer l'entrée dans la table vendeurs
            Vendeur vendeur = new Vendeur();
            vendeur.setIdVendeur(uid);
            vendeur.setNom(user.getNom());
            vendeur.setRole("vendeur");

            // Récupérer l'email depuis Firestore si disponible
            try {
                DocumentSnapshot doc = firestore.collection("users").document(uid).get().get();
                if (doc.exists()) {
                    String email = doc.getString("email");
                    vendeur.setEmail(email);
                }
            } catch (Exception e) {
                System.out.println("⚠️ Impossible de récupérer l'email depuis Firestore");
            }

            vendeurRepository.save(vendeur);

            System.out.println("✅ Utilisateur upgradé vers vendeur avec succès");

            response.put("success", true);
            response.put("message", "Vous êtes maintenant vendeur !");
            response.put("uid", uid);

            return response;

        } catch (Exception e) {
            System.out.println("🚨 Erreur lors de l'upgrade vers vendeur: " + e.getMessage());
            e.printStackTrace();

            response.put("success", false);
            response.put("message", "Erreur lors de la mise à jour du rôle: " + e.getMessage());
            return response;
        }
    }

    /**
     * ✅ MODIFIÉ : Mise à jour createUserInPostgreSQL pour supprimer les références livreur
     */
    @Transactional
    protected User createUserInPostgreSQL(String uid, String displayName, String email, String role) {
        // Créer l'utilisateur principal
        User user = new User();
        user.setIdUser(uid);
        user.setNom(displayName);
        user.setRole(role != null ? role : "client");

        // Définir les rôles (plus de livreur)
        user.setClient(true);
        user.setVendeur("vendeur".equals(role));
        user.setLivreur(false); // Toujours false maintenant

        user = userRepository.save(user);

        // Créer dans la table vendeurs si nécessaire
        if ("vendeur".equals(role)) {
            Vendeur vendeur = new Vendeur();
            vendeur.setIdVendeur(uid);
            vendeur.setNom(displayName);
            vendeur.setRole("vendeur");
            vendeur.setEmail(email);
            vendeurRepository.save(vendeur);
        }
        // Suppression de la logique livreur

        return user;
    }
}