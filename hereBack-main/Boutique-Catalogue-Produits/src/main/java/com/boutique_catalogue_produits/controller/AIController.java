package com.boutique_catalogue_produits.controller;

import com.boutique_catalogue_produits.dto.*;
import com.boutique_catalogue_produits.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 🤖 Contrôleur pour les services d'Intelligence Artificielle
 * Recherche par image, recommandations intelligentes, analyse IA
 */
@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = "*")
public class AIController {

    private static final Logger logger = LoggerFactory.getLogger(AIController.class);

    @Autowired
    private ImageSearchService imageSearchService;

    @Autowired
    private RecommendationService recommendationService;

    @Autowired
    private ImageEmbeddingService imageEmbeddingService;

    @Autowired
    private RAGService ragService;

    // =============== ENDPOINTS RECHERCHE PAR IMAGE ===============

    /**
     * 🖼️ Recherche par upload d'image
     */
    @PostMapping(value = "/image/search/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImageSearchResponse> searchByUploadedImage(
            @RequestParam("image") MultipartFile imageFile,
            @RequestParam(defaultValue = "MIXED") String searchType,
            @RequestParam(required = false) String userId) {

        logger.info("🖼️ [AI-API] Recherche par image uploadée (type: {}, user: {})", searchType, userId);

        try {
            ImageSearchResponse response = imageSearchService.searchByUploadedImage(imageFile, searchType);

            // Enrichir avec des recommandations si un utilisateur est fourni
            if (userId != null && response.isSuccess()) {
                List<RecommendationItem> imageRecs = recommendationService.recommendFromImageSearch(
                        response.getSimilarProducts(), userId);
                response.setRecommendations(
                        imageRecs.stream().map(RecommendationItem::getTitle).limit(5).toList());
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("❌ [AI-API] Erreur recherche image uploadée: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorImageResponse("Upload failed: " + e.getMessage()));
        }
    }

    /**
     * 🔗 Recherche par URL d'image
     */
    @PostMapping("/image/search/url")
    public ResponseEntity<ImageSearchResponse> searchByImageUrl(
            @RequestBody ImageSearchRequest request) {

        logger.info("🔗 [AI-API] Recherche par URL: {}", request.getImageUrl());

        try {
            ImageSearchResponse response = imageSearchService.searchByImageUrl(
                    request.getImageUrl(), request.getSearchType());

            // Enrichir avec recommandations
            if (request.getUserId() != null && response.isSuccess()) {
                List<RecommendationItem> imageRecs = recommendationService.recommendFromImageSearch(
                        response.getSimilarProducts(), request.getUserId());
                response.setRecommendations(
                        imageRecs.stream().map(RecommendationItem::getTitle).limit(5).toList());
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("❌ [AI-API] Erreur recherche image URL: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorImageResponse("URL search failed: " + e.getMessage()));
        }
    }

    /**
     * 🎯 Recherche de produits similaires par ID
     */
    @GetMapping("/image/similar/{productId}")
    public ResponseEntity<Map<String, Object>> findSimilarProducts(
            @PathVariable String productId,
            @RequestParam(defaultValue = "10") int limit) {

        logger.info("🎯 [AI-API] Produits similaires à: {}", productId);

        try {
            List<SimilarImageResult> similarProducts = imageSearchService.findSimilarProducts(productId, limit);

            Map<String, Object> response = new HashMap<>();
            response.put("productId", productId);
            response.put("similarProducts", similarProducts);
            response.put("totalResults", similarProducts.size());
            response.put("algorithm", "image_similarity_ai");
            response.put("timestamp", new Date());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("❌ [AI-API] Erreur produits similaires: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 🧠 Analyse d'image avec IA
     */
    @PostMapping(value = "/image/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> analyzeImage(
            @RequestParam("image") MultipartFile imageFile) {

        logger.info("🧠 [AI-API] Analyse IA de l'image: {}", imageFile.getOriginalFilename());

        try {
            Map<String, Object> analysis = imageSearchService.analyzeImageWithAI(imageFile);

            Map<String, Object> response = new HashMap<>();
            response.put("fileName", imageFile.getOriginalFilename());
            response.put("analysis", analysis);
            response.put("success", true);
            response.put("timestamp", new Date());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("❌ [AI-API] Erreur analyse image: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "error", e.getMessage(),
                            "fileName", imageFile.getOriginalFilename()
                    ));
        }
    }

    // =============== ENDPOINTS RECOMMANDATIONS ===============

    /**
     * 💡 Recommandations de produits pour un utilisateur
     */
    @GetMapping("/recommendations/products/{userId}")
    public ResponseEntity<RecommendationResponse> getProductRecommendations(
            @PathVariable String userId,
            @RequestParam(defaultValue = "10") int limit) {

        logger.info("💡 [AI-API] Recommandations produits pour: {}", userId);

        try {
            long startTime = System.currentTimeMillis();

            List<RecommendationItem> recommendations = recommendationService.recommendProductsForUser(userId, limit);

            RecommendationResponse response = new RecommendationResponse();
            response.setUserId(userId);
            response.setRecommendationType("USER_BASED_HYBRID");
            response.setRecommendations(recommendations);
            response.setAlgorithm("collaborative_content_ai");
            response.setConfidenceScore(calculateConfidenceScore(recommendations));
            response.setProcessingTimeMs(System.currentTimeMillis() - startTime);

            // Métadonnées
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("averageScore", calculateAverageScore(recommendations));
            metadata.put("categories", extractCategories(recommendations));
            metadata.put("aiEnhanced", true);
            response.setMetadata(metadata);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("❌ [AI-API] Erreur recommandations produits: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorRecommendationResponse(userId, "PRODUCTS", e));
        }
    }

    /**
     * 🏪 Recommandations de boutiques pour un utilisateur
     */
    @GetMapping("/recommendations/boutiques/{userId}")
    public ResponseEntity<RecommendationResponse> getBoutiqueRecommendations(
            @PathVariable String userId,
            @RequestParam(required = false) String ville,
            @RequestParam(defaultValue = "5") int limit) {

        logger.info("🏪 [AI-API] Recommandations boutiques pour: {} (ville: {})", userId, ville);

        try {
            long startTime = System.currentTimeMillis();

            List<RecommendationItem> recommendations = recommendationService.recommendBoutiquesForUser(
                    userId, ville, limit);

            RecommendationResponse response = new RecommendationResponse();
            response.setUserId(userId);
            response.setRecommendationType("BOUTIQUE_LOCATION_BASED");
            response.setRecommendations(recommendations);
            response.setAlgorithm("location_preference_ai");
            response.setConfidenceScore(calculateConfidenceScore(recommendations));
            response.setProcessingTimeMs(System.currentTimeMillis() - startTime);

            // Métadonnées spécifiques aux boutiques
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("ville", ville);
            metadata.put("averageScore", calculateAverageScore(recommendations));
            metadata.put("locationBased", ville != null);
            response.setMetadata(metadata);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("❌ [AI-API] Erreur recommandations boutiques: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorRecommendationResponse(userId, "BOUTIQUES", e));
        }
    }

    /**
     * 🔍 Recommandations basées sur un produit spécifique
     */
    @GetMapping("/recommendations/similar-products/{productId}")
    public ResponseEntity<RecommendationResponse> getSimilarProductRecommendations(
            @PathVariable String productId,
            @RequestParam(defaultValue = "8") int limit) {

        logger.info("🔍 [AI-API] Recommandations produits similaires à: {}", productId);

        try {
            long startTime = System.currentTimeMillis();

            List<RecommendationItem> recommendations = recommendationService.recommendSimilarProducts(
                    productId, limit);

            RecommendationResponse response = new RecommendationResponse();
            response.setUserId("anonymous");
            response.setRecommendationType("ITEM_BASED");
            response.setRecommendations(recommendations);
            response.setAlgorithm("content_image_similarity_ai");
            response.setConfidenceScore(calculateConfidenceScore(recommendations));
            response.setProcessingTimeMs(System.currentTimeMillis() - startTime);

            // Métadonnées spécifiques
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("referenceProductId", productId);
            metadata.put("similarityBased", true);
            metadata.put("aiEnhanced", true);
            response.setMetadata(metadata);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("❌ [AI-API] Erreur recommandations produits similaires: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorRecommendationResponse("anonymous", "SIMILAR_PRODUCTS", e));
        }
    }

    /**
     * 🧠 Recommandations contextuelles intelligentes
     */
    @PostMapping("/recommendations/contextual")
    public ResponseEntity<RecommendationResponse> getContextualRecommendations(
            @RequestBody RecommendationRequest request) {

        logger.info("🧠 [AI-API] Recommandations contextuelles: {} (contexte: {})",
                request.getUserId(), request.getContextType());

        try {
            long startTime = System.currentTimeMillis();

            List<RecommendationItem> recommendations = recommendationService.getContextualRecommendations(
                    request.getUserId(),
                    request.getContextType(),
                    request.getParameters());

            RecommendationResponse response = new RecommendationResponse();
            response.setUserId(request.getUserId());
            response.setRecommendationType("CONTEXTUAL_AI");
            response.setRecommendations(recommendations);
            response.setAlgorithm("context_aware_ai");
            response.setConfidenceScore(calculateConfidenceScore(recommendations));
            response.setProcessingTimeMs(System.currentTimeMillis() - startTime);

            // Métadonnées contextuelles
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("contextType", request.getContextType());
            metadata.put("parameters", request.getParameters());
            metadata.put("aiGenerated", true);
            response.setMetadata(metadata);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("❌ [AI-API] Erreur recommandations contextuelles: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorRecommendationResponse(request.getUserId(), "CONTEXTUAL", e));
        }
    }

    // =============== ENDPOINTS HYBRIDES RAG + IA ===============

    /**
     * 🚀 Recherche intelligente avec recommandations IA
     */
    @GetMapping("/search/intelligent-with-recommendations")
    public ResponseEntity<Map<String, Object>> intelligentSearchWithRecommendations(
            @RequestParam String query,
            @RequestParam(defaultValue = "MIXED") String searchType,
            @RequestParam(required = false) String userId) {

        logger.info("🚀 [AI-API] Recherche intelligente + recommandations: '{}' (user: {})", query, userId);

        try {
            long startTime = System.currentTimeMillis();

            // 1. Recherche RAG principale
            RagSearchResponse ragResults = ragService.searchWithRAG(query, searchType);

            // 2. Générer des recommandations basées sur les résultats
            List<RecommendationItem> smartRecommendations = new ArrayList<>();
            if (userId != null) {
                // Recommandations utilisateur
                smartRecommendations.addAll(
                        recommendationService.recommendProductsForUser(userId, 5));

                // Recommandations contextuelles basées sur la recherche
                Map<String, Object> contextParams = Map.of("searchQuery", query);
                smartRecommendations.addAll(
                        recommendationService.getContextualRecommendations(userId, "search_based", contextParams));
            }

            // 3. Construire la réponse hybride
            Map<String, Object> response = new HashMap<>();
            response.put("searchResults", ragResults);
            response.put("aiRecommendations", smartRecommendations);
            response.put("query", query);
            response.put("userId", userId);
            response.put("hybridSearch", true);
            response.put("processingTimeMs", System.currentTimeMillis() - startTime);

            // Statistiques combinées
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalSearchResults", ragResults.getTotalResults());
            stats.put("totalRecommendations", smartRecommendations.size());
            stats.put("ragEnhanced", ragResults.isRagEnhanced());
            stats.put("aiEnhanced", true);
            response.put("statistics", stats);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("❌ [AI-API] Erreur recherche intelligente + recommandations: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", e.getMessage(),
                            "query", query,
                            "searchType", searchType,
                            "hybridSearch", false
                    ));
        }
    }

    // =============== ENDPOINTS DE GESTION ET MONITORING ===============

    /**
     * 📊 Statistiques des services IA
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getAIStatistics() {
        logger.info("📊 [AI-API] Récupération statistiques IA");

        try {
            Map<String, Object> stats = new HashMap<>();

            // Stats recherche par image
            stats.put("imageSearch", imageSearchService.getStatistics());

            // Stats embeddings
            stats.put("imageEmbedding", imageEmbeddingService.getStatistics());

            // Stats RAG
            if (ragService != null) {
                stats.put("ragService", ragService.getStatistics());
            }

            // Stats générales
            stats.put("timestamp", new Date());
            stats.put("servicesActive", true);

            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            logger.error("❌ [AI-API] Erreur récupération stats: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 🗑️ Nettoyage des caches IA
     */
    @PostMapping("/cache/clear")
    public ResponseEntity<Map<String, Object>> clearAICaches() {
        logger.info("🗑️ [AI-API] Nettoyage caches IA");

        try {
            // Nettoyer tous les caches
            imageSearchService.clearCache();
            imageEmbeddingService.clearCache();
            recommendationService.clearCache();

            if (ragService != null) {
                ragService.clearCache();
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Tous les caches IA ont été vidés");
            response.put("timestamp", new Date());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("❌ [AI-API] Erreur nettoyage caches: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "error", e.getMessage()
                    ));
        }
    }

    /**
     * 🔄 Actualisation des embeddings produits
     */
    @PostMapping("/embeddings/refresh")
    public ResponseEntity<Map<String, Object>> refreshProductEmbeddings() {
        logger.info("🔄 [AI-API] Actualisation embeddings produits");

        try {
            imageSearchService.refreshProductEmbeddings();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Embeddings produits actualisés");
            response.put("timestamp", new Date());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("❌ [AI-API] Erreur actualisation embeddings: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "error", e.getMessage()
                    ));
        }
    }

    // =============== MÉTHODES UTILITAIRES ===============

    private ImageSearchResponse createErrorImageResponse(String errorMessage) {
        ImageSearchResponse response = new ImageSearchResponse();
        response.setSuccess(false);
        response.setErrorMessage(errorMessage);
        response.setTimestamp(new Date());
        response.setSimilarProducts(new ArrayList<>());
        response.setTotalResults(0);
        return response;
    }

    private RecommendationResponse createErrorRecommendationResponse(String userId, String type, Exception e) {
        RecommendationResponse response = new RecommendationResponse();
        response.setUserId(userId);
        response.setRecommendationType(type + "_ERROR");
        response.setRecommendations(new ArrayList<>());
        response.setConfidenceScore(0.0);
        response.setMetadata(Map.of("error", e.getMessage()));
        return response;
    }

    private double calculateConfidenceScore(List<RecommendationItem> recommendations) {
        if (recommendations == null || recommendations.isEmpty()) {
            return 0.0;
        }

        double averageScore = recommendations.stream()
                .mapToDouble(RecommendationItem::getScore)
                .average()
                .orElse(0.0);

        // Ajuster selon le nombre de recommandations
        double sizeBonus = Math.min(0.2, recommendations.size() * 0.02);

        return Math.min(1.0, averageScore + sizeBonus);
    }

    private double calculateAverageScore(List<RecommendationItem> recommendations) {
        if (recommendations == null || recommendations.isEmpty()) {
            return 0.0;
        }

        return recommendations.stream()
                .mapToDouble(RecommendationItem::getScore)
                .average()
                .orElse(0.0);
    }

    private Set<String> extractCategories(List<RecommendationItem> recommendations) {
        if (recommendations == null || recommendations.isEmpty()) {
            return new HashSet<>();
        }

        return recommendations.stream()
                .map(RecommendationItem::getCategory)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }
}