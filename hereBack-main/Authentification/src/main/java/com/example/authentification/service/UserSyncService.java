package com.example.authentification.service;
import com.example.authentification.model.Livreur;
import com.example.authentification.model.User;
import com.example.authentification.model.Vendeur;
import com.example.authentification.repository.LivreurRepository;
import com.example.authentification.repository.UserRepository;
import com.example.authentification.repository.VendeurRepository;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
// Autres imports existants...
@Service
public class UserSyncService {
    private static final Logger logger = LoggerFactory.getLogger(UserSyncService.class);
    private static final int BATCH_SIZE = 100; // Taille de lot configurable
    @Autowired
    private VendeurRepository vendeurRepository;
    @Autowired
    private LivreurRepository livreurRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private Firestore firestore;
    @Autowired
    private AuthService authService;
    @Transactional
    public Map<String, Object> syncNewUser(Map<String, String> userData) {
        Map<String, Object> syncReport = new HashMap<>();

        String uid = userData.get("uid");
        String email = userData.get("email");
        String displayName = userData.get("displayName");
        String role = userData.get("role");

        // Créer/mettre à jour dans PostgreSQL
        User user = userRepository.findByIdUser(uid)
                .orElse(new User());

        user.setIdUser(uid);
        user.setNom(displayName);
        user.setRole(role != null ? role : "client");

        // Réinitialiser les rôles
        user.setClient(false);
        user.setVendeur(false);
        user.setLivreur(false);

        // Définir les rôles
        user.setClient(true);
        if ("vendeur".equals(role)) {
            user.setVendeur(true);
            // Sauvegarder aussi dans la table vendeurs
            Vendeur vendeur = new Vendeur();
            vendeur.setIdVendeur(uid);
            vendeur.setNom(displayName);
            vendeur.setRole("vendeur");
            vendeurRepository.save(vendeur);
        }
        if ("livreur".equals(role)) {
            user.setLivreur(true);
            // Sauvegarder aussi dans la table livreurs
            Livreur livreur = new Livreur();
            livreur.setIdLivreur(uid);
            livreur.setNom(displayName);
            livreur.setRole("livreur");
            livreurRepository.save(livreur);
        }

        userRepository.save(user);

        syncReport.put("status", "success");
        syncReport.put("userId", user.getId());
        syncReport.put("role", user.getRole());

        return syncReport;
    }
    /**
     * Synchronise tous les vendeurs depuis Firestore vers PostgreSQL
     */
    @Transactional
    public Map<String, Object> syncVendeurs() throws Exception {
        long startTime = System.currentTimeMillis();
        Map<String, Object> syncReport = new HashMap<>();
        List<QueryDocumentSnapshot> vendeurDocs = getFirestoreUsersByRole("vendeur");
        syncReport.put("firestoreVendeurCount", vendeurDocs.size());
        List<Vendeur> vendeursToSync = processVendeurBatch(vendeurDocs);
        syncReport.put("syncedVendeurCount", vendeursToSync.size());
        syncReport.put("syncedVendeurIds", vendeursToSync.stream()
                .map(Vendeur::getIdVendeur)
                .collect(Collectors.toList()));
        long endTime = System.currentTimeMillis();
        logger.info("⏱️ Synchronisation des vendeurs terminée en {} ms", (endTime - startTime));
        return syncReport;
    }
    /**
     * Traitement par lots des vendeurs
     */
    private List<Vendeur> processVendeurBatch(List<QueryDocumentSnapshot> vendeurDocs) {
        List<Vendeur> vendeursToSync = new ArrayList<>();
        for (int i = 0; i < vendeurDocs.size(); i += BATCH_SIZE) {
            List<QueryDocumentSnapshot> batch = vendeurDocs.subList(
                    i,
                    Math.min(i + BATCH_SIZE, vendeurDocs.size())
            );
            List<Vendeur> batchVendeurs = batch.stream()
                    .map(doc -> {
                        String uid = doc.getId();
                        String displayName = doc.getString("displayName");
                        String email = doc.getString("email"); // Récupérer l'email

                        Vendeur vendeur = vendeurRepository.findByIdVendeur(uid)
                                .orElse(new Vendeur());
                        vendeur.setIdVendeur(uid);
                        vendeur.setRole("vendeur");
                        vendeur.setNom(displayName);
                        vendeur.setEmail(email); // Stocker l'email

                        return vendeur;
                    })
                    .collect(Collectors.toList());

            vendeursToSync.addAll(batchVendeurs);
            vendeurRepository.saveAll(batchVendeurs);
        }

        return vendeursToSync;
    }

