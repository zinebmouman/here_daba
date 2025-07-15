package com.boutique_catalogue_produits.service;

import com.boutique_catalogue_produits.dto.VendeurDTO;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * Classe de repli pour le client AuthService.
 * Utilisée lorsque le microservice Auth n'est pas accessible.
 */
@Component
public class AuthServiceClientFallback implements AuthServiceClient {
    private static final Logger logger = LoggerFactory.getLogger(AuthServiceClientFallback.class);

    @Override
    public VendeurDTO getVendeurById(String vendeurId) {
        logger.error("Fallback: Impossible de récupérer le vendeur avec l'ID {}", vendeurId);

        // Utiliser le constructeur avec tous les paramètres
        return new VendeurDTO(
                vendeurId,
                "vendeur",
                "Vendeur Défaut",
                "default-vendeur@exemple.com"
        );
    }
}
