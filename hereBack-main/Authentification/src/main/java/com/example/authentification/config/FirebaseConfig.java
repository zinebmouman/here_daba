package com.example.authentification.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.DependsOn;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Configuration
public class FirebaseConfig {

    @Bean
    @Primary
    public FirebaseApp firebaseApp() throws IOException {
        // Vérifier si FirebaseApp est déjà initialisé
        List<FirebaseApp> apps = FirebaseApp.getApps();
        if (apps.isEmpty()) {
            // Charger le fichier de configuration Firebase
            InputStream serviceAccount = getClass().getClassLoader().getResourceAsStream("firebase-service-account.json");
            if (serviceAccount == null) {
                throw new IOException("Firebase service account file not found");
            }
            // Construire les options Firebase
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();
            // Initialiser FirebaseApp
            FirebaseApp app = FirebaseApp.initializeApp(options);
            System.out.println("✅ Firebase initialisé avec succès !");
            return app;
        }
        System.out.println("✅ Firebase déjà initialisé.");
        return apps.get(0);
    }

    @Bean
    @DependsOn("firebaseApp")
    @Primary
    public Firestore firestore() throws IOException {
        return FirestoreClient.getFirestore();
    }

    @Bean
    @DependsOn("firebaseApp")
    public FirebaseAuth firebaseAuth() throws IOException {
        // S'assurer que FirebaseApp est initialisé
        FirebaseApp app = firebaseApp();
        // Créer une nouvelle instance de FirebaseAuth
        FirebaseAuth auth = FirebaseAuth.getInstance(app);
        System.out.println("✅ FirebaseAuth initialisé avec succès !");
        return auth;
    }
}