package com.boutique_catalogue_produits.interceptor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// Modifier VendeurIdInterceptor.java pour le rendre plus permissif en développement
@Component
public class VendeurIdInterceptor implements HandlerInterceptor {
    private static final Logger logger = LoggerFactory.getLogger(VendeurIdInterceptor.class);

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String idVendeur = request.getHeader("X-Vendeur-ID");
        logger.info("ID Vendeur reçu : {}", idVendeur);

        // TEMPORAIREMENT: Toujours autoriser les requêtes pour le débogage
        return true;

        // Le code ci-dessous est commenté pour le débogage
        /*
        // Optionnel : validation de base
        if (idVendeur == null || idVendeur.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }
        return true;
        */
    }
}