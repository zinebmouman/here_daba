package com.boutique_catalogue_produits.service;

import com.boutique_catalogue_produits.dto.*;
import com.boutique_catalogue_produits.model.Boutique;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 🚀 RAGService CORRIGÉ - 100% IA Scalable
 * Version corrigée avec parseRecommendations qui retourne List<RecommendationItem>
 */
@Service
@ConditionalOnProperty(name = "rag.enabled", havingValue = "true", matchIfMissing = false)
public class RAGService {

    private static final Logger logger = LoggerFactory.getLogger(RAGService.class);

    @Autowired
    private ScalableAISearchService aiSearchService;

    @Autowired(required = false)
    private GeminiAIService geminiAIService;

    @Autowired
    private ProduitService produitService;

    @Autowired
    private BoutiqueService boutiqueService;

    @Value("${rag.enabled:true}")
    private boolean ragEnabled;

    @Value("${rag.cache.enabled:true}")
    private boolean cacheEnabled;

    @Value("${rag.cache.duration.minutes:15}")
    private int cacheDurationMinutes;

    // Cache intelligent avec TTL
    private final Map<String, CachedSearchResponse> responseCache = new ConcurrentHashMap<>();
    private boolean systemHealthy = false;
    private String lastErrorMessage = "";

    @PostConstruct
    public void initializeRAGService() {
        logger.info("🚀 [RAG] Initialisation du service RAG 100% IA");

        try {
            checkDependencies();
            systemHealthy = true;
            logger.info("✅ [RAG] Service RAG IA initialisé avec succès");
        } catch (Exception e) {
            logger.error("❌ [RAG] Erreur initialisation: {}", e.getMessage());
            systemHealthy = false;
            lastErrorMessage = e.getMessage();
        }
    }

    private void checkDependencies() throws Exception {
        if (produitService == null) throw new IllegalStateException("ProduitService manquant");
        if (boutiqueService == null) throw new IllegalStateException("BoutiqueService manquant");
        if (aiSearchService == null) throw new IllegalStateException("ScalableAISearchService manquant");
    }

    /**
     * 🧠 Recherche RAG SCALABLE avec IA pure
     */
    public RagSearchResponse searchWithRAG(String query, String searchType) {
        logger.info("🧠 [RAG-AI] Recherche intelligente: '{}' (type: {})", query, searchType);

        long startTime = System.currentTimeMillis();

        try {
            // Validation
            if (query == null || query.trim().isEmpty()) {
                throw new IllegalArgumentException("Requête vide");
            }

            // Vérifier le cache
            String cacheKey = query + "|" + searchType;
            if (cacheEnabled && isCacheValid(cacheKey)) {
                logger.info("📦 [CACHE] Résultat trouvé en cache");
                return responseCache.get(cacheKey).response;
            }

            // 1. RECHERCHE AVEC IA PURE (sans mappings manuels)
            RagSearchResponse response = performAIOnlySearch(query, searchType);

            // 2. ENRICHISSEMENT AVEC L'IA
            enrichResponseWithGeminiAI(response, query);

            // 3. POST-TRAITEMENT
            finalizeResponse(response, startTime);

            // 4. MISE EN CACHE
            cacheResponse(cacheKey, response);

            logger.info("✅ [RAG-AI] {} résultats trouvés en {}ms",
                    response.getTotalResults(), response.getProcessingTimeMs());

            return response;

        } catch (Exception e) {
            logger.error("❌ [RAG-AI] Erreur: {}", e.getMessage());
            return createIntelligentFallback(query, searchType);
        }
    }

