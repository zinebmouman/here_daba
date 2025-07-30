package com.example.authentification.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Configuration
public class FirebaseConfig {

    @Value("${firebase.config.path}")
    private String firebaseConfigPath;

    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        List<FirebaseApp> apps = FirebaseApp.getApps();
        if (apps.isEmpty()) {
            // Essayer de charger depuis le système de fichiers (pour Docker)
            try (InputStream serviceAccount = new java.io.FileInputStream(firebaseConfigPath)) {
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .build();
                FirebaseApp app = FirebaseApp.initializeApp(options);
                System.out.println("✅ Firebase initialisé depuis le système de fichiers");
                return app;
            } catch (IOException e) {
                System.out.println("⚠ Tentative de chargement depuis classpath...");
                // Fallback: essayer depuis le classpath (pour le développement local)
                Resource resource = new ClassPathResource("firebase-service-account.json");
                try (InputStream fallbackStream = resource.getInputStream()) {
                    FirebaseOptions options = FirebaseOptions.builder()
                            .setCredentials(GoogleCredentials.fromStream(fallbackStream))
                            .build();
                    FirebaseApp app = FirebaseApp.initializeApp(options);
                    System.out.println("✅ Firebase initialisé depuis classpath");
                    return app;
                }
            }
        }
        System.out.println("✅ Firebase déjà initialisé");
        return apps.get(0);
    }

    @Bean
    @DependsOn("firebaseApp")
    public Firestore firestore() throws IOException {
        return FirestoreClient.getFirestore();
    }


    @Bean
    public FirebaseAuth firebaseAuth() throws IOException {
        return FirebaseAuth.getInstance(firebaseApp());
    }
}