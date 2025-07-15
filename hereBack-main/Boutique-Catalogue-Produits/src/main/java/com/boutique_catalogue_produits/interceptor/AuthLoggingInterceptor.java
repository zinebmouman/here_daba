package com.boutique_catalogue_produits.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

/**
 * Intercepteur pour la journalisation et le diagnostic des problèmes d'authentification
 */
@Component
public class AuthLoggingInterceptor implements HandlerInterceptor {
    private static final Logger logger = LoggerFactory.getLogger(AuthLoggingInterceptor.class);

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // Journaliser les informations de la requête
        String uri = request.getRequestURI();
        String method = request.getMethod();
        String authHeader = request.getHeader("Authorization");
        String vendeurId = request.getHeader("X-Vendeur-ID");

        logger.info("Requête reçue: {} {}", method, uri);
        logger.info("En-têtes d'authentification - Authorization: {}, X-Vendeur-ID: {}",
                (authHeader != null) ? authHeader.substring(0, Math.min(authHeader.length(), 20)) + "..." : "null",
                vendeurId);

        // En mode développement, autoriser les requêtes même sans authentification
        // IMPORTANT: Cette ligne est UNIQUEMENT pour le débogage et doit être retirée en production
        if (isDevelopmentMode() && (authHeader == null || vendeurId == null)) {
            logger.warn("⚠️ Mode développement: Autorisation accordée malgré des informations d'authentification manquantes");
            return true;
        }

        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) {
        // Journaliser le statut de la réponse
        String uri = request.getRequestURI();
        int status = response.getStatus();

        if (status >= 400) {
            logger.warn("⚠️ Réponse d'erreur pour {}: Statut {}", uri, status);
        } else {
            logger.info("✅ Réponse réussie pour {}: Statut {}", uri, status);
        }
    }

    /**
     * Détermine si l'application fonctionne en mode développement
     */
    private boolean isDevelopmentMode() {
        // Pour le développement, activez cette option
        return true;

        // En production, utilisez plutôt:
        // String springProfile = System.getProperty("spring.profiles.active");
        // return "dev".equals(springProfile) || "development".equals(springProfile);
    }
}