    /**
     * 🎯 Recherche pure IA sans mappings manuels
     */
    private RagSearchResponse performAIOnlySearch(String query, String searchType) throws Exception {
        RagSearchResponse response = new RagSearchResponse();
        response.setQuery(query);
        response.setSearchType(searchType);
        response.setRagEnhanced(true);
        response.setTimestamp(new Date());

        // RECHERCHE PRODUITS avec IA
        if ("PRODUIT".equals(searchType) || "MIXED".equals(searchType)) {
            logger.info("🛍️ [AI] Recherche produits IA...");

            List<ProduitDTO> allProduits = produitService.getAllProduits();
            logger.info("📦 [DATA] {} produits à analyser", allProduits.size());

            // Utiliser l'IA pour trouver les produits pertinents
            List<ProduitDTO> aiResults = aiSearchService.searchProductsWithAI(query, allProduits);

            response.setProduits(aiResults);
            response.setNombreProduits(aiResults.size());

            logger.info("✅ [AI-PRODUITS] {} produits trouvés par l'IA", aiResults.size());
        } else {
            response.setProduits(new ArrayList<>());
            response.setNombreProduits(0);
        }

        // RECHERCHE BOUTIQUES avec IA
        if ("BOUTIQUE".equals(searchType) || "MIXED".equals(searchType)) {
            logger.info("🏪 [AI] Recherche boutiques IA...");

            List<Boutique> allBoutiques = boutiqueService.getAllBoutiques();
            logger.info("🏢 [DATA] {} boutiques à analyser", allBoutiques.size());

            // Utiliser l'IA pour trouver les boutiques pertinentes
            List<Boutique> aiResults = aiSearchService.searchBoutiquesWithAI(query, allBoutiques);

            response.setBoutiques(aiResults);
            response.setNombreBoutiques(aiResults.size());

            logger.info("✅ [AI-BOUTIQUES] {} boutiques trouvées par l'IA", aiResults.size());
        } else {
            response.setBoutiques(new ArrayList<>());
            response.setNombreBoutiques(0);
        }

        // Calcul du total
        response.setTotalResults(response.getNombreProduits() + response.getNombreBoutiques());

        return response;
    }

    /**
     * 🤖 Enrichissement avec Gemini AI
     */
    private void enrichResponseWithGeminiAI(RagSearchResponse response, String query) {
        try {
            if (geminiAIService != null && response.getTotalResults() > 0) {
                logger.info("🧠 [GEMINI] Génération d'analyse IA...");

                // Créer un contexte pour l'IA
                String context = buildContextForAI(response);

                // Demander à Gemini d'analyser les résultats
                String aiAnalysis = geminiAIService.generateAdvancedAnalysis(query, context);
                response.setAnalysis(aiAnalysis);

                // Générer des recommandations CORRIGÉES
                String recommendations = geminiAIService.generateRecommendations(
                        query, response.getProduits(), response.getBoutiques());
                response.setRecommendations(parseRecommendationsToItems(recommendations, response, query));

                logger.info("✅ [GEMINI] Analyse IA générée avec {} recommandations", response.getRecommendations().size());

            } else {
                setFallbackAnalysis(response, query);
                response.setRecommendations(new ArrayList<>());
            }
        } catch (Exception e) {
            logger.warn("⚠️ [GEMINI] Erreur enrichissement IA: {}", e.getMessage());
            setFallbackAnalysis(response, query);
            response.setRecommendations(createFallbackRecommendations(response, query));
        }
    }

    /**
     * 💡 MÉTHODE CORRIGÉE - Parse les recommandations en RecommendationItem
     */
    private List<RecommendationItem> parseRecommendationsToItems(String recommendations, RagSearchResponse response, String query) {
        logger.info("💡 [RECOMMENDATIONS] Parsing des recommandations IA");

        List<RecommendationItem> items = new ArrayList<>();

        try {
            if (recommendations == null || recommendations.trim().isEmpty()) {
                return createFallbackRecommendations(response, query);
            }

            // Parser les recommandations de l'IA
            String[] lines = recommendations.split("\n");
            int itemId = 1;

            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty() || line.length() < 5) continue;

                RecommendationItem item = parseRecommendationLine(line, itemId++, query);
                if (item != null) {
                    items.add(item);
                }
            }

