package com.boutique_catalogue_produits.service;

import com.boutique_catalogue_produits.config.FeignClientConfig;
import com.boutique_catalogue_produits.dto.VendeurDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Client Feign pour communiquer avec le microservice d'authentification
 * et récupérer les informations des vendeurs
 */


@FeignClient(
        name = "authentification-direct",
        url = "http://localhost:8081",
        fallback = AuthServiceClientFallback.class,
        configuration = FeignClientConfig.class
)
public interface AuthServiceClient {
    // Changez cette URL pour correspondre à un endpoint existant dans votre AuthController
    @GetMapping("/api/vendeurs/{vendeurId}")  // ou "/api/users/{vendeurId}" selon vos contrôleurs
    VendeurDTO getVendeurById(@PathVariable("vendeurId") String vendeurId);
}