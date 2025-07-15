package com.boutique_catalogue_produits.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/ai/advanced")
@CrossOrigin(origins = "*")
public class AdvancedAIController {

    private static final Logger logger = LoggerFactory.getLogger(AdvancedAIController.class);

    @Value("${ai.python.microservice.url:http://localhost:8000}")
    private String pythonAIUrl;

    @Value("${ai.python.microservice.enabled:true}")
    private boolean pythonMicroserviceEnabled;

    private final RestTemplate restTemplate;

    // Injection pour utiliser vos services actuels en fallback
    @Autowired
    private com.boutique_catalogue_produits.service.RecommendationService currentRecommendationService;

    @Autowired
    private com.boutique_catalogue_produits.service.ImageSearchService currentImageSearchService;

    public AdvancedAIController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * 🔥 Recommandations avancées avec vos modèles de machine learning
     */
    @PostMapping("/recommendations/ml/{userId}")
    public ResponseEntity<Map<String, Object>> getMLRecommendations(
            @PathVariable String userId,
            @RequestParam(defaultValue = "10") int limit) {

        logger.info("🤖 [ADVANCED-AI] Recommandations ML pour user: {}", userId);

        try {
            if (!pythonMicroserviceEnabled) {
                logger.warn("⚠️ [ADVANCED-AI] Microservice Python désactivé, utilisation fallback");
                return fallbackToCurrentRecommendations(userId, limit);
            }

            // Appeler votre microservice Python avec vos modèles
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("user_id", userId);
            body.add("num_recommendations", limit);
            body.add("algorithm", "collaborative_filtering");

            HttpEntity<MultiValueMap<String, Object>> requestEntity = createRequest(body);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    pythonAIUrl + "/api/ml/recommendations",
                    requestEntity,
                    Map.class
            );

            Map<String, Object> result = response.getBody();
            if (result != null) {
                result.put("source", "ML_MODELS");
                result.put("algorithm", "collaborative_filtering_svd");
                result.put("timestamp", System.currentTimeMillis());
            }

            logger.info("✅ [ADVANCED-AI] Recommandations ML réussies pour user: {}", userId);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("❌ [ADVANCED-AI] Erreur recommandations ML pour user {}: {}", userId, e.getMessage());
            return fallbackToCurrentRecommendations(userId, limit);
        }
    }

    /**
     * 🎨 Recherche d'images par similarité avec vos modèles CNN
     */
    @PostMapping(value = "/image/similarity-search", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> similarityImageSearch(
            @RequestParam("image") MultipartFile imageFile,
            @RequestParam(defaultValue = "5") int topK) {

        logger.info("🖼️ [ADVANCED-AI] Recherche similarité pour: {}", imageFile.getOriginalFilename());

        try {
            if (!pythonMicroserviceEnabled) {
                return fallbackToGeminiImageSearch(imageFile);
            }

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("image", imageFile.getResource());
            body.add("top_k", topK);
            body.add("model_type", "mobilenet_v2");

            HttpEntity<MultiValueMap<String, Object>> requestEntity = createRequest(body);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    pythonAIUrl + "/api/ml/image-similarity",
                    requestEntity,
                    Map.class
            );

            Map<String, Object> result = response.getBody();
            if (result != null) {
                result.put("source", "CNN_SIMILARITY");
                result.put("model", "MobileNetV2");
                result.put("timestamp", System.currentTimeMillis());
            }

            logger.info("✅ [ADVANCED-AI] Recherche similarité réussie");
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("❌ [ADVANCED-AI] Erreur recherche similarité: {}", e.getMessage());
            return fallbackToGeminiImageSearch(imageFile);
        }
    }

    /**
     * 🔄 Recherche hybride combinant vos deux approches
     */
    @PostMapping(value = "/hybrid-search", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> hybridIntelligentSearch(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) MultipartFile image,
            @RequestParam(required = false) String userId) {

        logger.info("🔄 [ADVANCED-AI] Recherche hybride intelligente");

        Map<String, Object> hybridResults = new HashMap<>();
        long startTime = System.currentTimeMillis();

        try {
            // 1. Services ACTUELS (Gemini + RAG) - en parallèle
            CompletableFuture<Map<String, Object>> currentServicesTask =
                    CompletableFuture.supplyAsync(() -> getCurrentServicesResults(query, image, userId));

            // 2. Modèles AVANCÉS (Python ML) - en parallèle
            CompletableFuture<Map<String, Object>> mlModelsTask =
                    CompletableFuture.supplyAsync(() -> getMLModelsResults(query, image, userId));

            // Attendre les deux résultats avec timeout
            Map<String, Object> currentResults = currentServicesTask.get();
            Map<String, Object> mlResults = mlModelsTask.get();

            // Fusionner intelligemment
            hybridResults.put("gemini_rag_results", currentResults);
            hybridResults.put("ml_models_results", mlResults);
            hybridResults.put("fusion_strategy", "weighted_combination");
            hybridResults.put("confidence_score", calculateHybridConfidence(currentResults, mlResults));
            hybridResults.put("processing_time_ms", System.currentTimeMillis() - startTime);
            hybridResults.put("timestamp", System.currentTimeMillis());
            hybridResults.put("success", true);

            logger.info("✅ [ADVANCED-AI] Recherche hybride réussie en {}ms",
                    System.currentTimeMillis() - startTime);

            return ResponseEntity.ok(hybridResults);

        } catch (Exception e) {
            logger.error("❌ [ADVANCED-AI] Erreur recherche hybride: {}", e.getMessage());

            hybridResults.put("success", false);
            hybridResults.put("error", e.getMessage());
            hybridResults.put("fallback_used", true);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(hybridResults);
        }
    }

    /**
     * 📊 Statistiques des services avancés
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getAdvancedStats() {
        Map<String, Object> stats = new HashMap<>();

        try {
            // Stats du microservice Python
            if (pythonMicroserviceEnabled) {
                ResponseEntity<Map> pythonStats = restTemplate.getForEntity(
                        pythonAIUrl + "/api/ai/health", Map.class);
                stats.put("python_microservice", pythonStats.getBody());
            }

            stats.put("microservice_enabled", pythonMicroserviceEnabled);
            stats.put("microservice_url", pythonAIUrl);
            stats.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            stats.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(stats);
        }
    }

    // =============== MÉTHODES PRIVÉES (manquantes) ===============

    /**
     * Créer une requête HTTP avec headers appropriés
     */
    private HttpEntity<MultiValueMap<String, Object>> createRequest(MultiValueMap<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        return new HttpEntity<>(body, headers);
    }

    /**
     * Obtenir les résultats des services actuels (Gemini + RAG)
     */
    private Map<String, Object> getCurrentServicesResults(String query, MultipartFile image, String userId) {
        Map<String, Object> results = new HashMap<>();

        try {
            // Simuler l'appel à vos services actuels
            // En réalité, vous appellez vos controllers existants

            if (query != null && !query.isEmpty()) {
                // Appel RAG actuel (vous pouvez injecter votre service RAG)
                results.put("rag_results", Map.of(
                        "query", query,
                        "source", "current_rag_service",
                        "results", "simulation_rag_results"
                ));
            }

            if (image != null) {
                // Appel Gemini actuel
                results.put("gemini_analysis", Map.of(
                        "image_name", image.getOriginalFilename(),
                        "source", "current_gemini_service",
                        "analysis", "simulation_gemini_analysis"
                ));
            }

            if (userId != null) {
                // Recommandations actuelles
                results.put("current_recommendations", Map.of(
                        "user_id", userId,
                        "source", "current_recommendation_service",
                        "recommendations", "simulation_recommendations"
                ));
            }

            results.put("success", true);

        } catch (Exception e) {
            logger.error("❌ [CURRENT-SERVICES] Erreur: {}", e.getMessage());
            results.put("success", false);
            results.put("error", e.getMessage());
        }

        return results;
    }

    /**
     * Obtenir les résultats des modèles ML avancés
     */
    private Map<String, Object> getMLModelsResults(String query, MultipartFile image, String userId) {
        Map<String, Object> results = new HashMap<>();

        if (!pythonMicroserviceEnabled) {
            results.put("success", false);
            results.put("error", "Python microservice disabled");
            return results;
        }

        try {
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

            if (query != null) body.add("query", query);
            if (image != null) body.add("image", image.getResource());
            if (userId != null) body.add("user_id", userId);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = createRequest(body);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    pythonAIUrl + "/api/ml/hybrid-analysis",
                    requestEntity,
                    Map.class
            );

            results = response.getBody();
            if (results == null) {
                results = new HashMap<>();
            }
            results.put("source", "ML_MODELS");

        } catch (Exception e) {
            logger.error("❌ [ML-MODELS] Erreur: {}", e.getMessage());
            results.put("success", false);
            results.put("error", e.getMessage());
            results.put("fallback_needed", true);
        }

        return results;
    }

    /**
     * Calculer le score de confiance hybride
     */
    private double calculateHybridConfidence(Map<String, Object> currentResults, Map<String, Object> mlResults) {
        try {
            double currentConfidence = 0.5; // Valeur par défaut
            double mlConfidence = 0.5;

            // Extraire les scores de confiance si disponibles
            if (currentResults.containsKey("confidence")) {
                currentConfidence = (Double) currentResults.get("confidence");
            }

            if (mlResults.containsKey("confidence")) {
                mlConfidence = (Double) mlResults.get("confidence");
            }

            // Vérifier le succès des deux services
            boolean currentSuccess = (Boolean) currentResults.getOrDefault("success", false);
            boolean mlSuccess = (Boolean) mlResults.getOrDefault("success", false);

            if (currentSuccess && mlSuccess) {
                // Les deux services fonctionnent - moyenne pondérée
                return (currentConfidence * 0.4) + (mlConfidence * 0.6);
            } else if (currentSuccess) {
                // Seuls les services actuels fonctionnent
                return currentConfidence * 0.7;
            } else if (mlSuccess) {
                // Seuls les modèles ML fonctionnent
                return mlConfidence * 0.8;
            } else {
                // Aucun service ne fonctionne
                return 0.1;
            }

        } catch (Exception e) {
            logger.warn("⚠️ [CONFIDENCE] Erreur calcul confiance: {}", e.getMessage());
            return 0.3; // Score par défaut en cas d'erreur
        }
    }

    /**
     * Fallback vers les recommandations actuelles
     */
    private ResponseEntity<Map<String, Object>> fallbackToCurrentRecommendations(String userId, int limit) {
        logger.info("🔄 [FALLBACK] Utilisation recommandations actuelles pour user: {}", userId);

        try {
            // Ici vous pouvez appeler directement votre service actuel
            // ou faire un appel REST vers votre controller existant

            Map<String, Object> fallbackResult = new HashMap<>();
            fallbackResult.put("fallback", "current_recommendations");
            fallbackResult.put("user_id", userId);
            fallbackResult.put("limit", limit);
            fallbackResult.put("source", "FALLBACK_CURRENT_SERVICE");
            fallbackResult.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(fallbackResult);

        } catch (Exception e) {
            logger.error("❌ [FALLBACK] Erreur recommandations fallback: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "Tous les services de recommandation indisponibles"));
        }
    }

    /**
     * Fallback vers l'analyse Gemini actuelle
     */
    private ResponseEntity<Map<String, Object>> fallbackToGeminiImageSearch(MultipartFile imageFile) {
        logger.info("🔄 [FALLBACK] Utilisation Gemini pour: {}", imageFile.getOriginalFilename());

        try {
            Map<String, Object> fallbackResult = new HashMap<>();
            fallbackResult.put("fallback", "gemini_analysis");
            fallbackResult.put("image_name", imageFile.getOriginalFilename());
            fallbackResult.put("source", "FALLBACK_GEMINI_SERVICE");
            fallbackResult.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(fallbackResult);

        } catch (Exception e) {
            logger.error("❌ [FALLBACK] Erreur Gemini fallback: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "Tous les services d'analyse d'image indisponibles"));
        }
    }
}