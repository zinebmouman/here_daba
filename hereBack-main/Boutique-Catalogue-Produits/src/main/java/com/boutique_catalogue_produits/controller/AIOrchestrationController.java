package com.boutique_catalogue_produits.controller;

import com.boutique_catalogue_produits.dto.*;
import com.boutique_catalogue_produits.service.AIOrchestrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 🎯 Contrôleur d'orchestration IA - Point d'entrée principal pour toutes les fonctionnalités IA
 */
@RestController
@RequestMapping("/api/ai-orchestration")
@CrossOrigin(origins = "*")
public class AIOrchestrationController {

    private static final Logger logger = LoggerFactory.getLogger(AIOrchestrationController.class);

    @Autowired
    private AIOrchestrationService aiOrchestrationService;

    /**
     * 🌍 ENDPOINT PRINCIPAL - Recherche universelle avec toutes les IA
     */
    @PostMapping(value = "/universal-search", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CompletableFuture<ResponseEntity<UniversalSearchResponse>> universalSearch(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) MultipartFile image,
            @RequestParam(required = false) String userId,
            @RequestParam(defaultValue = "hybrid") String searchMode,
            @RequestParam(defaultValue = "MIXED") String searchType,
            @RequestParam(defaultValue = "50") int maxResults) {

        logger.info("🌍 [AI-ORCHESTRATION] Recherche universelle: query='{}', user='{}', mode='{}'",
                query, userId, searchMode);

        // Construire la requête
        UniversalSearchRequest request = new UniversalSearchRequest();
        request.setQuery(query);
        request.setImageFile(image);
        request.setUserId(userId);
        request.setSearchMode(searchMode);
        request.setSearchType(searchType);
        request.setMaxResults(maxResults);

        // Ajouter le contexte
        Map<String, Object> context = new HashMap<>();
        context.put("requestTime", System.currentTimeMillis());
        context.put("userAgent", "web-client");
        request.setContext(context);

        // Exécuter la recherche universelle
        return aiOrchestrationService.universalSearch(request)
                .thenApply(response -> {
                    if (response.isSuccess()) {
                        logger.info("✅ [AI-ORCHESTRATION] Recherche universelle réussie en {}ms",
                                response.getProcessingTimeMs());
                        return ResponseEntity.ok(response);
                    } else {
                        logger.error("❌ [AI-ORCHESTRATION] Échec recherche universelle: {}",
                                response.getErrorMessage());
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
                    }
                })
                .exceptionally(throwable -> {
                    logger.error("❌ [AI-ORCHESTRATION] Exception recherche universelle: {}",
                            throwable.getMessage());
                    UniversalSearchResponse errorResponse = new UniversalSearchResponse();
                    errorResponse.setSuccess(false);
                    errorResponse.setErrorMessage(throwable.getMessage());
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
                });
    }

