package com.boutique_catalogue_produits.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class AISecurityConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(AISecurityConfiguration.class);

    @Value("${ai.security.rate-limit.enabled:true}")
    private boolean rateLimitEnabled;

    @Value("${ai.security.max-requests-per-minute:60}")
    private int maxRequestsPerMinute;

    @Value("${ai.security.max-image-size-mb:10}")
    private int maxImageSizeMb;

    /**
     * 🛡️ Configuration du limiteur de débit IA
     */
    @Bean
    @ConditionalOnProperty(name = "ai.security.rate-limit.enabled", havingValue = "true")
    public AIRateLimiter aiRateLimiter() {
        logger.info("🛡️ [AI-SECURITY] Configuration limiteur débit IA ({} req/min)", maxRequestsPerMinute);
        return new AIRateLimiter(maxRequestsPerMinute);
    }

    /**
     * 🔍 Validateur de fichiers IA
     */
    @Bean
    public AIFileValidator aiFileValidator() {
        logger.info("🔍 [AI-SECURITY] Configuration validateur fichiers IA (max: {}MB)", maxImageSizeMb);
        return new AIFileValidator(maxImageSizeMb);
    }

    /**
     * 🚦 Limiteur de débit pour les services IA
     */
    public static class AIRateLimiter {
        private final int maxRequestsPerMinute;
        private final Map<String, Queue<Long>> userRequests = new ConcurrentHashMap<>();

        public AIRateLimiter(int maxRequestsPerMinute) {
            this.maxRequestsPerMinute = maxRequestsPerMinute;
        }

        public boolean isAllowed(String userId) {
            long currentTime = System.currentTimeMillis();
            Queue<Long> requests = userRequests.computeIfAbsent(userId, k -> new LinkedList<>());

            // Nettoyer les anciennes requêtes (plus d'1 minute)
            requests.removeIf(time -> currentTime - time > 60000);

            if (requests.size() >= maxRequestsPerMinute) {
                return false;
            }

            requests.offer(currentTime);
            return true;
        }
    }

    /**
     * ✅ Validateur de fichiers pour l'IA
     */
    public static class AIFileValidator {
        private final long maxSizeBytes;
        private static final Set<String> ALLOWED_TYPES = Set.of(
                "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"
        );

        public AIFileValidator(int maxSizeMb) {
            this.maxSizeBytes = maxSizeMb * 1024L * 1024L;
        }

        public void validateImageFile(MultipartFile file) throws Exception {
            if (file == null || file.isEmpty()) {
                throw new Exception("Fichier requis");
            }

            if (file.getSize() > maxSizeBytes) {
                throw new Exception("Fichier trop volumineux (max: " + (maxSizeBytes / 1024 / 1024) + "MB)");
            }

            String contentType = file.getContentType();
            if (contentType == null || !ALLOWED_TYPES.contains(contentType.toLowerCase())) {
                throw new Exception("Type de fichier non supporté: " + contentType);
            }
        }
    }
}
