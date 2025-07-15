package com.notificationsmessage.client;

import com.notificationsmessage.dto.VendeurDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class BoutiqueServiceClientFallback implements BoutiqueServiceClient {
    private static final Logger logger = LoggerFactory.getLogger(BoutiqueServiceClientFallback.class);

    @Override
    public VendeurDTO getVendeurById(String vendeurId) {
        logger.warn("FALLBACK: Impossible de récupérer le vendeur avec l'ID {}", vendeurId);
        // Retourner un vendeur par défaut en cas d'échec
        return new VendeurDTO(vendeurId, "vendeur", "Vendeur par défaut", "default@example.com");
    }

    @Override
    public Map<String, Object> getBoutiqueById(Long idBoutique) {
        logger.warn("FALLBACK: Impossible de récupérer la boutique avec l'ID {}", idBoutique);
        return new HashMap<>();
    }

    @Override
    public VendeurDTO getVendeurByBoutiqueId(Long idBoutique) {
        logger.warn("FALLBACK: Impossible de récupérer le vendeur pour la boutique {}", idBoutique);
        // Retourner un vendeur par défaut en cas d'échec
        return new VendeurDTO("default", "vendeur", "Vendeur par défaut", "default@example.com");
    }
}