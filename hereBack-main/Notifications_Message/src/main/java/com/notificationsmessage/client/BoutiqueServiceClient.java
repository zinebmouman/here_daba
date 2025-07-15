package com.notificationsmessage.client;

import com.notificationsmessage.dto.VendeurDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@FeignClient(name = "BOUTIQUE-CATALOGUE-PRODUITS", fallback = BoutiqueServiceClientFallback.class)
public interface BoutiqueServiceClient {
    // Nouvelle méthode pour récupérer le vendeur par son ID
    @GetMapping("/api/vendeurs/{vendeurId}")
    VendeurDTO getVendeurById(@PathVariable("vendeurId") String vendeurId);


    // Nouvelle méthode pour récupérer les informations de la boutique
    @GetMapping("/api/boutiques/{idBoutique}")
    Map<String, Object> getBoutiqueById(@PathVariable("idBoutique") Long idBoutique);

    // Nouvelle méthode pour récupérer l'ID du vendeur associé à une boutique
    @GetMapping("/api/boutiques/{idBoutique}/vendeur")
    VendeurDTO getVendeurByBoutiqueId(@PathVariable("idBoutique") Long idBoutique);
}