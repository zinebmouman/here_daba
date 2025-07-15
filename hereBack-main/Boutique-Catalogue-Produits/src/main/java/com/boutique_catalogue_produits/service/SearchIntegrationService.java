package com.boutique_catalogue_produits.service;

import com.boutique_catalogue_produits.dto.ProduitDTO;
import com.boutique_catalogue_produits.dto.RagSearchResponse;
import com.boutique_catalogue_produits.dto.SearchCriteria;
import com.boutique_catalogue_produits.model.Boutique;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Service d'intégration pour orchestrer la recherche RAG
 * Coordonne tous les services de recherche et optimise les performances
 */
@Service
public class SearchIntegrationService {

    private static final Logger logger = LoggerFactory.getLogger(SearchIntegrationService.class);

    @Autowired
    private RAGService ragService;

    @Autowired
    private ProduitService produitService;

    @Autowired
    private BoutiqueService boutiqueService;

    @Autowired
    private GeminiAIService geminiAIService;

    // Configuration depuis application.properties
    @Value("${rag.search.max.results:50}")
    private int maxResults;

    @Value("${rag.similarity.threshold:0.3}")
    private double similarityThreshold;

    @Value("${rag.cache.enabled:true}")
    private boolean cacheEnabled;

    // Pool de threads pour les recherches parallèles
    private final ExecutorService executorService = Executors.newFixedThreadPool(5);

    // Métriques de performance
    private final Map<String, Long> performanceMetrics = new HashMap<>();
    private final Map<String, Integer> usageStats = new HashMap<>();

    // ================== MÉTHODES PRINCIPALES D'INTÉGRATION ==================

    /**
     * 🚀 RECHERCHE UNIFIÉE - Point d'entrée principal pour toute recherche
     */
    public RagSearchResponse rechercheUnifiee(String query, String searchType, Map<String, Object> options) {
        logger.info("🔍 [INTEGRATION] Recherche unifiée: '{}' (type: {})", query, searchType);

        long startTime = System.currentTimeMillis();

        try {
            // 1. VALIDATION ET PREPROCESSING
            String cleanQuery = preprocessQuery(query);
            searchType = validateSearchType(searchType);

            // 2. ANALYSE INTELLIGENTE DE LA REQUÊTE
            SearchCriteria criteria = analyzeQueryWithFallback(cleanQuery);

            // 3. RECHERCHE PARALLÈLE OPTIMISÉE
            RagSearchResponse response = performParallelSearch(cleanQuery, searchType, criteria, options);

            // 4. POST-PROCESSING ET ENRICHISSEMENT
            enrichResponse(response, criteria);

            // 5. MÉTRIQUES ET MONITORING
            long processingTime = System.currentTimeMillis() - startTime;
            updateMetrics(searchType, processingTime, response.getTotalResults());

            response.setProcessingTimeMs(processingTime);

            logger.info("✅ [INTEGRATION] Recherche terminée: {} résultats en {}ms",
                    response.getTotalResults(), processingTime);

            return response;

        } catch (Exception e) {
            logger.error("❌ [INTEGRATION] Erreur recherche unifiée: {}", e.getMessage(), e);
            return createErrorResponse(query, searchType, e);
        }
    }

    /**
     * 🎯 RECHERCHE CONTEXTUELLE - Adapte la recherche selon le contexte utilisateur
     */
    public RagSearchResponse rechercheContextuelle(String query, String userId, String sessionId, Map<String, Object> context) {
        logger.info("🎯 [INTEGRATION] Recherche contextuelle: '{}' (user: {})", query, userId);

        try {
            // 1. ENRICHIR AVEC LE CONTEXTE UTILISATEUR
            SearchCriteria baseCriteria = geminiAIService.analyzeSearchQuery(query);
            SearchCriteria enrichedCriteria = enrichWithUserContext(baseCriteria, userId, context);

            // 2. PERSONNALISER LA RECHERCHE
            String searchType = inferSearchTypeFromContext(enrichedCriteria, context);

            // 3. EXÉCUTER RECHERCHE PERSONNALISÉE
            RagSearchResponse response = ragService.searchWithRAG(query, searchType);

            // 4. FILTRER ET RÉORDONNER SELON LE CONTEXTE
            personalizeResults(response, userId, context);

            // 5. APPRENTISSAGE ET AMÉLIORATION
            learnFromUserInteraction(userId, query, response);

            return response;

        } catch (Exception e) {
            logger.error("❌ [INTEGRATION] Erreur recherche contextuelle: {}", e.getMessage());
            return rechercheUnifiee(query, "MIXED", Collections.emptyMap());
        }
    }

