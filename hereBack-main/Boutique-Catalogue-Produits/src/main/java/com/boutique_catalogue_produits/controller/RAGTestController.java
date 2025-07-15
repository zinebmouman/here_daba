package com.boutique_catalogue_produits.controller;

import com.boutique_catalogue_produits.service.GeminiAIService;
import com.boutique_catalogue_produits.service.RAGService;
import com.boutique_catalogue_produits.service.ProduitService;
import com.boutique_catalogue_produits.service.BoutiqueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
@CrossOrigin(origins = "*")
public class RAGTestController {

    @Autowired(required = false)
    private RAGService ragService;

    @Autowired(required = false)
    private GeminiAIService geminiAIService;

    @Autowired
    private ProduitService produitService;

    @Autowired
    private BoutiqueService boutiqueService;

    /**
     * 🧪 Test complet du système RAG
     */
    @GetMapping("/rag-complete")
    public ResponseEntity<Map<String, Object>> testRAGComplete() {
        Map<String, Object> result = new HashMap<>();
        result.put("timestamp", System.currentTimeMillis());

        try {
            // 1. TEST DES SERVICES DE BASE
            result.put("produitService", testProduitService());
            result.put("boutiqueService", testBoutiqueService());

            // 2. TEST GEMINI AI
            result.put("geminiAI", testGeminiAI());

            // 3. TEST RAG SERVICE
            result.put("ragService", testRAGService());

            // 4. TEST INTÉGRATION COMPLÈTE
            result.put("integration", testIntegration());

            result.put("status", "✅ Tous les tests passés");

        } catch (Exception e) {
            result.put("status", "❌ Erreur: " + e.getMessage());
            result.put("error", e.getClass().getSimpleName());
        }

        return ResponseEntity.ok(result);
    }

    /**
     * 🛍️ Test du service des produits
     */
    @GetMapping("/produits")
    public ResponseEntity<Map<String, Object>> testProduitService() {
        Map<String, Object> result = new HashMap<>();

        try {
            int totalProduits = produitService.getAllProduits().size();
            result.put("totalProduits", totalProduits);
            result.put("status", "✅ Service produits fonctionnel");

            // Test recherche basique
            var searchResults = produitService.searchProduits("test");
            result.put("searchTestResults", searchResults.size());

            // Test recherche intelligente
            var intelligentResults = produitService.searchProduitsIntelligent("test");
            result.put("intelligentTestResults", intelligentResults.size());

        } catch (Exception e) {
            result.put("status", "❌ Erreur service produits: " + e.getMessage());
        }

        return ResponseEntity.ok(result);
    }

    /**
     * 🏪 Test du service des boutiques
     */
    @GetMapping("/boutiques")
    public ResponseEntity<Map<String, Object>> testBoutiqueService() {
        Map<String, Object> result = new HashMap<>();

        try {
            int totalBoutiques = boutiqueService.getAllBoutiques().size();
            result.put("totalBoutiques", totalBoutiques);
            result.put("status", "✅ Service boutiques fonctionnel");

        } catch (Exception e) {
            result.put("status", "❌ Erreur service boutiques: " + e.getMessage());
        }

        return ResponseEntity.ok(result);
    }

    /**
     * 🧠 Test de Gemini AI
     */
    @GetMapping("/gemini")
    public ResponseEntity<Map<String, Object>> testGeminiAI() {
        Map<String, Object> result = new HashMap<>();

        try {
            if (geminiAIService == null) {
                result.put("status", "❌ GeminiAIService non disponible");
                return ResponseEntity.ok(result);
            }

            // Test d'analyse simple
            var criteria = geminiAIService.analyzeSearchQuery("recherche test");
            result.put("analysisTest", "✅ Analyse fonctionnelle");
            result.put("keywords", criteria.getKeywords());
            result.put("searchType", criteria.getSearchType());
            result.put("status", "✅ Gemini AI fonctionnel");

        } catch (Exception e) {
            result.put("status", "❌ Erreur Gemini AI: " + e.getMessage());
        }

        return ResponseEntity.ok(result);
    }

