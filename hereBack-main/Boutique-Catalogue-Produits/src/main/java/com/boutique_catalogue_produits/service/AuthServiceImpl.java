package com.boutique_catalogue_produits.service;

import com.boutique_catalogue_produits.dto.VendeurDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl {
    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final AuthServiceClient authServiceClient;

    public AuthServiceImpl(AuthServiceClient authServiceClient) {
        this.authServiceClient = authServiceClient;
    }

    /**
     * Récupère les informations d'un vendeur
     * Utilise une logique de secours si le service distant échoue
     */
    public VendeurDTO getVendeurInfo(String vendeurId) {
        try {
            logger.info("Tentative de récupération du vendeur avec l'ID: {}", vendeurId);
            // Essayer d'abord le service distant
            return authServiceClient.getVendeurById(vendeurId);
        } catch (Exception e) {
            logger.warn("Échec de récupération du vendeur depuis le service distant: {}", e.getMessage());
            // En cas d'échec, créer un vendeur par défaut
            // Note: Ceci est une solution temporaire pour tester
            return new VendeurDTO(
                    vendeurId,
                    "vendeur",
                    "Vendeur Défaut",
                    "default-vendeur@exemple.com"
            );
        }
    }

    /**
     * Vérifie si un ID token est valide
     * Cette méthode pourrait être développée pour intégrer une vérification Firebase
     */
    public boolean isValidToken(String idToken, String idVendeur) {
        // Dans un environnement de production, vous devriez vérifier le token
        // Pour ce test, nous supposons que tout token non vide est valide
        if (idToken == null || idToken.isEmpty() || !idToken.startsWith("Bearer ")) {
            logger.warn("Token invalide pour le vendeur: {}", idVendeur);
            return false;
        }

        // Implémentation réelle: vérifier le token avec Firebase Admin SDK
        // FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken.substring(7));
        // return decodedToken != null && decodedToken.getUid().equals(idVendeur);

        logger.info("Token considéré comme valide pour le vendeur: {}", idVendeur);
        return true;
    }
}