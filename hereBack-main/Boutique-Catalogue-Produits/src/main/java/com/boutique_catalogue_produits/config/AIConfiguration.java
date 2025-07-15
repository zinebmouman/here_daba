package com.boutique_catalogue_produits.config;

import com.boutique_catalogue_produits.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

/**
 * 🤖 Configuration des services d'Intelligence Artificielle
 */
@Configuration
@EnableAsync
@EnableScheduling
public class AIConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(AIConfiguration.class);

    @Value("${ai.enabled:true}")
    private boolean aiEnabled;

    @Value("${ai.thread-pool.core-size:5}")
    private int corePoolSize;

    @Value("${ai.thread-pool.max-size:20}")
    private int maxPoolSize;

    @Value("${ai.thread-pool.queue-capacity:100}")
    private int queueCapacity;

    /**
     * 🔧 Configuration du pool de threads pour l'IA
     */
    @Bean(name = "aiTaskExecutor")
    @ConditionalOnProperty(name = "ai.enabled", havingValue = "true", matchIfMissing = true)
    public Executor aiTaskExecutor() {
        logger.info("🔧 [AI-CONFIG] Configuration pool threads IA");

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("AI-");
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();

        logger.info("✅ [AI-CONFIG] Pool threads IA configuré (core: {}, max: {})",
                corePoolSize, maxPoolSize);

        return executor;
    }

    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();

        // Configuration timeout pour les appels vers le microservice Python
        HttpComponentsClientHttpRequestFactory factory =
                new HttpComponentsClientHttpRequestFactory();
        factory.setConnectTimeout(5000);  // 5 secondes
        factory.setReadTimeout(30000);    // 30 secondes

        restTemplate.setRequestFactory(factory);
        return restTemplate;
    }

    /**
     * 🌐 RestTemplate pour les appels API IA
     */
    @Bean(name = "aiRestTemplate")
    public RestTemplate aiRestTemplate() {
        logger.info("🌐 [AI-CONFIG] Configuration RestTemplate IA");

        RestTemplate restTemplate = new RestTemplate();

        // Configuration timeout personnalisée pour l'IA
        restTemplate.getInterceptors().add((request, body, execution) -> {
            request.getHeaders().add("User-Agent", "BoutiqueCatalogue-AI/1.0");
            return execution.execute(request, body);
        });

        return restTemplate;
    }

    /**
     * 🧠 Configuration du service d'embedding d'images
     */
    @Bean
    @ConditionalOnProperty(name = "ai.image.embedding.enabled", havingValue = "true", matchIfMissing = true)
    public ImageEmbeddingService imageEmbeddingService() {
        logger.info("🧠 [AI-CONFIG] Configuration service embedding images");
        return new ImageEmbeddingService();
    }

    /**
     * 🔍 Configuration du service de recherche par image
     */
    @Bean
    @ConditionalOnProperty(name = "ai.image.search.enabled", havingValue = "true", matchIfMissing = true)
    public ImageSearchService imageSearchService() {
        logger.info("🔍 [AI-CONFIG] Configuration service recherche image");
        return new ImageSearchService();
    }

    /**
     * 💡 Configuration du service de recommandations
     */
    @Bean
    @ConditionalOnProperty(name = "ai.recommendation.enabled", havingValue = "true", matchIfMissing = true)
    public RecommendationService recommendationService() {
        logger.info("💡 [AI-CONFIG] Configuration service recommandations");
        return new RecommendationService();
    }

    /**
     * 🚀 Configuration du service de recherche IA scalable
     */
    @Bean
    @ConditionalOnProperty(name = "ai.scalable.search.enabled", havingValue = "true", matchIfMissing = true)
    public ScalableAISearchService scalableAISearchService() {
        logger.info("🚀 [AI-CONFIG] Configuration service recherche IA scalable");
        return new ScalableAISearchService();
    }

    /**
     * 🎯 Configuration du service RAG (si pas déjà configuré)
     */
    @Bean
    @ConditionalOnProperty(name = "rag.enabled", havingValue = "true", matchIfMissing = false)
    public RAGService ragService() {
        logger.info("🎯 [AI-CONFIG] Configuration service RAG");
        return new RAGService();
    }

    /**
     * 📋 Bean de validation de la configuration IA
     */
    @Bean
    public AIConfigurationValidator aiConfigurationValidator() {
        return new AIConfigurationValidator();
    }

    /**
     * 🔍 Classe de validation de la configuration IA
     */
    public static class AIConfigurationValidator {

        private static final Logger logger = LoggerFactory.getLogger(AIConfigurationValidator.class);

        @Value("${gemini.api.key:}")
        private String geminiApiKey;

        @Value("${ai.image.model.path:}")
        private String modelPath;

        @Value("${minio.endpoint:}")
        private String minioEndpoint;

        public void validateConfiguration() {
            logger.info("🔍 [AI-CONFIG] Validation configuration IA");

            StringBuilder issues = new StringBuilder();

            // Vérifier la clé API Gemini
            if (geminiApiKey == null || geminiApiKey.trim().isEmpty()) {
                issues.append("- Clé API Gemini manquante (gemini.api.key)\n");
            }

            // Vérifier MinIO pour le stockage d'images
            if (minioEndpoint == null || minioEndpoint.trim().isEmpty()) {
                issues.append("- Configuration MinIO manquante (minio.endpoint)\n");
            }

            // Afficher les résultats
            if (issues.length() > 0) {
                logger.warn("⚠️ [AI-CONFIG] Problèmes de configuration détectés:\n{}", issues.toString());
            } else {
                logger.info("✅ [AI-CONFIG] Configuration IA validée avec succès");
            }
        }
    }
}