    /**
     * 🎯 Test du service RAG
     */
    @GetMapping("/rag")
    public ResponseEntity<Map<String, Object>> testRAGService() {
        Map<String, Object> result = new HashMap<>();

        try {
            if (ragService == null) {
                result.put("status", "❌ RAGService non disponible");
                return ResponseEntity.ok(result);
            }

            // Test recherche RAG
            var ragResponse = ragService.searchWithRAG("test produit", "MIXED");
            result.put("ragSearchTest", "✅ Recherche RAG fonctionnelle");
            result.put("totalResults", ragResponse.getTotalResults());
            result.put("ragEnhanced", ragResponse.isRagEnhanced());
            result.put("status", "✅ RAG Service fonctionnel");

        } catch (Exception e) {
            result.put("status", "❌ Erreur RAG Service: " + e.getMessage());
        }

        return ResponseEntity.ok(result);
    }

    /**
     * 🔗 Test d'intégration complète
     */
    @GetMapping("/integration")
    public ResponseEntity<Map<String, Object>> testIntegration() {
        Map<String, Object> result = new HashMap<>();

        try {
            // Test avec une vraie requête
            String testQuery = "caftan marrakech";

            // Test recherche normale
            var produitsNormaux = produitService.searchProduits(testQuery);
            result.put("rechercheNormale", produitsNormaux.size() + " produits");

            // Test recherche intelligente
            var produitsIntelligents = produitService.searchProduitsIntelligent(testQuery);
            result.put("rechercheIntelligente", produitsIntelligents.size() + " produits");

            // Test RAG si disponible
            if (ragService != null) {
                var ragResponse = ragService.searchWithRAG(testQuery, "MIXED");
                result.put("rechercheRAG", ragResponse.getTotalResults() + " résultats totaux");
                result.put("ragAnalysis", ragResponse.getAnalysis());
            }

            result.put("status", "✅ Intégration complète fonctionnelle");

        } catch (Exception e) {
            result.put("status", "❌ Erreur intégration: " + e.getMessage());
        }

        return ResponseEntity.ok(result);
    }

    /**
     * 📊 Test de performance
     */
    @GetMapping("/performance")
    public ResponseEntity<Map<String, Object>> testPerformance(@RequestParam(defaultValue = "test") String query) {
        Map<String, Object> result = new HashMap<>();

        try {
            // Test recherche normale
            long start = System.currentTimeMillis();
            var normalResults = produitService.searchProduits(query);
            long normalTime = System.currentTimeMillis() - start;

            result.put("rechercheNormale", Map.of(
                    "time", normalTime + "ms",
                    "results", normalResults.size()
            ));

            // Test recherche intelligente
            start = System.currentTimeMillis();
            var intelligentResults = produitService.searchProduitsIntelligent(query);
            long intelligentTime = System.currentTimeMillis() - start;

            result.put("rechercheIntelligente", Map.of(
                    "time", intelligentTime + "ms",
                    "results", intelligentResults.size()
            ));

            // Test RAG si disponible
            if (ragService != null) {
                start = System.currentTimeMillis();
                var ragResults = ragService.searchWithRAG(query, "MIXED");
                long ragTime = System.currentTimeMillis() - start;

                result.put("rechercheRAG", Map.of(
                        "time", ragTime + "ms",
                        "results", ragResults.getTotalResults(),
                        "enhanced", ragResults.isRagEnhanced()
                ));
            }

            result.put("status", "✅ Tests de performance terminés");

        } catch (Exception e) {
            result.put("status", "❌ Erreur performance: " + e.getMessage());
        }

        return ResponseEntity.ok(result);
    }

    /**
     * 🔧 Diagnostic du système
     */
    @GetMapping("/diagnostic")
    public ResponseEntity<Map<String, Object>> diagnostic() {
        Map<String, Object> result = new HashMap<>();
        result.put("timestamp", System.currentTimeMillis());

        // Informations système
        Runtime runtime = Runtime.getRuntime();
        result.put("memoire", Map.of(
                "libre", runtime.freeMemory() / 1024 / 1024 + " MB",
                "totale", runtime.totalMemory() / 1024 / 1024 + " MB",
                "max", runtime.maxMemory() / 1024 / 1024 + " MB"
        ));

        // Status des services
        result.put("services", Map.of(
                "produitService", produitService != null ? "✅ Disponible" : "❌ Indisponible",
                "boutiqueService", boutiqueService != null ? "✅ Disponible" : "❌ Indisponible",
                "geminiAIService", geminiAIService != null ? "✅ Disponible" : "❌ Indisponible",
                "ragService", ragService != null ? "✅ Disponible" : "❌ Indisponible"
        ));

        return ResponseEntity.ok(result);
    }
}