            // Si pas assez de recommandations, ajouter des recommandations basées sur les résultats
            if (items.size() < 3) {
                items.addAll(generateRecommendationsFromResults(response, query, items.size()));
            }

            // Limiter à 10 recommandations max
            if (items.size() > 10) {
                items = items.subList(0, 10);
            }

            logger.info("✅ [RECOMMENDATIONS] {} recommandations générées", items.size());

        } catch (Exception e) {
            logger.error("❌ [RECOMMENDATIONS] Erreur parsing: {}", e.getMessage());
            return createFallbackRecommendations(response, query);
        }

        return items;
    }

    /**
     * 🔍 Parser une ligne de recommandation
     */
    private RecommendationItem parseRecommendationLine(String line, int itemId, String query) {
        try {
            // Nettoyer la ligne
            line = line.replaceAll("^[0-9]+[.)\\s]+", ""); // Enlever numérotation
            line = line.replaceAll("^[-•*]\\s*", ""); // Enlever puces

            if (line.length() < 10) return null;

            RecommendationItem item = new RecommendationItem();
            item.setId("rec_" + itemId);

            // Détecter le type de recommandation
            if (line.toLowerCase().contains("produit") || line.toLowerCase().contains("article")) {
                item.setType("PRODUIT");
                item.setCategory("produit");
            } else if (line.toLowerCase().contains("boutique") || line.toLowerCase().contains("magasin")) {
                item.setType("BOUTIQUE");
                item.setCategory("boutique");
            } else {
                item.setType("CONSEIL");
                item.setCategory("conseil");
            }

            // Extraire titre et description
            if (line.contains(":")) {
                String[] parts = line.split(":", 2);
                item.setTitle(parts[0].trim());
                item.setDescription(parts.length > 1 ? parts[1].trim() : "");
            } else {
                // Si pas de ':', prendre les premiers 50 caractères comme titre
                if (line.length() > 50) {
                    item.setTitle(line.substring(0, 50) + "...");
                    item.setDescription(line);
                } else {
                    item.setTitle(line);
                    item.setDescription("Recommandation basée sur votre recherche: " + query);
                }
            }

            // Score basé sur la position et le contenu
            double score = Math.max(0.5, 1.0 - (itemId * 0.1));
            item.setScore(score);

            // Raison de la recommandation
            item.setReason("Recommandé par l'IA basé sur votre recherche");

            return item;

        } catch (Exception e) {
            logger.warn("⚠️ [RECOMMENDATION] Erreur parsing ligne: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 🎯 Générer des recommandations à partir des résultats de recherche
     */
    private List<RecommendationItem> generateRecommendationsFromResults(RagSearchResponse response, String query, int startingId) {
        List<RecommendationItem> items = new ArrayList<>();

        try {
            // Recommandations basées sur les produits trouvés
            if (!response.getProduits().isEmpty()) {
                for (int i = 0; i < Math.min(3, response.getProduits().size()); i++) {
                    ProduitDTO produit = response.getProduits().get(i);

                    RecommendationItem item = new RecommendationItem();
                    item.setId("rec_prod_" + (startingId + i + 1));
                    item.setType("PRODUIT");
                    item.setTitle("Produit recommandé: " + produit.getNomProduit());
                    item.setDescription(String.format("Prix: %.2f DH - %s",
                            produit.getPrix(),
                            produit.getDescription() != null ? produit.getDescription() : "Produit de qualité"));
                    item.setScore(0.8 - (i * 0.1));
                    item.setCategory("produit");
                    item.setReason("Correspond à votre recherche: " + query);

                    items.add(item);
                }
            }

            // Recommandations basées sur les boutiques trouvées
            if (!response.getBoutiques().isEmpty()) {
                for (int i = 0; i < Math.min(2, response.getBoutiques().size()); i++) {
                    Boutique boutique = response.getBoutiques().get(i);

                    RecommendationItem item = new RecommendationItem();
                    item.setId("rec_boutique_" + (startingId + items.size() + i + 1));
                    item.setType("BOUTIQUE");
                    item.setTitle("Boutique recommandée: " + boutique.getNom());
                    item.setDescription("Localisation: " + boutique.getVille() +
                            (boutique.getAdress() != null ? " - " + boutique.getAdress() : ""));
                    item.setScore(0.7 - (i * 0.1));
                    item.setCategory("boutique");
                    item.setReason("Boutique pertinente pour votre recherche");

                    items.add(item);
                }
            }

        } catch (Exception e) {
            logger.error("❌ [RECOMMENDATIONS] Erreur génération depuis résultats: {}", e.getMessage());
        }

        return items;
    }

    /**
     * 🔄 Créer des recommandations de fallback
     */
    private List<RecommendationItem> createFallbackRecommendations(RagSearchResponse response, String query) {
        List<RecommendationItem> fallbackItems = new ArrayList<>();

        try {
            // Recommandation générale 1
            RecommendationItem item1 = new RecommendationItem();
            item1.setId("fallback_1");
            item1.setType("CONSEIL");
            item1.setTitle("Affiner votre recherche");
            item1.setDescription("Essayez des mots-clés plus spécifiques ou différents pour de meilleurs résultats");
            item1.setScore(0.6);
            item1.setCategory("conseil");
            item1.setReason("Conseil automatique");
            fallbackItems.add(item1);

            // Recommandation générale 2
            RecommendationItem item2 = new RecommendationItem();
            item2.setId("fallback_2");
            item2.setType("CONSEIL");
            item2.setTitle("Explorer les catégories");
            item2.setDescription("Parcourez nos différentes catégories pour découvrir plus de produits");
            item2.setScore(0.5);
            item2.setCategory("conseil");
            item2.setReason("Suggestion de navigation");
            fallbackItems.add(item2);

            // Si des résultats existent, utiliser la méthode de génération basée sur les résultats
            if (response.getTotalResults() > 0) {
                fallbackItems.addAll(generateRecommendationsFromResults(response, query, 2));
            }

        } catch (Exception e) {
            logger.error("❌ [FALLBACK-RECOMMENDATIONS] Erreur: {}", e.getMessage());
        }

        return fallbackItems;
    }

    /**
     * 📝 Construction du contexte pour l'IA
     */
    private String buildContextForAI(RagSearchResponse response) {
        StringBuilder context = new StringBuilder();

        context.append("RÉSULTATS DE RECHERCHE:\n");
        context.append("Total: ").append(response.getTotalResults()).append(" résultats\n\n");

        if (!response.getProduits().isEmpty()) {
            context.append("PRODUITS TROUVÉS:\n");
            response.getProduits().stream()
                    .limit(5) // Limiter pour ne pas dépasser les tokens
                    .forEach(p -> context.append("- ")
                            .append(p.getNomProduit())
                            .append(" (").append(p.getPrix()).append(" DH)\n"));
        }

        if (!response.getBoutiques().isEmpty()) {
            context.append("\nBOUTIQUES TROUVÉES:\n");
            response.getBoutiques().stream()
                    .limit(3)
                    .forEach(b -> context.append("- ")
                            .append(b.getNom())
                            .append(" (").append(b.getVille()).append(")\n"));
        }

        return context.toString();
    }

    /**
     * 🎯 Finalisation de la réponse
     */
    private void finalizeResponse(RagSearchResponse response, long startTime) {
        // Temps de traitement
        response.setProcessingTimeMs(System.currentTimeMillis() - startTime);

        // Score de confiance basé sur les résultats
        double confidence = calculateConfidenceScore(response);
        response.setConfidenceScore(confidence);

        // Résumé intelligent
        String summary = generateSmartSummary(response);
        response.setSummary(summary);

        // Validation finale
        if (response.getAnalysis() == null || response.getAnalysis().trim().isEmpty()) {
            setFallbackAnalysis(response, response.getQuery());
        }

        // Validation des recommandations
        if (response.getRecommendations() == null) {
            response.setRecommendations(new ArrayList<>());
        }
    }

    /**
     * 📊 Calcul du score de confiance
     */
    private double calculateConfidenceScore(RagSearchResponse response) {
        if (response.getTotalResults() == 0) return 0.0;
        if (response.getTotalResults() >= 10) return 0.9;
        if (response.getTotalResults() >= 5) return 0.8;
        if (response.getTotalResults() >= 2) return 0.6;
        return 0.4;
    }

    /**
     * 📋 Génération de résumé intelligent
     */
    private String generateSmartSummary(RagSearchResponse response) {
        if (response.getTotalResults() == 0) {
            return "Aucun résultat trouvé - essayez des termes différents";
        }

        StringBuilder summary = new StringBuilder();
        summary.append(response.getTotalResults()).append(" résultats trouvés");

        if (response.getNombreProduits() > 0 && response.getNombreBoutiques() > 0) {
            summary.append(" (").append(response.getNombreProduits())
                    .append(" produits, ").append(response.getNombreBoutiques())
                    .append(" boutiques)");
        } else if (response.getNombreProduits() > 0) {
            summary.append(" (").append(response.getNombreProduits()).append(" produits)");
        } else if (response.getNombreBoutiques() > 0) {
            summary.append(" (").append(response.getNombreBoutiques()).append(" boutiques)");
        }

        return summary.toString();
    }

    private void setFallbackAnalysis(RagSearchResponse response, String query) {
        if (response.getTotalResults() > 0) {
            response.setAnalysis(String.format(
                    "Recherche IA réussie pour '%s'. %d résultats pertinents trouvés grâce à l'analyse automatique.",
                    query, response.getTotalResults()));
        } else {
            response.setAnalysis(String.format(
                    "Aucun résultat trouvé pour '%s'. L'IA n'a pas pu identifier de produits ou boutiques correspondants. " +
                            "Essayez des termes plus généraux ou différents.", query));
        }
    }

    /**
     * 🔄 Fallback intelligent en cas d'erreur
     */
    private RagSearchResponse createIntelligentFallback(String query, String searchType) {
        logger.info("🔄 [FALLBACK] Création fallback pour: '{}'", query);

        RagSearchResponse response = new RagSearchResponse();
        response.setQuery(query);
        response.setSearchType(searchType);
        response.setRagEnhanced(false);
        response.setTimestamp(new Date());

        try {
            // Même en fallback, essayer une recherche basique
            if ("PRODUIT".equals(searchType) || "MIXED".equals(searchType)) {
                List<ProduitDTO> basicResults = performBasicProductSearch(query);
                response.setProduits(basicResults);
                response.setNombreProduits(basicResults.size());
            } else {
                response.setProduits(new ArrayList<>());
                response.setNombreProduits(0);
            }

            if ("BOUTIQUE".equals(searchType) || "MIXED".equals(searchType)) {
                List<Boutique> basicResults = performBasicBoutiqueSearch(query);
                response.setBoutiques(basicResults);
                response.setNombreBoutiques(basicResults.size());
            } else {
                response.setBoutiques(new ArrayList<>());
                response.setNombreBoutiques(0);
            }

            response.setTotalResults(response.getNombreProduits() + response.getNombreBoutiques());

        } catch (Exception e) {
            logger.error("❌ [FALLBACK] Erreur complète: {}", e.getMessage());
            response.setTotalResults(0);
            response.setProduits(new ArrayList<>());
            response.setBoutiques(new ArrayList<>());
            response.setNombreProduits(0);
            response.setNombreBoutiques(0);
        }

        setFallbackAnalysis(response, query);
        response.setSummary(generateSmartSummary(response));
        response.setConfidenceScore(calculateConfidenceScore(response));
        response.setRecommendations(createFallbackRecommendations(response, query));

        return response;
    }

    /**
     * 🔍 Recherche basique de produits (fallback)
     */
    private List<ProduitDTO> performBasicProductSearch(String query) {
        try {
            String lowerQuery = query.toLowerCase();
            return produitService.getAllProduits().stream()
                    .filter(p -> p.getNomProduit() != null &&
                            p.getNomProduit().toLowerCase().contains(lowerQuery))
                    .limit(20)
                    .toList();
        } catch (Exception e) {
            logger.error("❌ [BASIC-SEARCH] Erreur recherche produits: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 🏪 Recherche basique de boutiques (fallback)
     */
    private List<Boutique> performBasicBoutiqueSearch(String query) {
        try {
            String lowerQuery = query.toLowerCase();
            return boutiqueService.getAllBoutiques().stream()
                    .filter(Boutique::isValid)
                    .filter(b -> (b.getNom() != null && b.getNom().toLowerCase().contains(lowerQuery)) ||
                            (b.getVille() != null && b.getVille().toLowerCase().contains(lowerQuery)))
                    .limit(10)
                    .toList();
        } catch (Exception e) {
            logger.error("❌ [BASIC-SEARCH] Erreur recherche boutiques: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    // Méthodes de cache
    private boolean isCacheValid(String cacheKey) {
        CachedSearchResponse cached = responseCache.get(cacheKey);
        if (cached == null) return false;

        long age = System.currentTimeMillis() - cached.timestamp;
        return age < (cacheDurationMinutes * 60 * 1000L);
    }

    private void cacheResponse(String cacheKey, RagSearchResponse response) {
        if (cacheEnabled) {
            responseCache.put(cacheKey, new CachedSearchResponse(response, System.currentTimeMillis()));

            // Nettoyage automatique du cache (garder seulement les 100 dernières)
            if (responseCache.size() > 100) {
                cleanOldCacheEntries();
            }
        }
    }

    private void cleanOldCacheEntries() {
        long cutoff = System.currentTimeMillis() - (cacheDurationMinutes * 60 * 1000L);
        responseCache.entrySet().removeIf(entry -> entry.getValue().timestamp < cutoff);
    }

    // Classes utilitaires
    private static class CachedSearchResponse {
        final RagSearchResponse response;
        final long timestamp;

        CachedSearchResponse(RagSearchResponse response, long timestamp) {
            this.response = response;
            this.timestamp = timestamp;
        }
    }

    // Méthodes de service public
    public void clearCache() {
        responseCache.clear();
        aiSearchService.clearCache();
        logger.info("🗑️ [CACHE] Tous les caches vidés");
    }

    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("systemHealthy", systemHealthy);
        stats.put("lastError", lastErrorMessage);
        stats.put("ragEnabled", ragEnabled);
        stats.put("cacheEnabled", cacheEnabled);
        stats.put("responseCacheSize", responseCache.size());
        stats.put("aiSearchStats", aiSearchService.getStatistics());
        return stats;
    }

    /**
     * 💡 MÉTHODE CORRIGÉE - enrichWithRecommendations
     */
    public void enrichWithRecommendations(RagSearchResponse response, RagSearchRequest request) {
        try {
            if (geminiAIService != null) {
                String recommendations = geminiAIService.generateRecommendations(
                        request.getQuery(), response.getProduits(), response.getBoutiques());

                // Utiliser la méthode corrigée qui retourne List<RecommendationItem>
                List<RecommendationItem> recommendationItems = parseRecommendationsToItems(
                        recommendations, response, request.getQuery());
                response.setRecommendations(recommendationItems);

                logger.info("✅ [RECOMMENDATIONS] {} recommandations enrichies", recommendationItems.size());
            } else {
                response.setRecommendations(createFallbackRecommendations(response, request.getQuery()));
            }
        } catch (Exception e) {
            logger.warn("⚠️ [RECOMMENDATIONS] Erreur enrichissement: {}", e.getMessage());
            response.setRecommendations(createFallbackRecommendations(response, request.getQuery()));
        }
    }
}