    /**
     * 🌍 RECHERCHE GÉOLOCALISÉE - Intègre la géolocalisation dans la recherche
     */
    @Cacheable(value = "geoSearchCache", key = "#query + '_' + #latitude + '_' + #longitude")
    public RagSearchResponse rechercheGeolocalisee(String query, double latitude, double longitude, int rayonKm) {
        logger.info("🌍 [INTEGRATION] Recherche géolocalisée: '{}' ({}, {}) {}km",
                query, latitude, longitude, rayonKm);

        try {
            // 1. DÉTECTER LA VILLE/RÉGION À PARTIR DES COORDONNÉES
            String ville = detectCityFromCoordinates(latitude, longitude);

            // 2. ANALYSER LA REQUÊTE AVEC CONTEXTE GÉOGRAPHIQUE
            SearchCriteria criteria = geminiAIService.analyzeSearchQuery(query);
            criteria.setVille(ville);
            criteria.setLatitude(latitude);
            criteria.setLongitude(longitude);
            criteria.setRayonKm(rayonKm);

            // 3. RECHERCHE AVEC PRIORITÉ GÉOGRAPHIQUE
            RagSearchResponse response = ragService.searchWithRAG(query, "MIXED");

            // 4. FILTRER ET TRIER PAR PROXIMITÉ
            filterAndSortByProximity(response, latitude, longitude, rayonKm);

            // 5. ENRICHIR AVEC INFORMATIONS GÉOGRAPHIQUES
            enrichWithGeoInfo(response, ville, rayonKm);

            return response;

        } catch (Exception e) {
            logger.error("❌ [INTEGRATION] Erreur recherche géolocalisée: {}", e.getMessage());
            return rechercheUnifiee(query, "MIXED", Collections.emptyMap());
        }
    }

    /**
     * ⚡ RECHERCHE RAPIDE - Version optimisée pour les recherches fréquentes
     */
    @Cacheable(value = "fastSearchCache", key = "#query + '_' + #searchType")
    public RagSearchResponse rechercheRapide(String query, String searchType) {
        logger.info("⚡ [INTEGRATION] Recherche rapide: '{}' ({})", query, searchType);

        try {
            // 1. VÉRIFIER LE CACHE INTELLIGENT
            RagSearchResponse cachedResult = getCachedResultIfValid(query, searchType);
            if (cachedResult != null) {
                logger.info("💾 [INTEGRATION] Résultat depuis cache rapide");
                return cachedResult;
            }

            // 2. RECHERCHE OPTIMISÉE SANS RAG COMPLET
            if ("PRODUIT".equals(searchType)) {
                return performFastProductSearch(query);
            } else if ("BOUTIQUE".equals(searchType)) {
                return performFastBoutiqueSearch(query);
            } else {
                return performFastMixedSearch(query);
            }

        } catch (Exception e) {
            logger.error("❌ [INTEGRATION] Erreur recherche rapide: {}", e.getMessage());
            return createEmptyResponse(query, searchType);
        }
    }

    // ================== MÉTHODES PRIVÉES D'ORCHESTRATION ==================