    /**
     * Synchronise tous les livreurs depuis Firestore vers PostgreSQL
     */
    @Transactional
    public Map<String, Object> syncLivreurs() throws Exception {
        long startTime = System.currentTimeMillis();
        Map<String, Object> syncReport = new HashMap<>();

        List<QueryDocumentSnapshot> livreurDocs = getFirestoreUsersByRole("livreur");
        syncReport.put("firestoreLivreurCount", livreurDocs.size());

        List<Livreur> livreursToSync = processLivreurBatch(livreurDocs);

        syncReport.put("syncedLivreurCount", livreursToSync.size());
        syncReport.put("syncedLivreurIds", livreursToSync.stream()
                .map(Livreur::getIdLivreur)
                .collect(Collectors.toList()));

        long endTime = System.currentTimeMillis();
        logger.info("⏱️ Synchronisation des livreurs terminée en {} ms", (endTime - startTime));

        return syncReport;
    }


    /**
     * Traitement par lots des livreurs
     */
    private List<Livreur> processLivreurBatch(List<QueryDocumentSnapshot> livreurDocs) {
        List<Livreur> livreursToSync = new ArrayList<>();

        for (int i = 0; i < livreurDocs.size(); i += BATCH_SIZE) {
            List<QueryDocumentSnapshot> batch = livreurDocs.subList(
                    i,
                    Math.min(i + BATCH_SIZE, livreurDocs.size())
            );

            List<Livreur> batchLivreurs = batch.stream()
                    .map(doc -> {
                        String uid = doc.getId();
                        String displayName = doc.getString("displayName");

                        Livreur livreur = livreurRepository.findByIdLivreur(uid)
                                .orElse(new Livreur());

                        livreur.setIdLivreur(uid);
                        livreur.setRole("livreur");
                        livreur.setNom(displayName);

                        return livreur;
                    })
                    .collect(Collectors.toList());

            livreursToSync.addAll(batchLivreurs);
            livreurRepository.saveAll(batchLivreurs);
        }

        return livreursToSync;
    }
    @Transactional
    public void updateUserRole(String uid, String newRole) {
        try {
            logger.info("🔄 Début de la mise à jour du rôle pour l'utilisateur: {}", uid);

            // Récupération du document utilisateur dans Firestore
            DocumentReference docRef = firestore.collection("users").document(uid);

            // Utiliser get() avec timeout explicite
            ApiFuture<DocumentSnapshot> future = docRef.get();
            DocumentSnapshot document = future.get(10, TimeUnit.SECONDS);  // Timeout de 10 secondes

            if (!document.exists()) {
                logger.error("❌ Utilisateur non trouvé dans Firestore : {}", uid);
                throw new EntityNotFoundException("Utilisateur non trouvé");
            }

            String displayName = document.getString("displayName");
            String oldRole = document.getString("role");

            logger.info("📄 Données utilisateur récupérées: displayName={}, oldRole={}", displayName, oldRole);

            // Mise à jour du rôle dans Firestore avec gestion de timeout
            ApiFuture<WriteResult> updateFuture = docRef.update("role", newRole);
            updateFuture.get(10, TimeUnit.SECONDS);  // Timeout de 10 secondes

            // Récupération ou création dans PostgreSQL
            User user = userRepository.findByIdUser(uid)
                    .orElse(new User());

            // Définir ou mettre à jour les propriétés
            user.setIdUser(uid);
            user.setNom(displayName);

            // Réinitialiser les rôles
            user.setClient(false);
            user.setVendeur(false);
            user.setLivreur(false);

            // Définir le nouveau rôle
            user.setClient(true);
            user.setRole(newRole);

            // Gestion des rôles spécifiques
            if ("vendeur".equals(newRole)) {
                user.setVendeur(true);
                String email = document.getString("email");
                updateVendeurTable(uid, user.getNom(), email);
            } else if ("livreur".equals(newRole)) {
                user.setLivreur(true);
                updateLivreurTable(uid, user.getNom());
            }

            // Sauvegarder les modifications
            userRepository.save(user);

            logger.info("✅ Rôle mis à jour avec succès: {} - Ancien rôle: {}, Nouveau rôle: {}", uid, oldRole, newRole);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            logger.error("❌ Erreur lors de la communication avec Firestore", e);
            throw new RuntimeException("Erreur de communication avec Firestore", e);
        } catch (Exception e) {
            logger.error("❌ Erreur lors de la mise à jour du rôle", e);
            throw new RuntimeException("Impossible de mettre à jour le rôle", e);
        }
    }

