package com.boutique_catalogue_produits.controller;

import com.boutique_catalogue_produits.service.GeminiAIService;
import com.boutique_catalogue_produits.service.RAGService;
import com.boutique_catalogue_produits.service.ProduitService;
import com.boutique_catalogue_produits.service.BoutiqueService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/rag/diagnostic")
@CrossOrigin(origins = "*")
public class RAGDiagnosticController {

    private static final Logger logger = LoggerFactory.getLogger(RAGDiagnosticController.class);

    @Autowired(required = false)
    private RAGService ragService;

    @Autowired(required = false)
    private GeminiAIService geminiAIService;

    @Autowired(required = false)
    private ProduitService produitService;

    @Autowired(required = false)
    private BoutiqueService boutiqueService;

    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    @Value("${gemini.api.url:}")
    private String geminiApiUrl;

    @Value("${rag.enabled:false}")
    private boolean ragEnabled;

    /**
     * 🔍 Diagnostic complet du système RAG
     */
    @GetMapping("/full")
    public ResponseEntity<Map<String, Object>> fullDiagnostic() {
        logger.info("🔍 [DIAGNOSTIC] Lancement du diagnostic complet RAG");

        Map<String, Object> result = new HashMap<>();
        result.put("timestamp", System.currentTimeMillis());
        result.put("status", "DIAGNOSTIC_STARTED");

        // 1. VÉRIFICATION DE LA CONFIGURATION
        result.put("configuration", checkConfiguration());

        // 2. VÉRIFICATION DES SERVICES
        result.put("services", checkServices());

        // 3. VÉRIFICATION DE L'API GEMINI
        result.put("geminiApi", checkGeminiApi());

        // 4. VÉRIFICATION DES DONNÉES
        result.put("data", checkData());

        // 5. TEST DE LA CHAÎNE COMPLÈTE
        result.put("integration", testIntegration());

        // 6. RECOMMANDATIONS
        result.put("recommendations", generateRecommendations(result));

        logger.info("🔍 [DIAGNOSTIC] Diagnostic complet terminé");
        return ResponseEntity.ok(result);
    }

    /**
     * 📋 Vérification de la configuration
     */
    private Map<String, Object> checkConfiguration() {
        Map<String, Object> config = new HashMap<>();

        try {
            config.put("ragEnabled", ragEnabled);
            config.put("geminiApiKeyPresent", geminiApiKey != null && !geminiApiKey.trim().isEmpty());
            config.put("geminiApiKeyLength", geminiApiKey != null ? geminiApiKey.length() : 0);
            config.put("geminiApiUrl", geminiApiUrl);
            config.put("geminiApiUrlValid", geminiApiUrl != null && geminiApiUrl.contains("googleapis.com"));

            // Vérifier les variables d'environnement
            config.put("systemProperties", Map.of(
                    "java.version", System.getProperty("java.version"),
                    "spring.profiles.active", System.getProperty("spring.profiles.active", "default")
            ));

            config.put("status", "✅ Configuration vérifiée");
        } catch (Exception e) {
            config.put("status", "❌ Erreur configuration: " + e.getMessage());
            config.put("error", e.getClass().getSimpleName());
        }

        return config;
    }

    /**
     * 🔧 Vérification des services
     */
    private Map<String, Object> checkServices() {
        Map<String, Object> services = new HashMap<>();

        // Vérifier RAGService
        if (ragService != null) {
            services.put("ragService", "✅ Injecté");
            try {
                Map<String, Object> stats = ragService.getStatistics();
                services.put("ragServiceStats", stats);
            } catch (Exception e) {
                services.put("ragServiceError", "❌ Erreur: " + e.getMessage());
            }
        } else {
            services.put("ragService", "❌ Non injecté");
        }

        // Vérifier GeminiAIService
        if (geminiAIService != null) {
            services.put("geminiAIService", "✅ Injecté");
            try {
                // Test simple
                var criteria = geminiAIService.analyzeSearchQuery("test");
                services.put("geminiAIServiceTest", "✅ Fonctionnel");
                services.put("geminiAIServiceResult", Map.of(
                        "keywords", criteria.getKeywords(),
                        "searchType", criteria.getSearchType()
                ));
            } catch (Exception e) {
                services.put("geminiAIServiceError", "❌ Erreur: " + e.getMessage());
                services.put("geminiAIServiceStackTrace", e.getStackTrace()[0].toString());
            }
        } else {
            services.put("geminiAIService", "❌ Non injecté");
        }

        // Vérifier ProduitService
        if (produitService != null) {
            services.put("produitService", "✅ Injecté");
            try {
                int count = produitService.getAllProduits().size();
                services.put("produitServiceCount", count);
            } catch (Exception e) {
                services.put("produitServiceError", "❌ Erreur: " + e.getMessage());
            }
        } else {
            services.put("produitService", "❌ Non injecté");
        }

        // Vérifier BoutiqueService
        if (boutiqueService != null) {
            services.put("boutiqueService", "✅ Injecté");
            try {
                int count = boutiqueService.getAllBoutiques().size();
                services.put("boutiqueServiceCount", count);
            } catch (Exception e) {
                services.put("boutiqueServiceError", "❌ Erreur: " + e.getMessage());
            }
        } else {
            services.put("boutiqueService", "❌ Non injecté");
        }

        return services;
    }

