package com.example.authentification.service;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;

@Service
public class FirestoreService {

    @Autowired
    private Firestore firestore;

    /**
     * Récupère un document Firestore de manière sécurisée
     */
    public DocumentSnapshot getDocumentSafely(String collection, String document) {
        try {
            DocumentReference docRef = firestore.collection(collection).document(document);
            ApiFuture<DocumentSnapshot> future = docRef.get();
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            System.out.println("🚨 Erreur lors de la récupération du document Firestore: " + e.getMessage());
            return null;
        }
    }

    /**
     * Vérifie si un utilisateur a un rôle spécifique
     */
    public boolean hasRole(String uid, String roleToCheck) {
        DocumentSnapshot document = getDocumentSafely("users", uid);
        if (document != null && document.exists()) {
            String role = document.getString("role");
            return roleToCheck.equals(role);
        }
        return false;
    }
}