    private RagSearchResponse performParallelSearch(String query, String searchType,
                                                    SearchCriteria criteria, Map<String, Object> options) {

        List<CompletableFuture<Void>> futures = new ArrayList<>();
        RagSearchResponse response = new RagSearchResponse(query, searchType);

        // Recherche produits en parallèle
        if ("PRODUIT".equals(searchType) || "MIXED".equals(searchType)) {
            CompletableFuture<Void> productFuture = CompletableFuture.runAsync(() -> {
                try {
                    List<ProduitDTO> produits = produitService.searchProduitsIntelligent(query);
                    synchronized (response) {
                        response.setProduits(produits);
                        response.setNombreProduits(produits.size());
                    }
                } catch (Exception e) {
                    logger.warn("⚠️ [INTEGRATION] Erreur recherche produits parallèle: {}", e.getMessage());
                }
            }, executorService);
            futures.add(productFuture);
        }

        // Recherche boutiques en parallèle
        if ("BOUTIQUE".equals(searchType) || "MIXED".equals(searchType)) {
            CompletableFuture<Void> boutiqueFuture = CompletableFuture.runAsync(() -> {
                try {
                    List<Boutique> boutiques = boutiqueService.searchBoutiquesIntelligent(query);
                    synchronized (response) {
                        response.setBoutiques(boutiques);
                        response.setNombreBoutiques(boutiques.size());
                    }
                } catch (Exception e) {
                    logger.warn("⚠️ [INTEGRATION] Erreur recherche boutiques parallèle: {}", e.getMessage());
                }
            }, executorService);
            futures.add(boutiqueFuture);
        }

        // Analyse RAG en parallèle
        CompletableFuture<Void> ragFuture = CompletableFuture.runAsync(() -> {
            try {
                RagSearchResponse ragResponse = ragService.searchWithRAG(query, searchType);
                synchronized (response) {
                    response.setAnalysis(ragResponse.getAnalysis());
                    response.setSummary(ragResponse.getSummary());
                    response.setAlternatives(ragResponse.getAlternatives());
                    response.setTips(ragResponse.getTips());
                    response.setRagEnhanced(true);
                }
            } catch (Exception e) {
                logger.warn("⚠️ [INTEGRATION] Erreur analyse RAG parallèle: {}", e.getMessage());
                response.setRagEnhanced(false);
            }
        }, executorService);
        futures.add(ragFuture);

        // Attendre la completion de toutes les tâches
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // Calculer totaux
        response.setTotalResults(response.getNombreProduits() + response.getNombreBoutiques());

        return response;
    }

    private SearchCriteria analyzeQueryWithFallback(String query) {
        try {
            return geminiAIService.analyzeSearchQuery(query);
        } catch (Exception e) {
            logger.warn("⚠️ [INTEGRATION] Analyse Gemini échouée, fallback: {}", e.getMessage());

            // Créer des critères basiques
            SearchCriteria fallback = new SearchCriteria();
            fallback.setKeywords(query);
            fallback.setSearchType("MIXED");
            return fallback;
        }
    }

    private String preprocessQuery(String query) {
        if (query == null) return "";

        // Nettoyage et normalisation
        String cleaned = query.trim()
                .replaceAll("\\s+", " ")  // Espaces multiples
                .replaceAll("[^\\w\\s\\u00C0-\\u017F]", " ")  // Caractères spéciaux sauf accents
                .toLowerCase();

        // Correction automatique des fautes courantes
        Map<String, String> corrections = Map.of(
                "telefon", "téléphone",
                "samsugn", "samsung",
                "ordinater", "ordinateur",
                "chausure", "chaussure",
                "voituer", "voiture"
        );

        for (Map.Entry<String, String> correction : corrections.entrySet()) {
            cleaned = cleaned.replace(correction.getKey(), correction.getValue());
        }

        return cleaned;
    }

    private String validateSearchType(String searchType) {
        if (searchType == null) return "MIXED";

        switch (searchType.toUpperCase()) {
            case "PRODUIT":
            case "BOUTIQUE":
            case "MIXED":
                return searchType.toUpperCase();
            default:
                return "MIXED";
        }
    }

