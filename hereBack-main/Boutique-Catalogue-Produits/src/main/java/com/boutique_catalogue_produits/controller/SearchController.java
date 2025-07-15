package com.boutique_catalogue_produits.controller;

import com.boutique_catalogue_produits.dto.ProduitDTO;
import com.boutique_catalogue_produits.dto.RagSearchResponse;
import com.boutique_catalogue_produits.dto.SearchCriteria;
import com.boutique_catalogue_produits.model.Boutique;
import com.boutique_catalogue_produits.service.RAGService;
import com.boutique_catalogue_produits.service.GeminiAIService;
import com.boutique_catalogue_produits.service.ProduitService;
import com.boutique_catalogue_produits.service.BoutiqueService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/search")
@CrossOrigin(origins = "*")
public class SearchController {

    private static final Logger logger = LoggerFactory.getLogger(SearchController.class);

    @Autowired
    private RAGService ragService;

    @Autowired
    private GeminiAIService geminiAIService;

    @Autowired
    private ProduitService produitService;

    @Autowired
    private BoutiqueService boutiqueService;

    /**
     * 🚀 ENDPOINT PRINCIPAL - Recherche intelligente unifiée avec RAG
     */
    @GetMapping("/intelligent")
    public ResponseEntity<RagSearchResponse> rechercheIntelligente(
            @RequestParam String q,
            @RequestParam(defaultValue = "MIXED") String type,
            @RequestParam(defaultValue = "true") boolean enableRAG,
            @RequestParam(required = false) String ville,
            @RequestParam(required = false) Integer rayonKm,
            @RequestParam(required = false) Double prixMin,
            @RequestParam(required = false) Double prixMax,
            @RequestParam(required = false) String categorie) {

        long startTime = System.currentTimeMillis();

        logger.info("🔍 [SEARCH] Recherche intelligente: '{}' (type: {}, RAG: {})", q, type, enableRAG);

        try {
            RagSearchResponse response;

            if (enableRAG) {
                // ✅ RECHERCHE AVEC RAG - MÉTHODE PRINCIPALE
                response = ragService.searchWithRAG(q, type);

                // Enrichir avec paramètres additionnels
                if (ville != null) response.setVille(ville);
                if (rayonKm != null) response.setRayonKm(rayonKm);

            } else {
                // 🔄 FALLBACK - Recherche classique
                response = performClassicSearch(q, type, ville, categorie, prixMin, prixMax);
            }

            // Métadonnées de performance
            long processingTime = System.currentTimeMillis() - startTime;
            response.setProcessingTimeMs(processingTime);
            response.setAiModel("Gemini-1.5-Pro");

            logger.info("✅ [SEARCH] Terminé en {}ms: {} résultats (RAG: {})",
                    processingTime, response.getTotalResults(), response.isRagEnhanced());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("❌ [SEARCH] Erreur recherche intelligente: {}", e.getMessage(), e);

            // Réponse d'erreur avec fallback
            RagSearchResponse errorResponse = createErrorResponse(q, type, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 🔍 ENDPOINT - Recherche produits uniquement avec RAG
     */
    @GetMapping("/produits")
    public ResponseEntity<Map<String, Object>> rechercheProduitsRAG(
            @RequestParam String q,
            @RequestParam(defaultValue = "true") boolean enableRAG) {

        logger.info("🛍️ [SEARCH] Recherche produits RAG: '{}'", q);

        try {
            RagSearchResponse ragResponse = ragService.searchWithRAG(q, "PRODUIT");

            Map<String, Object> response = new HashMap<>();
            response.put("query", q);
            response.put("ragEnhanced", ragResponse.isRagEnhanced());
            response.put("analysis", ragResponse.getAnalysis());
            response.put("produits", ragResponse.getProduits());
            response.put("count", ragResponse.getNombreProduits());
            response.put("alternatives", ragResponse.getAlternatives());
            response.put("tips", ragResponse.getTips());
            response.put("processingTime", ragResponse.getProcessingTimeMs());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("❌ [SEARCH] Erreur recherche produits: {}", e.getMessage());
            return createProductErrorResponse(q, e);
        }
    }

    /**
     * 🏪 ENDPOINT - Recherche boutiques uniquement avec RAG
     */
    @GetMapping("/boutiques")
    public ResponseEntity<Map<String, Object>> rechercheBoutiquesRAG(
            @RequestParam String q,
            @RequestParam(required = false) String ville,
            @RequestParam(defaultValue = "true") boolean enableRAG) {

        logger.info("🏪 [SEARCH] Recherche boutiques RAG: '{}' (ville: {})", q, ville);

        try {
            RagSearchResponse ragResponse = ragService.searchWithRAG(q, "BOUTIQUE");

            // Filtrer par ville si spécifié
            List<Boutique> boutiques = ragResponse.getBoutiques();
            if (ville != null && !ville.isEmpty()) {
                boutiques = boutiques.stream()
                        .filter(b -> ville.equalsIgnoreCase(b.getVille()))
                        .collect(Collectors.toList());
            }

            Map<String, Object> response = new HashMap<>();
            response.put("query", q);
            response.put("ville", ville);
            response.put("ragEnhanced", ragResponse.isRagEnhanced());
            response.put("analysis", ragResponse.getAnalysis());
            response.put("boutiques", boutiques);
            response.put("count", boutiques.size());
            response.put("alternatives", ragResponse.getAlternatives());
            response.put("tips", ragResponse.getTips());
            response.put("processingTime", ragResponse.getProcessingTimeMs());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("❌ [SEARCH] Erreur recherche boutiques: {}", e.getMessage());
            return createBoutiqueErrorResponse(q, ville, e);
        }
    }

    /**
     * 🎯 ENDPOINT - Recherche combinée produits + boutiques (comme existant amélioré)
     */
    @GetMapping("/combined")
    public ResponseEntity<Map<String, Object>> rechercheCombinee(@RequestParam String q) {
        logger.info("🎯 [SEARCH] Recherche combinée RAG: '{}'", q);

        try {
            long startTime = System.currentTimeMillis();

            // Recherche RAG combinée
            RagSearchResponse ragResponse = ragService.searchWithRAG(q, "MIXED");

            // Construire la réponse dans le format attendu par le frontend
            Map<String, Object> response = new HashMap<>();
            response.put("query", q);
            response.put("ragEnhanced", true);
            response.put("timestamp", LocalDateTime.now());

            // Données principales
            response.put("produits", ragResponse.getProduits());
            response.put("boutiques", ragResponse.getBoutiques());

            // Créer la structure "boutiquesAvecProduits" comme dans le code existant
            List<Map<String, Object>> boutiquesAvecProduits = createBoutiquesAvecProduits(
                    ragResponse.getBoutiques(), ragResponse.getProduits());
            response.put("boutiquesAvecProduits", boutiquesAvecProduits);

            // Statistiques
            response.put("nombreProduits", ragResponse.getNombreProduits());
            response.put("nombreBoutiques", ragResponse.getNombreBoutiques());
            response.put("nombreStocks", ragResponse.getProduits().stream()
                    .map(ProduitDTO::getIdStock)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet()).size());

            // Analyse IA
            response.put("analysis", ragResponse.getAnalysis());
            response.put("alternatives", ragResponse.getAlternatives());
            response.put("tips", ragResponse.getTips());

            // Message intelligent
            String message = createIntelligentMessage(q, ragResponse.getNombreProduits(),
                    ragResponse.getNombreBoutiques(), true);
            response.put("message", message);

            long processingTime = System.currentTimeMillis() - startTime;
            response.put("processingTime", processingTime);

            logger.info("✅ [SEARCH] Recherche combinée terminée en {}ms", processingTime);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("❌ [SEARCH] Erreur recherche combinée: {}", e.getMessage(), e);
            return createCombinedErrorResponse(q, e);
        }
    }

    /**
     * 🧪 ENDPOINT - Diagnostic et test RAG
     */
    @GetMapping("/diagnostic")
    public ResponseEntity<Map<String, Object>> diagnosticRAG(@RequestParam String q) {
        logger.info("🧪 [SEARCH] Diagnostic RAG pour: '{}'", q);

        Map<String, Object> diagnostic = new HashMap<>();
        long startTime = System.currentTimeMillis();

        try {
            // Test 1: Analyse Gemini
            SearchCriteria criteria = geminiAIService.analyzeSearchQuery(q);
            diagnostic.put("geminiAnalysis", criteria);
            diagnostic.put("geminiWorking", true);

            // Test 2: Recherche classique
            List<ProduitDTO> classicProduits = produitService.searchProduits(q);
            diagnostic.put("classicResults", classicProduits.size());

            // Test 3: Recherche intelligente
            List<ProduitDTO> smartProduits = produitService.searchProduitsIntelligent(q);
            diagnostic.put("smartResults", smartProduits.size());

            // Test 4: RAG complet
            try {
                RagSearchResponse ragResponse = ragService.searchWithRAG(q, "MIXED");
                diagnostic.put("ragWorking", true);
                diagnostic.put("ragResults", ragResponse.getTotalResults());
                diagnostic.put("ragAnalysis", ragResponse.getAnalysis());
            } catch (Exception ragError) {
                diagnostic.put("ragWorking", false);
                diagnostic.put("ragError", ragError.getMessage());
            }

            // Métriques
            long processingTime = System.currentTimeMillis() - startTime;
            diagnostic.put("processingTime", processingTime);
            diagnostic.put("timestamp", LocalDateTime.now());
            diagnostic.put("status", "success");

            return ResponseEntity.ok(diagnostic);

        } catch (Exception e) {
            diagnostic.put("status", "error");
            diagnostic.put("error", e.getMessage());
            diagnostic.put("geminiWorking", false);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(diagnostic);
        }
    }

    // ================== MÉTHODES PRIVÉES ==================

    private RagSearchResponse performClassicSearch(String query, String type, String ville,
                                                   String categorie, Double prixMin, Double prixMax) {
        logger.info("🔄 [SEARCH] Recherche classique pour: {}", query);

        RagSearchResponse response = new RagSearchResponse(query, type);
        response.setRagEnhanced(false);
        response.setAnalysis("Recherche classique sans RAG");

        try {
            if ("PRODUIT".equals(type) || "MIXED".equals(type)) {
                List<ProduitDTO> produits = produitService.searchProduits(query);

                // Filtrer par critères
                if (categorie != null) {
                    produits = produits.stream()
                            .filter(p -> categorie.equals(p.getIdCategorie()))
                            .collect(Collectors.toList());
                }
                if (prixMin != null) {
                    produits = produits.stream()
                            .filter(p -> p.getPrix() != null && p.getPrix().doubleValue() >= prixMin)
                            .collect(Collectors.toList());
                }
                if (prixMax != null) {
                    produits = produits.stream()
                            .filter(p -> p.getPrix() != null && p.getPrix().doubleValue() <= prixMax)
                            .collect(Collectors.toList());
                }

                response.setProduits(produits);
                response.setNombreProduits(produits.size());
            }

            if ("BOUTIQUE".equals(type) || "MIXED".equals(type)) {
                List<Boutique> boutiques = boutiqueService.getAllBoutiques().stream()
                        .filter(b -> b.getNom().toLowerCase().contains(query.toLowerCase()))
                        .collect(Collectors.toList());

                if (ville != null) {
                    boutiques = boutiques.stream()
                            .filter(b -> ville.equalsIgnoreCase(b.getVille()))
                            .collect(Collectors.toList());
                }

                response.setBoutiques(boutiques);
                response.setNombreBoutiques(boutiques.size());
            }

            response.setTotalResults(response.getNombreProduits() + response.getNombreBoutiques());
            response.setSummary(String.format("Recherche classique: %d résultats trouvés", response.getTotalResults()));

        } catch (Exception e) {
            logger.error("❌ [SEARCH] Erreur recherche classique: {}", e.getMessage());
            response.setAnalysis("Erreur lors de la recherche classique: " + e.getMessage());
        }

        return response;
    }

    private List<Map<String, Object>> createBoutiquesAvecProduits(List<Boutique> boutiques, List<ProduitDTO> produits) {
        List<Map<String, Object>> result = new ArrayList<>();

        for (Boutique boutique : boutiques) {
            // Associer les produits à la boutique (logique simplifiée)
            List<ProduitDTO> produitsBoutique = produits.stream()
                    .limit(5) // Limiter pour l'exemple
                    .collect(Collectors.toList());

            if (!produitsBoutique.isEmpty()) {
                Map<String, Object> boutiqueInfo = new HashMap<>();
                boutiqueInfo.put("boutique", boutique);
                boutiqueInfo.put("produits", produitsBoutique);
                boutiqueInfo.put("nombreProduits", produitsBoutique.size());

                result.add(boutiqueInfo);
            }
        }

        return result;
    }

    private String createIntelligentMessage(String query, int nombreProduits, int nombreBoutiques, boolean ragEnhanced) {
        String prefix = ragEnhanced ? "🤖 " : "🔍 ";

        if (nombreProduits == 0 && nombreBoutiques == 0) {
            return prefix + String.format("Aucun résultat trouvé pour '%s'. Essayez d'autres mots-clés.", query);
        } else if (nombreBoutiques == 0) {
            return prefix + String.format("%d produit(s) trouvé(s) pour '%s', mais aucune boutique disponible.",
                    nombreProduits, query);
        } else if (nombreProduits == 0) {
            return prefix + String.format("%d boutique(s) trouvée(s) pour '%s', mais aucun produit disponible.",
                    nombreBoutiques, query);
        } else {
            return prefix + String.format("Excellent ! %d produit(s) dans %d boutique(s) pour '%s'%s",
                    nombreProduits, nombreBoutiques, query,
                    ragEnhanced ? " (optimisé par IA)" : "");
        }
    }

    private RagSearchResponse createErrorResponse(String query, String type, String errorMessage) {
        RagSearchResponse response = new RagSearchResponse(query, type);
        response.setRagEnhanced(false);
        response.setAnalysis("Erreur: " + errorMessage);
        response.setSummary("Recherche échouée");
        response.setProduits(new ArrayList<>());
        response.setBoutiques(new ArrayList<>());
        response.setTotalResults(0);
        return response;
    }

    private ResponseEntity<Map<String, Object>> createProductErrorResponse(String query, Exception e) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("query", query);
        errorResponse.put("ragEnhanced", false);
        errorResponse.put("error", e.getMessage());
        errorResponse.put("produits", Collections.emptyList());
        errorResponse.put("count", 0);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    private ResponseEntity<Map<String, Object>> createBoutiqueErrorResponse(String query, String ville, Exception e) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("query", query);
        errorResponse.put("ville", ville);
        errorResponse.put("ragEnhanced", false);
        errorResponse.put("error", e.getMessage());
        errorResponse.put("boutiques", Collections.emptyList());
        errorResponse.put("count", 0);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    private ResponseEntity<Map<String, Object>> createCombinedErrorResponse(String query, Exception e) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("query", query);
        errorResponse.put("ragEnhanced", false);
        errorResponse.put("error", e.getMessage());
        errorResponse.put("produits", Collections.emptyList());
        errorResponse.put("boutiques", Collections.emptyList());
        errorResponse.put("boutiquesAvecProduits", Collections.emptyList());
        errorResponse.put("message", "❌ Erreur lors de la recherche: " + e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}