    /**
     * 🖼️ Analyse complète d'image avec toutes les IA
     */
    @PostMapping(value = "/complete-image-analysis", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CompletableFuture<ResponseEntity<CompleteImageAnalysisResponse>> completeImageAnalysis(
            @RequestParam("image") MultipartFile imageFile,
            @RequestParam(required = false) String userId) {

        logger.info("🖼️ [AI-ORCHESTRATION] Analyse complète image: '{}' (user: {})",
                imageFile.getOriginalFilename(), userId);

        return aiOrchestrationService.completeImageAnalysis(imageFile, userId)
                .thenApply(response -> {
                    if (response.isSuccess()) {
                        logger.info("✅ [AI-ORCHESTRATION] Analyse image réussie en {}ms",
                                response.getProcessingTimeMs());
                        return ResponseEntity.ok(response);
                    } else {
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
                    }
                })
                .exceptionally(throwable -> {
                    logger.error("❌ [AI-ORCHESTRATION] Exception analyse image: {}", throwable.getMessage());
                    CompleteImageAnalysisResponse errorResponse = new CompleteImageAnalysisResponse();
                    errorResponse.setSuccess(false);
                    errorResponse.setErrorMessage(throwable.getMessage());
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
                });
    }

    /**
     * 💡 Recommandations intelligentes multi-contextes
     */
    @PostMapping("/intelligent-recommendations")
    public CompletableFuture<ResponseEntity<IntelligentRecommendationResponse>> intelligentRecommendations(
            @RequestParam String userId,
            @RequestBody(required = false) Map<String, Object> context) {

        logger.info("💡 [AI-ORCHESTRATION] Recommandations intelligentes pour: {}", userId);

        if (context == null) {
            context = new HashMap<>();
        }

        return aiOrchestrationService.intelligentRecommendations(userId, context)
                .thenApply(response -> {
                    if (response.isSuccess()) {
                        logger.info("✅ [AI-ORCHESTRATION] Recommandations générées en {}ms",
                                response.getProcessingTimeMs());
                        return ResponseEntity.ok(response);
                    } else {
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
                    }
                })
                .exceptionally(throwable -> {
                    logger.error("❌ [AI-ORCHESTRATION] Exception recommandations: {}", throwable.getMessage());
                    IntelligentRecommendationResponse errorResponse = new IntelligentRecommendationResponse();
                    errorResponse.setSuccess(false);
                    errorResponse.setErrorMessage(throwable.getMessage());
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
                });
    }

    /**
     * 🔗 Recherche hybride - Combine texte, image et recommandations
     */
    @PostMapping(value = "/hybrid-search", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CompletableFuture<ResponseEntity<HybridSearchResponse>> hybridSearch(
            @RequestParam(required = false) String textQuery,
            @RequestParam(required = false) MultipartFile image,
            @RequestParam(required = false) String userId,
            @RequestParam(defaultValue = "MIXED") String searchType,
            @RequestParam(defaultValue = "30") int maxResults) {

        logger.info("🔗 [AI-ORCHESTRATION] Recherche hybride: text='{}', hasImage={}, user='{}'",
                textQuery, image != null, userId);

        // Validation
        if ((textQuery == null || textQuery.trim().isEmpty()) && image == null) {
            HybridSearchResponse errorResponse = new HybridSearchResponse();
            errorResponse.setSuccess(false);
            errorResponse.setErrorMessage("Au moins un texte ou une image est requis");
            return CompletableFuture.completedFuture(
                    ResponseEntity.badRequest().body(errorResponse));
        }

        // Construire la requête
        HybridSearchRequest request = new HybridSearchRequest();
        request.setTextQuery(textQuery);
        request.setImageFile(image);
        request.setUserId(userId);
        request.setSearchType(searchType);
        request.setMaxResults(maxResults);

        return aiOrchestrationService.hybridSearch(request)
                .thenApply(response -> {
                    if (response.isSuccess()) {
                        logger.info("✅ [AI-ORCHESTRATION] Recherche hybride réussie en {}ms",
                                response.getProcessingTimeMs());
                        return ResponseEntity.ok(response);
                    } else {
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
                    }
                })
                .exceptionally(throwable -> {
                    logger.error("❌ [AI-ORCHESTRATION] Exception recherche hybride: {}", throwable.getMessage());
                    HybridSearchResponse errorResponse = new HybridSearchResponse();
                    errorResponse.setSuccess(false);
                    errorResponse.setErrorMessage(throwable.getMessage());
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
                });
    }

    /**
     * 📊 Métriques et statistiques de l'orchestration IA
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getOrchestrationStatistics() {
        logger.info("📊 [AI-ORCHESTRATION] Récupération statistiques");

        try {
            Map<String, Object> stats = aiOrchestrationService.getOrchestrationStatistics();
            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            logger.error("❌ [AI-ORCHESTRATION] Erreur récupération stats: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 🔥 Préchauffage des services IA
     */
    @PostMapping("/warmup")
    public ResponseEntity<Map<String, Object>> warmupServices() {
        logger.info("🔥 [AI-ORCHESTRATION] Préchauffage services IA");

        try {
            aiOrchestrationService.warmupAIServices();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Préchauffage des services IA lancé en arrière-plan");
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("❌ [AI-ORCHESTRATION] Erreur préchauffage: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "error", e.getMessage()
                    ));
        }
    }

    /**
     * 🗑️ Nettoyage de tous les caches IA
     */
    @PostMapping("/cache/clear-all")
    public ResponseEntity<Map<String, Object>> clearAllCaches() {
        logger.info("🗑️ [AI-ORCHESTRATION] Nettoyage tous caches IA");

        try {
            aiOrchestrationService.clearAllAICaches();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Tous les caches IA ont été vidés");
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("❌ [AI-ORCHESTRATION] Erreur nettoyage caches: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "error", e.getMessage()
                    ));
        }
    }

    /**
     * 🧪 Test de santé de tous les services IA
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        logger.info("🧪 [AI-ORCHESTRATION] Test santé services IA");

        Map<String, Object> health = new HashMap<>();
        boolean allHealthy = true;

        try {
            // Tester chaque service IA
            Map<String, Object> serviceHealth = new HashMap<>();

            // Test simple pour vérifier que les services répondent
            try {
                Map<String, Object> stats = aiOrchestrationService.getOrchestrationStatistics();
                serviceHealth.put("orchestration", "UP");
                serviceHealth.put("stats", stats);
            } catch (Exception e) {
                serviceHealth.put("orchestration", "DOWN");
                serviceHealth.put("error", e.getMessage());
                allHealthy = false;
            }

            health.put("services", serviceHealth);
            health.put("overall", allHealthy ? "UP" : "DOWN");
            health.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(health);

        } catch (Exception e) {
            logger.error("❌ [AI-ORCHESTRATION] Erreur test santé: {}", e.getMessage());
            health.put("overall", "DOWN");
            health.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(health);
        }
    }
}
