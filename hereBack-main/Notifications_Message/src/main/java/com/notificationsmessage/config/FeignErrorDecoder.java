package com.notificationsmessage.config;

import feign.Response;
import feign.codec.ErrorDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class FeignErrorDecoder implements ErrorDecoder {
    private static final Logger logger = LoggerFactory.getLogger(FeignErrorDecoder.class);
    private final ErrorDecoder defaultErrorDecoder = new Default();

    @Override
    public Exception decode(String methodKey, Response response) {
        logger.error("Status code: {}, methodKey: {}", response.status(), methodKey);

        if (response.status() >= 500) {
            logger.error("Erreur serveur lors de l'appel à Feign: {} - {}", methodKey, response.reason());
            // Utiliser une exception personnalisée que votre @Retryable peut capturer
            return new ServerErrorException("Erreur serveur lors de l'appel à: " + methodKey);
        }

        return defaultErrorDecoder.decode(methodKey, response);
    }

    // Exception personnalisée pour les erreurs serveur
    public static class ServerErrorException extends RuntimeException {
        public ServerErrorException(String message) {
            super(message);
        }
    }
}