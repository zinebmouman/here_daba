package com.gestionstocks.client;

import com.gestionstocks.dto.BoutiqueDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.List;
import java.util.Optional;

@FeignClient(name = "BOUTIQUE-CATALOGUE-PRODUITS", fallback = BoutiqueServiceClientFallback.class)
public interface BoutiqueServiceClient {

    /**
     * Récupère l'ID du vendeur pour une boutique
     * @param idBoutique L'ID de la boutique (Long)
     * @return L'ID du vendeur
     */
    @GetMapping("/api/boutiques/{idBoutique}/vendeur")
    @CircuitBreaker(name = "boutiqueService", fallbackMethod = "getVendeurIdByBoutiqueIdFallback")
    String getVendeurIdByBoutiqueId(@PathVariable("idBoutique") Long idBoutique);

    default String getVendeurIdByBoutiqueIdFallback(Long idBoutique, Exception e) {
        return "default-vendeur-id";
    }

    /**
     * Récupère les IDs des boutiques d'un vendeur
     * @param idVendeur L'ID du vendeur
     * @return Liste des IDs de boutiques
     */
    @GetMapping("/api/boutiques/vendeurs/{idVendeur}/boutiques-ids")
    @CircuitBreaker(name = "boutiqueService", fallbackMethod = "getBoutiqueIdsByVendeurIdFallback")
    List<Long> getBoutiqueIdsByVendeurId(@PathVariable("idVendeur") String idVendeur);

    default List<Long> getBoutiqueIdsByVendeurIdFallback(String idVendeur, Exception e) {
        return java.util.Collections.emptyList();
    }

    /**
     * Récupère les boutiques d'un vendeur
     * @param idVendeur L'ID du vendeur
     * @return Liste des boutiques
     */
    @GetMapping("/api/boutiques/vendeur/{idVendeur}")
    @CircuitBreaker(name = "boutiqueService", fallbackMethod = "getBoutiquesByVendeurIdFallback")
    List<BoutiqueDTO> getBoutiquesByVendeurId(@PathVariable("idVendeur") String idVendeur);

    default List<BoutiqueDTO> getBoutiquesByVendeurIdFallback(String idVendeur, Exception e) {
        return java.util.Collections.emptyList();
    }

    /**
     * Vérifie si un vendeur est propriétaire d'une boutique
     * @param idVendeur L'ID du vendeur
     * @param idBoutique L'ID de la boutique
     * @return true si le vendeur est propriétaire, false sinon
     */
    @GetMapping("/api/vendeurs/verifier-propriete")
    @CircuitBreaker(name = "boutiqueService", fallbackMethod = "verifierProprieteBoutiqueFallback")
    boolean verifierProprieteBoutique(
            @RequestParam("idVendeur") String idVendeur,
            @RequestParam("idBoutique") Long idBoutique
    );

    default boolean verifierProprieteBoutiqueFallback(String idVendeur, Long idBoutique, Exception e) {
        return false;
    }
}