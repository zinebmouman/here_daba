package com.example.authentification.controller;

import com.example.authentification.model.Livreur;
import com.example.authentification.model.User;
import com.example.authentification.model.Vendeur;
import com.example.authentification.repository.LivreurRepository;
import com.example.authentification.repository.UserRepository;
import com.example.authentification.repository.VendeurRepository;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
@RestController
@RequestMapping("/api/users/check")
public class UserCheckController {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private VendeurRepository vendeurRepository;
    @Autowired
    private LivreurRepository livreurRepository;
    @Autowired
    private Firestore firestore;
    /**
     * Récupère tous les utilisateurs stockés dans PostgreSQL
     */
    @GetMapping("/postgresql")
    public ResponseEntity<Map<String, Object>> checkUsersInPostgreSQL() {
        // Récupérer tous les utilisateurs de la base de données
        List<User> users = userRepository.findAll();
        // Compter les utilisateurs par rôle
        long clientCount = users.stream().filter(User::isClient).count();
        long vendeurCount = users.stream().filter(User::isVendeur).count();
        long livreurCount = users.stream().filter(User::isLivreur).count();
        // Préparer la réponse
        Map<String, Object> response = new HashMap<>();
        response.put("total_users", users.size());
        response.put("client_count", clientCount);
        response.put("vendeur_count", vendeurCount);
        response.put("livreur_count", livreurCount);
        // Lister les utilisateurs par rôle principal
        Map<String, List<Map<String, Object>>> usersByRole = users.stream()
                .collect(Collectors.groupingBy(
                        User::getRole,
                        Collectors.mapping(
                                user -> {
                                    Map<String, Object> userInfo = new HashMap<>();
                                    userInfo.put("id", user.getId());
                                    userInfo.put("firebase_uid", user.getIdUser());
                                    userInfo.put("nom", user.getNom());
                                    userInfo.put("isClient", user.isClient());
                                    userInfo.put("isVendeur", user.isVendeur());
                                    userInfo.put("isLivreur", user.isLivreur());
                                    return userInfo;
                                },
                                Collectors.toList()
                        )
                ));
        response.put("users_by_role", usersByRole);
        return ResponseEntity.ok(response);
    }
    /**
     * Récupère tous les utilisateurs stockés dans Firestore
     */
    @GetMapping("/firestore")
    public ResponseEntity<Map<String, Object>> checkUsersInFirestore() throws Exception {
        // Récupérer tous les documents utilisateurs dans Firestore
        List<QueryDocumentSnapshot> documents = firestore.collection("users")
                .get()
                .get()
                .getDocuments();
        // Compter les utilisateurs par rôle
        Map<String, Long> roleCount = documents.stream()
                .collect(Collectors.groupingBy(
                        doc -> {
                            String role = doc.getString("role");
                            return role != null ? role : "client"; // Par défaut, client
                        },
                        Collectors.counting()
                ));
        // Préparer la réponse
        Map<String, Object> response = new HashMap<>();
        response.put("total_users", documents.size());
        response.put("role_distribution", roleCount);
        // Informations détaillées sur les utilisateurs
        response.put("user_details", documents.stream()
                .map(doc -> {
                    Map<String, Object> userInfo = new HashMap<>();
                    userInfo.put("id", doc.getId());
                    userInfo.put("email", doc.getString("email"));
                    userInfo.put("displayName", doc.getString("displayName"));
                    userInfo.put("role", doc.getString("role"));
                    return userInfo;
                })
                .collect(Collectors.toList()));

        return ResponseEntity.ok(response);
    }
    /**
     * Vérifier les vendeurs
     */
    @GetMapping("/vendeurs")
    public ResponseEntity<Map<String, Object>> checkVendeurs() {
        // Récupérer tous les vendeurs de la base de données
        List<Vendeur> vendeurs = vendeurRepository.findAll();

        // Filtrer uniquement les vendeurs
        List<Vendeur> vendeursOnly = vendeurs.stream()
                .filter(v -> "vendeur".equals(v.getRole()))
                .collect(Collectors.toList());
        // Préparer la réponse
        Map<String, Object> response = new HashMap<>();
        response.put("total_vendeurs", vendeurs.size());
        response.put("vendeur_count", vendeursOnly.size());
        response.put("vendeur_details", vendeursOnly.stream()
                .map(v -> {
                    Map<String, Object> vendeurInfo = new HashMap<>();
                    vendeurInfo.put("id", v.getId());
                    vendeurInfo.put("firebase_uid", v.getIdVendeur());
                    vendeurInfo.put("nom", v.getNom());
                    vendeurInfo.put("role", v.getRole());
                    return vendeurInfo;
                })
                .collect(Collectors.toList()));
        return ResponseEntity.ok(response);
    }
    /**
     * Vérifier les livreurs
     */
    @GetMapping("/livreurs")
    public ResponseEntity<Map<String, Object>> checkLivreurs() {
        // Récupérer tous les livreurs de la base de données
        List<Livreur> livreurs = livreurRepository.findAll();

        // Filtrer uniquement les livreurs
        List<Livreur> livreursOnly = livreurs.stream()
                .filter(l -> "livreur".equals(l.getRole()))
                .collect(Collectors.toList());
        // Préparer la réponse
        Map<String, Object> response = new HashMap<>();
        response.put("total_livreurs", livreurs.size());
        response.put("livreur_count", livreursOnly.size());
        response.put("livreur_details", livreursOnly.stream()
                .map(l -> {
                    Map<String, Object> livreurInfo = new HashMap<>();
                    livreurInfo.put("id", l.getId());
                    livreurInfo.put("firebase_uid", l.getIdLivreur());
                    livreurInfo.put("nom", l.getNom());
                    livreurInfo.put("role", l.getRole());
                    return livreurInfo;
                })
                .collect(Collectors.toList()));

        return ResponseEntity.ok(response);
    }

    /**
     * Statistiques sur les utilisateurs multi-rôles
     */
    @GetMapping("/multi-role")
    public ResponseEntity<Map<String, Object>> getMultiRoleStatistics() {
        List<User> users = userRepository.findAll();

        // Compter les utilisateurs avec plusieurs rôles
        long usersWithMultipleRoles = users.stream()
                .filter(user ->
                        (user.isClient() ? 1 : 0) +
                                (user.isVendeur() ? 1 : 0) +
                                (user.isLivreur() ? 1 : 0) > 1
                )
                .count();

        // Identifier les combinaisons de rôles
        Map<String, Long> roleCombinations = new HashMap<>();

        for (User user : users) {
            StringBuilder combo = new StringBuilder();
            if (user.isClient()) combo.append("Client");
            if (user.isVendeur()) {
                if (combo.length() > 0) combo.append("+");
                combo.append("Vendeur");
            }
            if (user.isLivreur()) {
                if (combo.length() > 0) combo.append("+");
                combo.append("Livreur");
            }

            String comboStr = combo.toString();
            roleCombinations.put(comboStr, roleCombinations.getOrDefault(comboStr, 0L) + 1);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("total_users", users.size());
        response.put("users_with_multiple_roles", usersWithMultipleRoles);
        response.put("role_combinations", roleCombinations);

        return ResponseEntity.ok(response);
    }
}