    /**
     * 🧠 Vérification de l'API Gemini
     */
    private Map<String, Object> checkGeminiApi() {
        Map<String, Object> apiCheck = new HashMap<>();

        try {
            if (geminiApiKey == null || geminiApiKey.trim().isEmpty()) {
                apiCheck.put("status", "❌ Clé API manquante");
                return apiCheck;
            }

            if (geminiApiUrl == null || geminiApiUrl.trim().isEmpty()) {
                apiCheck.put("status", "❌ URL API manquante");
                return apiCheck;
            }

            // Test de connectivité (ping simple)
            apiCheck.put("keyFormat", geminiApiKey.startsWith("AIzaSy") ? "✅ Format correct" : "❌ Format suspect");
            apiCheck.put("urlFormat", geminiApiUrl.contains("googleapis.com") ? "✅ URL valide" : "❌ URL invalide");

            // Test avec GeminiAIService si disponible
            if (geminiAIService != null) {
                try {
                    var result = geminiAIService.analyzeSearchQuery("test api");
                    apiCheck.put("apiTest", "✅ API fonctionnelle");
                    apiCheck.put("apiResponse", result.getKeywords());
                } catch (Exception e) {
                    apiCheck.put("apiTest", "❌ Erreur API: " + e.getMessage());

                    // Analyser le type d'erreur
                    if (e.getMessage().contains("401")) {
                        apiCheck.put("errorType", "AUTHENTICATION_ERROR");
                    } else if (e.getMessage().contains("403")) {
                        apiCheck.put("errorType", "PERMISSION_ERROR");
                    } else if (e.getMessage().contains("timeout")) {
                        apiCheck.put("errorType", "TIMEOUT_ERROR");
                    } else {
                        apiCheck.put("errorType", "UNKNOWN_ERROR");
                    }
                }
            } else {
                apiCheck.put("apiTest", "❌ Service non disponible");
            }

        } catch (Exception e) {
            apiCheck.put("status", "❌ Erreur vérification API: " + e.getMessage());
        }

        return apiCheck;
    }

    /**
     * 📊 Vérification des données
     */
    private Map<String, Object> checkData() {
        Map<String, Object> dataCheck = new HashMap<>();

        try {
            // Vérifier les produits
            if (produitService != null) {
                var produits = produitService.getAllProduits();
                dataCheck.put("totalProduits", produits.size());

                // Rechercher "caftan" spécifiquement
                var caftanProduits = produits.stream()
                        .filter(p -> p.getNomProduit().toLowerCase().contains("caftan"))
                        .toList();
                dataCheck.put("caftanProduits", caftanProduits.size());

                if (!caftanProduits.isEmpty()) {
                    dataCheck.put("caftanExample", Map.of(
                            "nom", caftanProduits.get(0).getNomProduit(),
                            "prix", caftanProduits.get(0).getPrix(),
                            "description", caftanProduits.get(0).getDescription()
                    ));
                }

                // Test recherche normale
                var searchResults = produitService.searchProduits("caftan");
                dataCheck.put("searchNormaleResults", searchResults.size());

                // Test recherche intelligente
                var intelligentResults = produitService.searchProduitsIntelligent("caftan");
                dataCheck.put("searchIntelligentResults", intelligentResults.size());
            }

            // Vérifier les boutiques
            if (boutiqueService != null) {
                var boutiques = boutiqueService.getAllBoutiques();
                dataCheck.put("totalBoutiques", boutiques.size());

                var rabatBoutiques = boutiques.stream()
                        .filter(b -> "rabat".equalsIgnoreCase(b.getVille()))
                        .toList();
                dataCheck.put("rabatBoutiques", rabatBoutiques.size());
            }

            dataCheck.put("status", "✅ Données vérifiées");

        } catch (Exception e) {
            dataCheck.put("status", "❌ Erreur vérification données: " + e.getMessage());
        }

        return dataCheck;
    }