    private void enrichResponse(RagSearchResponse response, SearchCriteria criteria) {
        // Ajouter des métadonnées enrichies
        if (response.getAnalysis() == null) {
            response.setAnalysis("Recherche effectuée avec succès");
        }

        if (response.getSummary() == null) {
            response.setSummary(String.format(
                    "%d résultat(s) trouvé(s) pour votre recherche",
                    response.getTotalResults()
            ));
        }

        // Ajouter des suggestions si peu de résultats
        if (response.getTotalResults() < 3) {
            List<String> suggestions = generateSearchSuggestions(criteria);
            response.setAlternatives(suggestions);
        }

        // Ajouter des conseils personnalisés
        List<String> tips = generatePersonalizedTips(response, criteria);
        response.setTips(tips);
    }

    private List<String> generateSearchSuggestions(SearchCriteria criteria) {
        List<String> suggestions = new ArrayList<>();

        if (criteria.getKeywords() != null) {
            String[] words = criteria.getKeywords().split("\\s+");
            for (String word : words) {
                if (word.length() > 3) {
                    suggestions.add("Essayez: " + word + " pas cher");
                    suggestions.add("Rechercher: " + word + " haute qualité");
                }
            }
        }

        if (criteria.getCategorie() != null) {
            suggestions.add("Explorez la catégorie: " + criteria.getCategorie());
        }

        return suggestions.stream().limit(3).collect(Collectors.toList());
    }

    private List<String> generatePersonalizedTips(RagSearchResponse response, SearchCriteria criteria) {
        List<String> tips = new ArrayList<>();

        if (response.getNombreProduits() > 10) {
            tips.add("Utilisez des filtres de prix pour affiner votre recherche");
        }

        if (response.getNombreBoutiques() > 5) {
            tips.add("Vérifiez les avis et notes des boutiques avant d'acheter");
        }

        if (criteria.getVille() == null) {
            tips.add("Spécifiez votre ville pour trouver des boutiques près de chez vous");
        }

        return tips;
    }

    // ================== MÉTHODES DE RECHERCHE RAPIDE ==================

    private RagSearchResponse performFastProductSearch(String query) {
        List<ProduitDTO> produits = produitService.searchProduits(query);

        RagSearchResponse response = new RagSearchResponse(query, "PRODUIT");
        response.setProduits(produits);
        response.setNombreProduits(produits.size());
        response.setTotalResults(produits.size());
        response.setRagEnhanced(false);
        response.setAnalysis("Recherche rapide effectuée");

        return response;
    }

    private RagSearchResponse performFastBoutiqueSearch(String query) {
        List<Boutique> boutiques = boutiqueService.searchBoutiquesClassique(query);

        RagSearchResponse response = new RagSearchResponse(query, "BOUTIQUE");
        response.setBoutiques(boutiques);
        response.setNombreBoutiques(boutiques.size());
        response.setTotalResults(boutiques.size());
        response.setRagEnhanced(false);
        response.setAnalysis("Recherche rapide effectuée");

        return response;
    }

    private RagSearchResponse performFastMixedSearch(String query) {
        List<ProduitDTO> produits = produitService.searchProduits(query).stream()
                .limit(10)
                .collect(Collectors.toList());

        List<Boutique> boutiques = boutiqueService.searchBoutiquesClassique(query).stream()
                .limit(5)
                .collect(Collectors.toList());

        RagSearchResponse response = new RagSearchResponse(query, "MIXED");
        response.setProduits(produits);
        response.setBoutiques(boutiques);
        response.setNombreProduits(produits.size());
        response.setNombreBoutiques(boutiques.size());
        response.setTotalResults(produits.size() + boutiques.size());
        response.setRagEnhanced(false);
        response.setAnalysis("Recherche rapide mixte effectuée");

        return response;
    }

    // ================== MÉTHODES UTILITAIRES ==================

