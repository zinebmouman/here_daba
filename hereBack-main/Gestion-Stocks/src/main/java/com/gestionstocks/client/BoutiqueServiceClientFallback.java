package com.gestionstocks.client;

import com.gestionstocks.dto.BoutiqueDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class BoutiqueServiceClientFallback implements BoutiqueServiceClient {
    private static final Logger logger = LoggerFactory.getLogger(BoutiqueServiceClientFallback.class);

    @Override
    public String getVendeurIdByBoutiqueId(Long idBoutique) {
        logger.warn("FALLBACK: Impossible de récupérer le vendeur pour la boutique {}", idBoutique);
        return "default-vendeur-id";
    }

    @Override
    public List<Long> getBoutiqueIdsByVendeurId(String idVendeur) {
        logger.warn("FALLBACK: Impossible de récupérer les boutiques pour le vendeur {}", idVendeur);
        return Collections.emptyList();
    }

    @Override
    public List<BoutiqueDTO> getBoutiquesByVendeurId(String idVendeur) {
        logger.warn("FALLBACK: Impossible de récupérer les détails des boutiques pour le vendeur {}", idVendeur);
        return Collections.emptyList();
    }

    @Override
    public boolean verifierProprieteBoutique(String idVendeur, Long idBoutique) {
        logger.warn("FALLBACK: Impossible de vérifier la propriété de la boutique {} pour le vendeur {}", idBoutique, idVendeur);
        return false;
    }
}