    /**
     * 🔗 Test de l'intégration complète
     */
    private Map<String, Object> testIntegration() {
        Map<String, Object> integrationTest = new HashMap<>();

        try {
            if (ragService == null) {
                integrationTest.put("status", "❌ RAGService non disponible");
                return integrationTest;
            }

            // Test avec une requête simple
            String testQuery = "caftan";

            long startTime = System.currentTimeMillis();
            var response = ragService.searchWithRAG(testQuery, "MIXED");
            long duration = System.currentTimeMillis() - startTime;

            integrationTest.put("testQuery", testQuery);
            integrationTest.put("duration", duration + "ms");
            integrationTest.put("ragEnhanced", response.isRagEnhanced());
            integrationTest.put("totalResults", response.getTotalResults());
            integrationTest.put("analysis", response.getAnalysis());
            integrationTest.put("summary", response.getSummary());

            if (response.isRagEnhanced()) {
                integrationTest.put("status", "✅ RAG fonctionnel");
            } else {
                integrationTest.put("status", "❌ RAG utilise le fallback");
                integrationTest.put("fallbackReason", "Vérifier les logs pour les exceptions");
            }

        } catch (Exception e) {
            integrationTest.put("status", "❌ Erreur test intégration: " + e.getMessage());
            integrationTest.put("stackTrace", e.getStackTrace()[0].toString());
        }

        return integrationTest;
    }

    /**
     * 💡 Génération de recommandations
     */
    private Map<String, Object> generateRecommendations(Map<String, Object> diagnosticResult) {
        Map<String, Object> recommendations = new HashMap<>();

        // Analyser les résultats du diagnostic
        var config = (Map<String, Object>) diagnosticResult.get("configuration");
        var services = (Map<String, Object>) diagnosticResult.get("services");
        var geminiApi = (Map<String, Object>) diagnosticResult.get("geminiApi");
        var data = (Map<String, Object>) diagnosticResult.get("data");

        // Recommandations basées sur la configuration
        if (!(Boolean) config.get("ragEnabled")) {
            recommendations.put("config_1", "Activer RAG avec: rag.enabled=true");
        }

        if (!(Boolean) config.get("geminiApiKeyPresent")) {
            recommendations.put("config_2", "Configurer la clé API Gemini dans application.properties");
        }

        // Recommandations basées sur les services
        if (services.get("ragService").toString().contains("❌")) {
            recommendations.put("service_1", "Vérifier l'annotation @Service sur RAGService");
        }

        if (services.get("geminiAIService").toString().contains("❌")) {
            recommendations.put("service_2", "Vérifier l'injection de GeminiAIService");
        }

        // Recommandations basées sur l'API
        if (geminiApi.get("apiTest") != null && geminiApi.get("apiTest").toString().contains("❌")) {
            recommendations.put("api_1", "Vérifier la connectivité Internet et la clé API");
        }

        // Recommandations basées sur les données
        if (data.get("caftanProduits") != null && (Integer) data.get("caftanProduits") == 0) {
            recommendations.put("data_1", "Ajouter des produits 'caftan' pour tester");
        }

        return recommendations;
    }

    /**
     * 🧪 Test spécifique de la recherche
     */
    @GetMapping("/test-search")
    public ResponseEntity<Map<String, Object>> testSearch(@RequestParam(defaultValue = "caftan") String query) {
        Map<String, Object> result = new HashMap<>();

        try {
            // Test direct des services
            if (produitService != null) {
                result.put("produitService_direct", produitService.searchProduits(query).size());
                result.put("produitService_intelligent", produitService.searchProduitsIntelligent(query).size());
            }

            // Test RAG
            if (ragService != null) {
                try {
                    var ragResult = ragService.searchWithRAG(query, "MIXED");
                    result.put("rag_result", Map.of(
                            "enhanced", ragResult.isRagEnhanced(),
                            "total", ragResult.getTotalResults(),
                            "analysis", ragResult.getAnalysis()
                    ));
                } catch (Exception e) {
                    result.put("rag_error", e.getMessage());
                    result.put("rag_stack", e.getStackTrace()[0].toString());
                }
            }

        } catch (Exception e) {
            result.put("error", e.getMessage());
        }

        return ResponseEntity.ok(result);
    }

    /**
     * 🔄 Réinitialisation du système RAG
     */
    @PostMapping("/reset")
    public ResponseEntity<Map<String, String>> resetRAG() {
        Map<String, String> result = new HashMap<>();

        try {
            if (ragService != null) {
                ragService.clearCache();
                result.put("status", "✅ Cache RAG vidé");
            } else {
                result.put("status", "❌ RAGService non disponible");
            }
        } catch (Exception e) {
            result.put("status", "❌ Erreur: " + e.getMessage());
        }

        return ResponseEntity.ok(result);
    }
}