    // Méthode modifiée pour inclure l'email
    private void updateVendeurTable(String uid, String nom, String email) {
        vendeurRepository.findByIdVendeur(uid)
                .ifPresentOrElse(
                        vendeur -> {
                            // Mettre à jour si existe déjà
                            vendeur.setNom(nom);
                            vendeur.setEmail(email);  // Ajout de l'email
                            vendeurRepository.save(vendeur);
                        },
                        () -> {
                            // Créer si n'existe pas
                            Vendeur vendeur = new Vendeur();
                            vendeur.setIdVendeur(uid);
                            vendeur.setNom(nom);
                            vendeur.setRole("vendeur");
                            vendeur.setEmail(email);  // Ajout de l'email
                            vendeurRepository.save(vendeur);
                        }
                );
    }
    private void updateLivreurTable(String uid, String nom) {
        livreurRepository.findByIdLivreur(uid)
                .ifPresentOrElse(
                        livreur -> {
                            // Mettre à jour si existe déjà
                            livreur.setNom(nom);
                            livreurRepository.save(livreur);
                        },
                        () -> {
                            // Créer si n'existe pas
                            Livreur livreur = new Livreur();
                            livreur.setIdLivreur(uid);
                            livreur.setNom(nom);
                            livreur.setRole("livreur");
                            livreurRepository.save(livreur);
                        }
                );
    }
    /**
     * Synchronise tous les utilisateurs (tous rôles confondus) depuis Firestore vers PostgreSQL
     */
    @Transactional
    public Map<String, Object> syncAllUsers() throws Exception {
        long startTime = System.currentTimeMillis();
        Map<String, Object> syncReport = new HashMap<>();

        List<QueryDocumentSnapshot> userDocs = getFirestoreAllUsers();
        syncReport.put("firestoreUserCount", userDocs.size());

        // Grouper les utilisateurs par rôle
        Map<String, List<DocumentSnapshot>> usersByRole = userDocs.stream()
                .collect(Collectors.groupingBy(
                        doc -> Optional.ofNullable(doc.getString("role")).orElse("client")
                ));

        // Statistiques par rôle
        Map<String, Integer> roleCounts = usersByRole.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().size()));
        syncReport.put("roleDistribution", roleCounts);

        // Synchronisation par lots
        List<User> usersToSync = processUserBatch(userDocs);

        syncReport.put("syncedUserCount", usersToSync.size());

        // logger des performances
        long endTime = System.currentTimeMillis();
        logger.info("⏱️ Synchronisation de tous les utilisateurs terminée en {} ms", (endTime - startTime));

        // Synchroniser les tables spécifiques
        syncVendeurs();
        syncLivreurs();

        return syncReport;
    }

    /**
     * Traitement par lots des utilisateurs
     */
    private List<User> processUserBatch(List<QueryDocumentSnapshot> userDocs) {
        List<User> usersToSync = new ArrayList<>();

        for (int i = 0; i < userDocs.size(); i += BATCH_SIZE) {
            List<QueryDocumentSnapshot> batch = userDocs.subList(
                    i,
                    Math.min(i + BATCH_SIZE, userDocs.size())
            );

            List<User> batchUsers = batch.stream()
                    .map(doc -> {
                        String uid = doc.getId();
                        String displayName = doc.getString("displayName");
                        String role = Optional.ofNullable(doc.getString("role")).orElse("client");

                        User user = userRepository.findByIdUser(uid)
                                .orElse(new User());

                        user.setIdUser(uid);
                        user.setNom(displayName);
                        user.setRole(role);
                        user.setClient(true);
                        user.setVendeur("vendeur".equals(role));
                        user.setLivreur("livreur".equals(role));

                        return user;
                    })
                    .collect(Collectors.toList());

            usersToSync.addAll(batchUsers);
            userRepository.saveAll(batchUsers);
        }

        return usersToSync;
    }

    /**
     * Récupère tous les utilisateurs d'un rôle spécifique depuis Firestore
     */
    private List<QueryDocumentSnapshot> getFirestoreUsersByRole(String role) throws Exception {
        ApiFuture<QuerySnapshot> future = firestore.collection("users")
                .whereEqualTo("role", role)
                .get();

        QuerySnapshot querySnapshot = future.get();
        return querySnapshot.getDocuments();
    }
    @Transactional
    public void handleRoleConflict(String uid, String oldRole, String newRole) {
        try {
            // Récupérer l'utilisateur principal
            User user = userRepository.findByIdUser(uid)
                    .orElseThrow(() -> new EntityNotFoundException("Utilisateur non trouvé"));
            // Mettre à jour le rôle principal
            user.setRole(newRole);
            // Réinitialiser tous les flags de rôle
            user.setClient(false);
            user.setVendeur(false);
            user.setLivreur(false);
            // Définir les nouveaux flags de rôle
            user.setClient(true); // Tous les utilisateurs sont des clients par défaut
            switch(newRole) {
                case "vendeur":
                    user.setVendeur(true);
                    // Supprimer de la table livreur si existant
                    Optional<Livreur> livreurExistant = livreurRepository.findByIdLivreur(uid);
                    if (livreurExistant.isPresent()) {
                        livreurRepository.delete(livreurExistant.get());
                    }
                    // Ajouter/mettre à jour dans la table vendeur
                    Vendeur vendeur = vendeurRepository.findByIdVendeur(uid)
                            .orElse(new Vendeur());
                    vendeur.setIdVendeur(uid);
                    vendeur.setNom(user.getNom());
                    vendeur.setRole("vendeur");
                    vendeurRepository.save(vendeur);
                    break;
                case "livreur":
                    user.setLivreur(true);
                    // Supprimer de la table vendeur si existant
                    Optional<Vendeur> vendeurExistant = vendeurRepository.findByIdVendeur(uid);
                    if (vendeurExistant.isPresent()) {
                        vendeurRepository.delete(vendeurExistant.get());
                    }
                    // Ajouter/mettre à jour dans la table livreur
                    Livreur livreur = livreurRepository.findByIdLivreur(uid)
                            .orElse(new Livreur());
                    livreur.setIdLivreur(uid);
                    livreur.setNom(user.getNom());
                    livreur.setRole("livreur");
                    livreurRepository.save(livreur);
                    break;
                default:
                    // Pour les autres rôles ou "client"
                    Optional<Vendeur> existingVendeur = vendeurRepository.findByIdVendeur(uid);
                    if (existingVendeur.isPresent()) {
                        vendeurRepository.delete(existingVendeur.get());
                    }
                    Optional<Livreur> existingLivreur = livreurRepository.findByIdLivreur(uid);
                    if (existingLivreur.isPresent()) {
                        livreurRepository.delete(existingLivreur.get());
                    }
            }
            // Sauvegarder les modifications de l'utilisateur principal
            userRepository.save(user);
            // Journalisation des changements
            System.out.println("Rôle mis à jour : " + uid + " - Ancien rôle : " + oldRole + ", Nouveau rôle : " + newRole);
        } catch (Exception e) {
            // Gestion des erreurs
            System.err.println("Erreur lors du changement de rôle : " + e.getMessage());
            throw new RuntimeException("Impossible de mettre à jour le rôle", e);
        }
    }
    /**
     * Récupère tous les utilisateurs depuis Firestore
     */
    private List<QueryDocumentSnapshot> getFirestoreAllUsers() throws Exception {
        ApiFuture<QuerySnapshot> future = firestore.collection("users").get();
        QuerySnapshot querySnapshot = future.get();
        return querySnapshot.getDocuments();
    }
    public boolean checkUserExists(String uid) {
        // Vérifier dans la table principale des utilisateurs
        boolean existsInUserTable = userRepository.findByIdUser(uid).isPresent();
        // Si le rôle principal n'est pas trouvé, vérifier dans les tables spécifiques
        if (!existsInUserTable) {
            boolean existsInVendeurTable = vendeurRepository.findByIdVendeur(uid).isPresent();
            boolean existsInLivreurTable = livreurRepository.findByIdLivreur(uid).isPresent();
            return existsInVendeurTable || existsInLivreurTable;
        }
        return true;
    }
}