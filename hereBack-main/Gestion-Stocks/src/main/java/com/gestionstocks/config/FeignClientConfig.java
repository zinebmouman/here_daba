package com.gestionstocks.config;

import feign.Logger;
import feign.Request;
import feign.codec.ErrorDecoder;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class FeignClientConfig {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(FeignClientConfig.class);

    /**
     * Configuration des délais d'attente pour Feign
     * Augmente les délais pour éviter les timeout prématurés
     */
    @Bean
    public Request.Options requestOptions() {
        return new Request.Options(
                10, TimeUnit.SECONDS, // Délai de connexion (connectTimeout)
                30, TimeUnit.SECONDS, // Délai de lecture (readTimeout)
                true // suivre les redirections
        );
    }

    /**
     * Niveau de journalisation détaillé pour Feign
     */
    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL; // Journalise les en-têtes, le corps des requêtes, les métadonnées
    }

    /**
     * Décodeur d'erreurs personnalisé pour mieux gérer les erreurs Feign
     */
    @Bean
    public ErrorDecoder errorDecoder() {
        return new BoutiqueServiceErrorDecoder();
    }

    /**
     * Décodeur d'erreurs spécifique pour le service Boutique
     */
    public static class BoutiqueServiceErrorDecoder implements ErrorDecoder {
        private final ErrorDecoder defaultErrorDecoder = new Default();

        @Override
        public Exception decode(String methodKey, feign.Response response) {
            logger.warn("Erreur Feign lors de l'appel à {} - Status: {}",
                    methodKey, response.status());

            // Journalisation détaillée pour aider au debugging
            if (response.status() >= 400 && response.status() <= 499) {
                logger.warn("Erreur client (4xx) lors de l'appel à {}", methodKey);

                if (response.status() == 404) {
                    // Pour les erreurs 404, créer une exception spécifique
                    if (methodKey.contains("getBoutiqueIdsByVendeurId")) {
                        logger.error("Aucune boutique trouvée pour ce vendeur");
                        return new com.gestionstocks.exception.ResourceNotFoundException(
                                "Aucune boutique trouvée pour ce vendeur");
                    }
                    else if (methodKey.contains("getVendeurIdByBoutiqueId")) {
                        logger.error("Boutique non trouvée ou vendeur non associé");
                        return new com.gestionstocks.exception.ResourceNotFoundException(
                                "Boutique non trouvée ou vendeur non associé");
                    }
                }
            }
            else if (response.status() >= 500) {
                logger.error("Erreur serveur (5xx) lors de l'appel à {}", methodKey);
            }

            return defaultErrorDecoder.decode(methodKey, response);
        }
    }
}