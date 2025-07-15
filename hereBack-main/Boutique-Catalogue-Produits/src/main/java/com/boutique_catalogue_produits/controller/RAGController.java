package com.boutique_catalogue_produits.controller;

import com.boutique_catalogue_produits.dto.RagSearchRequest;
import com.boutique_catalogue_produits.dto.RagSearchResponse;
import com.boutique_catalogue_produits.service.EnhancedRAGService;
import com.boutique_catalogue_produits.service.RAGService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/rag")
@CrossOrigin(origins = "*")
public class RAGController {

    private static final Logger logger = LoggerFactory.getLogger(RAGController.class);

    @Autowired
    private RAGService ragService;
    @Autowired
    private EnhancedRAGService enhancedRAGService;

    /**
     * 🧠 ENDPOINT PRINCIPAL POUR RECHERCHE RAG
     */
    @PostMapping("/search")
    public ResponseEntity<RagSearchResponse> searchWithRAG(@RequestBody RagSearchRequest request) {
        logger.info("🚀 [RAG] Nouvelle recherche: {} (type: {})", request.getQuery(), request.getSearchType());

        try {
            // Validation de base
            if (request.getQuery() == null || request.getQuery().trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }

            // Recherche avec RAG
            RagSearchResponse response = ragService.searchWithRAG(request.getQuery(), request.getSearchType());

            // Enrichir la réponse avec les critères de la requête
            if (request.getVille() != null) response.setVille(request.getVille());
            if (request.getRayonKm() != null) response.setRayonKm(request.getRayonKm());

            logger.info("✅ [RAG] Recherche terminée: {} résultats", response.getTotalResults());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("❌ [RAG] Erreur recherche: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse(request.getQuery(), e.getMessage()));
        }
    }

    /**
     * 🔍 RECHERCHE RAPIDE (GET)
     */
    @GetMapping("/search")
    public ResponseEntity<RagSearchResponse> quickSearch(
            @RequestParam String q,
            @RequestParam(defaultValue = "MIXED") String type) {

        logger.info("🔍 [RAG] Recherche rapide: {} ({})", q, type);

        try {
            // ✅ VALIDATION : Vérifier si la requête est valide
            if (q == null || q.trim().isEmpty() || q.length() < 2) {
                return ResponseEntity.badRequest().body(
                        createEmptyResponse(q, "Requête trop courte")
                );
            }

            RagSearchResponse response = ragService.searchWithRAG(q, type);

            // ✅ VALIDATION : Vérifier la pertinence des résultats
            if (response.getTotalResults() == 0) {
                logger.warn("⚠️ [RAG] Aucun résultat pertinent pour: {}", q);
                response.setAnalysis("Aucun résultat pertinent trouvé pour cette recherche");
                response.setSummary("Essayez avec des mots-clés différents");
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("❌ [RAG] Erreur recherche rapide: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse(q, e.getMessage()));
        }
    }

    private RagSearchResponse createEmptyResponse(String query, String message) {
        RagSearchResponse response = new RagSearchResponse();
        response.setQuery(query);
        response.setSearchType("MIXED");
        response.setTimestamp(new Date());
        response.setRagEnhanced(false);
        response.setTotalResults(0);
        response.setNombreProduits(0);
        response.setNombreBoutiques(0);
        response.setProduits(new ArrayList<>());
        response.setBoutiques(new ArrayList<>());
        response.setAnalysis("❌ Requête non traitée : " + message);
        response.setSummary("Aucun résultat - " + message);
        return response;
    }


    /**
     * 🎯 RECHERCHE DE PRODUITS AVEC RAG
     */
    @GetMapping("/produits")
    public ResponseEntity<RagSearchResponse> searchProduits(@RequestParam String q) {
        logger.info("🛍️ [RAG] Recherche produits: {}", q);

        try {
            RagSearchResponse response = ragService.searchWithRAG(q, "PRODUIT");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("❌ [RAG] Erreur recherche produits: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse(q, e.getMessage()));
        }
    }

    /**
     * 🏪 RECHERCHE DE BOUTIQUES AVEC RAG
     */
    @GetMapping("/boutiques")
    public ResponseEntity<RagSearchResponse> searchBoutiques(@RequestParam String q) {
        logger.info("🏪 [RAG] Recherche boutiques: {}", q);

        try {
            RagSearchResponse response = ragService.searchWithRAG(q, "BOUTIQUE");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("❌ [RAG] Erreur recherche boutiques: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse(q, e.getMessage()));
        }
    }

    /**
     * 📊 RECHERCHE AVEC RECOMMANDATIONS
     */
    @PostMapping("/recommendations")
    public ResponseEntity<RagSearchResponse> getRecommendations(@RequestBody RagSearchRequest request) {
        logger.info("💡 [RAG] Demande recommandations: {}", request.getQuery());

        try {
            RagSearchResponse response = ragService.searchWithRAG(request.getQuery(), "MIXED");

            // Enrichir avec recommandations spécifiques
            ragService.enrichWithRecommendations(response, request);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("❌ [RAG] Erreur recommandations: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse(request.getQuery(), e.getMessage()));
        }
    }

    /**
     * 🧪 ENDPOINT DE TEST RAG
     */
    @GetMapping("/test")
    public ResponseEntity<Map<String, Object>> testRAG(@RequestParam String q) {
        logger.info("🧪 [RAG] Test système: {}", q);

        Map<String, Object> testResult = new HashMap<>();
        testResult.put("query", q);
        testResult.put("timestamp", System.currentTimeMillis());

        try {
            // Test du service RAG
            RagSearchResponse response = ragService.searchWithRAG(q, "MIXED");

            testResult.put("ragStatus", "✅ Fonctionnel");
            testResult.put("resultCount", response.getTotalResults());
            testResult.put("ragEnhanced", response.isRagEnhanced());
            testResult.put("processingTime", response.getProcessingTimeMs() + "ms");
            testResult.put("confidence", response.getConfidenceScore());

            return ResponseEntity.ok(testResult);

        } catch (Exception e) {
            testResult.put("ragStatus", "❌ Erreur: " + e.getMessage());
            testResult.put("error", e.getClass().getSimpleName());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(testResult);
        }
    }

    /**
     * 💾 ENDPOINT POUR VIDER LE CACHE RAG
     */
    @PostMapping("/cache/clear")
    public ResponseEntity<Map<String, String>> clearCache() {
        logger.info("🗑️ [RAG] Nettoyage du cache");

        try {
            ragService.clearCache();

            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Cache RAG vidé avec succès");
            response.put("timestamp", java.time.LocalDateTime.now().toString());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("❌ [RAG] Erreur nettoyage cache: {}", e.getMessage());

            Map<String, String> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * 📈 ENDPOINT POUR STATISTIQUES RAG
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getRAGStats() {
        logger.info("📊 [RAG] Demande de statistiques");

        try {
            Map<String, Object> stats = ragService.getStatistics();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            logger.error("❌ [RAG] Erreur statistiques: {}", e.getMessage());

            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            error.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Créer une réponse d'erreur
     */
    private RagSearchResponse createErrorResponse(String query, String errorMessage) {
        RagSearchResponse errorResponse = new RagSearchResponse();
        errorResponse.setQuery(query);
        errorResponse.setRagEnhanced(false);
        errorResponse.setAnalysis("Erreur lors de la recherche: " + errorMessage);
        errorResponse.setSummary("Recherche échouée");
        errorResponse.setTotalResults(0);
        errorResponse.setTimestamp(new java.util.Date());
        return errorResponse;
    }

    @GetMapping("/search/advanced")
    public RagSearchResponse advancedSearch(@RequestParam String q, @RequestParam String type) {
        return enhancedRAGService.semanticSearch(q, type);
    }

    @GetMapping("/search/semantic")
    public RagSearchResponse semanticSearch(@RequestParam String q, @RequestParam String type) {
        return enhancedRAGService.semanticSearch(q, type);
    }
}