    private SearchCriteria enrichWithUserContext(SearchCriteria criteria, String userId, Map<String, Object> context) {
        // Enrichir avec l'historique utilisateur, préférences, etc.
        // Cette logique dépend de votre système de gestion utilisateur

        if (context.containsKey("preferredCity")) {
            criteria.setVille((String) context.get("preferredCity"));
        }

        if (context.containsKey("budgetRange")) {
            Map<String, Double> budget = (Map<String, Double>) context.get("budgetRange");
            criteria.setPrixMin(budget.get("min"));
            criteria.setPrixMax(budget.get("max"));
        }

        return criteria;
    }

    private String inferSearchTypeFromContext(SearchCriteria criteria, Map<String, Object> context) {
        // Logique pour inférer le type de recherche selon le contexte
        if (context.containsKey("searchHistory")) {
            // Analyser l'historique pour déduire les préférences
        }

        return criteria.getSearchType() != null ? criteria.getSearchType() : "MIXED";
    }

    private void personalizeResults(RagSearchResponse response, String userId, Map<String, Object> context) {
        // Personnaliser l'ordre des résultats selon les préférences utilisateur
        // Implémenter selon votre logique de personnalisation
    }

    private void learnFromUserInteraction(String userId, String query, RagSearchResponse response) {
        // Enregistrer les interactions pour améliorer les futurs résultats
        // Machine learning et amélioration continue
    }

    private String detectCityFromCoordinates(double latitude, double longitude) {
        // Logique pour détecter la ville à partir des coordonnées
        // Vous pouvez utiliser une API de géocodage ou une base de données locale

        // Pour l'exemple, retourner Casablanca par défaut
        return "Casablanca";
    }

    private void filterAndSortByProximity(RagSearchResponse response, double latitude, double longitude, int rayonKm) {
        // Filtrer et trier les boutiques par proximité géographique
        // Nécessite l'ajout de coordonnées GPS dans le modèle Boutique
    }

    private void enrichWithGeoInfo(RagSearchResponse response, String ville, int rayonKm) {
        response.setVille(ville);
        response.setRayonKm(rayonKm);

        if (response.getAnalysis() != null) {
            response.setAnalysis(response.getAnalysis() +
                    String.format(" (Recherche dans un rayon de %d km autour de %s)", rayonKm, ville));
        }
    }

    private void updateMetrics(String searchType, long processingTime, int resultCount) {
        String key = "search_" + searchType.toLowerCase();
        performanceMetrics.put(key + "_time", processingTime);
        performanceMetrics.put(key + "_results", (long) resultCount);

        usageStats.merge(key + "_count", 1, Integer::sum);

        logger.debug("📊 [INTEGRATION] Métriques mises à jour: {} ({}ms, {} résultats)",
                searchType, processingTime, resultCount);
    }

    private RagSearchResponse getCachedResultIfValid(String query, String searchType) {
        // Implémentation du cache intelligent
        // Retourner null si pas de cache valide
        return null;
    }

    private RagSearchResponse createErrorResponse(String query, String searchType, Exception e) {
        RagSearchResponse response = new RagSearchResponse(query, searchType);
        response.setRagEnhanced(false);
        response.setAnalysis("Erreur lors de la recherche: " + e.getMessage());
        response.setSummary("Aucun résultat disponible");
        response.setProduits(new ArrayList<>());
        response.setBoutiques(new ArrayList<>());
        response.setTotalResults(0);
        return response;
    }

    private RagSearchResponse createEmptyResponse(String query, String searchType) {
        RagSearchResponse response = new RagSearchResponse(query, searchType);
        response.setRagEnhanced(false);
        response.setAnalysis("Recherche effectuée");
        response.setSummary("Aucun résultat trouvé");
        response.setProduits(new ArrayList<>());
        response.setBoutiques(new ArrayList<>());
        response.setTotalResults(0);
        return response;
    }

    // ================== MÉTHODES DE MONITORING ==================

    public Map<String, Object> getPerformanceMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("performance", performanceMetrics);
        metrics.put("usage", usageStats);
        metrics.put("timestamp", System.currentTimeMillis());
        return metrics;
    }

    public void resetMetrics() {
        performanceMetrics.clear();
        usageStats.clear();
        logger.info("📊 [INTEGRATION] Métriques réinitialisées");
    }
}