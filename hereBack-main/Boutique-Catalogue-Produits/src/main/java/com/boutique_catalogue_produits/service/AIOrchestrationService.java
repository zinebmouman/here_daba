package com.boutique_catalogue_produits.service;

import com.boutique_catalogue_produits.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 🎯 Service d'orchestration principal pour tous les services d'IA
 * Coordonne et optimise l'utilisation de tous les modules d'intelligence artificielle
 */
@Service
public class AIOrchestrationService {

    private static final Logger logger = LoggerFactory.getLogger(AIOrchestrationService.class);

    @Autowired
    private ImageSearchService imageSearchService;

    @Autowired
    private RecommendationService recommendationService;

    @Autowired
    private RAGService ragService;

    @Autowired
    private GeminiAIService geminiAIService;

    @Autowired
    private ImageEmbeddingService imageEmbeddingService;

    @Autowired
    private ScalableAISearchService scalableAISearchService;

    @Value("${ai.enabled:true}")
    private boolean aiEnabled;

    @Value("${ai.orchestration.parallel-processing:true}")
    private boolean parallelProcessingEnabled;

    @Value("${ai.orchestration.max-concurrent-tasks:10}")
    private int maxConcurrentTasks;

    @PostConstruct
    public void initialize() {
        logger.info("🎯 [AI-ORCHESTRATION] Initialisation du service d'orchestration IA");

        if (!aiEnabled) {
            logger.warn("⚠️ [AI-ORCHESTRATION] Services IA désactivés");
            return;
        }

        validateServices();
        logger.info("✅ [AI-ORCHESTRATION] Service d'orchestration IA initialisé avec succès");
    }

    /**
     * 🚀 RECHERCHE UNIVERSELLE - Point d'entrée principal pour toute recherche
     */
    public CompletableFuture<UniversalSearchResponse> universalSearch(UniversalSearchRequest request) {
        logger.info("🚀 [AI-ORCHESTRATION] Recherche universelle: {}", request.getQuery());

        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            UniversalSearchResponse response = new UniversalSearchResponse();

            try {
                // 1. Déterminer le type de recherche optimal
                SearchStrategy strategy = determineSearchStrategy(request);
                response.setStrategy(strategy.name());

                // 2. Exécuter la recherche selon la stratégie
                switch (strategy) {
                    case TEXT_ONLY:
                        executeTextSearch(request, response);
                        break;
                    case IMAGE_ONLY:
                        executeImageSearch(request, response);
                        break;
                    case HYBRID_TEXT_IMAGE:
                        executeHybridSearch(request, response);
                        break;
                    case RECOMMENDATION_BASED:
                        executeRecommendationSearch(request, response);
                        break;
                }

                // 3. Enrichissement intelligent avec toutes les IA
                enrichResponseWithAllAI(request, response);

                // 4. Post-traitement et optimisation
                optimizeResponse(response);

                response.setSuccess(true);
                response.setProcessingTimeMs(System.currentTimeMillis() - startTime);

            } catch (Exception e) {
                logger.error("❌ [AI-ORCHESTRATION] Erreur recherche universelle: {}", e.getMessage());
                response = createErrorResponse(request, e);
            }

            return response;
        });
    }

    /**
     * 🖼️ ANALYSE COMPLÈTE D'IMAGE avec toutes les IA
     */
    public CompletableFuture<CompleteImageAnalysisResponse> completeImageAnalysis(
            MultipartFile imageFile, String userId) {

        logger.info("🖼️ [AI-ORCHESTRATION] Analyse complète image: {}", imageFile.getOriginalFilename());

        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            CompleteImageAnalysisResponse response = new CompleteImageAnalysisResponse();

            try {
                // Lancer toutes les analyses en parallèle
                List<CompletableFuture<Void>> analysisTask = new ArrayList<>();

                // 1. Analyse Gemini Vision
                CompletableFuture<Void> geminiTask = CompletableFuture.runAsync(() -> {
                    try {
                        Map<String, Object> geminiAnalysis = imageSearchService.analyzeImageWithAI(imageFile);
                        response.setGeminiAnalysis(geminiAnalysis);
                    } catch (Exception e) {
                        logger.warn("⚠️ Erreur analyse Gemini: {}", e.getMessage());
                    }
                });
                analysisTask.add(geminiTask);

                // 2. Recherche par similarité d'image
                CompletableFuture<Void> similarityTask = CompletableFuture.runAsync(() -> {
                    try {
                        ImageSearchResponse similarityResults = imageSearchService.searchByUploadedImage(
                                imageFile, "MIXED");
                        response.setSimilarityResults(similarityResults);
                    } catch (Exception e) {
                        logger.warn("⚠️ Erreur recherche similarité: {}", e.getMessage());
                    }
                });
                analysisTask.add(similarityTask);

                // 3. Recommandations basées sur l'image
                if (userId != null) {
                    CompletableFuture<Void> recommendationTask = CompletableFuture.runAsync(() -> {
                        try {
                            // Attendre les résultats de similarité d'abord
                            ImageSearchResponse simResults = response.getSimilarityResults();
                            if (simResults != null) {
                                List<RecommendationItem> recommendations =
                                        recommendationService.recommendFromImageSearch(
                                                simResults.getSimilarProducts(), userId);
                                response.setPersonalizedRecommendations(recommendations);
                            }
                        } catch (Exception e) {
                            logger.warn("⚠️ Erreur recommandations image: {}", e.getMessage());
                        }
                    });
                    analysisTask.add(recommendationTask);
                }

                // 4. Extraction d'embeddings
                CompletableFuture<Void> embeddingTask = CompletableFuture.runAsync(() -> {
                    try {
                        float[] embeddings = imageEmbeddingService.extractImageFeatures(imageFile);
                        response.setImageEmbeddings(embeddings);
                    } catch (Exception e) {
                        logger.warn("⚠️ Erreur extraction embeddings: {}", e.getMessage());
                    }
                });
                analysisTask.add(embeddingTask);

                // Attendre la completion de toutes les tâches
                CompletableFuture.allOf(analysisTask.toArray(new CompletableFuture[0])).join();

                // 5. Synthèse intelligente des résultats
                synthesizeImageAnalysisResults(response);

                response.setSuccess(true);
                response.setProcessingTimeMs(System.currentTimeMillis() - startTime);

            } catch (Exception e) {
                logger.error("❌ [AI-ORCHESTRATION] Erreur analyse complète image: {}", e.getMessage());
                response.setSuccess(false);
                response.setErrorMessage(e.getMessage());
            }

            return response;
        });
    }

    /**
     * 💡 RECOMMANDATIONS INTELLIGENTES MULTI-CONTEXTES
     */
    public CompletableFuture<IntelligentRecommendationResponse> intelligentRecommendations(
            String userId, Map<String, Object> context) {

        logger.info("💡 [AI-ORCHESTRATION] Recommandations intelligentes pour: {}", userId);

        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            IntelligentRecommendationResponse response = new IntelligentRecommendationResponse();

            try {
                List<CompletableFuture<List<RecommendationItem>>> recommendationTasks = new ArrayList<>();

                // 1. Recommandations basées sur l'utilisateur
                CompletableFuture<List<RecommendationItem>> userBasedTask = CompletableFuture.supplyAsync(() -> {
                    try {
                        return recommendationService.recommendProductsForUser(userId, 10);
                    } catch (Exception e) {
                        logger.warn("⚠️ Erreur recommandations utilisateur: {}", e.getMessage());
                        return new ArrayList<>();
                    }
                });
                recommendationTasks.add(userBasedTask);

                // 2. Recommandations contextuelles
                for (String contextType : Arrays.asList("trending", "seasonal", "budget")) {
                    CompletableFuture<List<RecommendationItem>> contextTask = CompletableFuture.supplyAsync(() -> {
                        try {
                            return recommendationService.getContextualRecommendations(
                                    userId, contextType, context);
                        } catch (Exception e) {
                            logger.warn("⚠️ Erreur recommandations {}: {}", contextType, e.getMessage());
                            return new ArrayList<>();
                        }
                    });
                    recommendationTasks.add(contextTask);
                }

                // 3. Recommandations de boutiques
                String ville = (String) context.get("ville");
                CompletableFuture<List<RecommendationItem>> boutiqueTask = CompletableFuture.supplyAsync(() -> {
                    try {
                        return recommendationService.recommendBoutiquesForUser(userId, ville, 5);
                    } catch (Exception e) {
                        logger.warn("⚠️ Erreur recommandations boutiques: {}", e.getMessage());
                        return new ArrayList<>();
                    }
                });
                recommendationTasks.add(boutiqueTask);

                // Attendre toutes les recommandations
                CompletableFuture<Void> allTasks = CompletableFuture.allOf(
                        recommendationTasks.toArray(new CompletableFuture[0]));

                allTasks.join();

                // Collecter et fusionner les résultats
                List<RecommendationItem> allRecommendations = new ArrayList<>();
                for (CompletableFuture<List<RecommendationItem>> task : recommendationTasks) {
                    allRecommendations.addAll(task.get());
                }

                // 4. Déduplication et optimisation intelligente
                List<RecommendationItem> optimizedRecommendations =
                        optimizeRecommendations(allRecommendations, context);

                // 5. Enrichissement avec l'IA
                enrichRecommendationsWithGemini(optimizedRecommendations, userId, context);

                response.setRecommendations(optimizedRecommendations);
                response.setUserId(userId);
                response.setContext(context);
                response.setSuccess(true);
                response.setProcessingTimeMs(System.currentTimeMillis() - startTime);

            } catch (Exception e) {
                logger.error("❌ [AI-ORCHESTRATION] Erreur recommandations intelligentes: {}", e.getMessage());
                response.setSuccess(false);
                response.setErrorMessage(e.getMessage());
            }

            return response;
        });
    }

    /**
     * 🔗 RECHERCHE HYBRIDE - Combine RAG, recherche par image et recommandations
     */
    public CompletableFuture<HybridSearchResponse> hybridSearch(HybridSearchRequest request) {
        logger.info("🔗 [AI-ORCHESTRATION] Recherche hybride: {}", request.getTextQuery());

        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            HybridSearchResponse response = new HybridSearchResponse();

            try {
                List<CompletableFuture<Void>> searchTasks = new ArrayList<>();

                // 1. Recherche RAG textuelle
                if (request.getTextQuery() != null && !request.getTextQuery().isEmpty()) {
                    CompletableFuture<Void> ragTask = CompletableFuture.runAsync(() -> {
                        try {
                            RagSearchResponse ragResults = ragService.searchWithRAG(
                                    request.getTextQuery(), request.getSearchType());
                            response.setRagResults(ragResults);
                        } catch (Exception e) {
                            logger.warn("⚠️ Erreur recherche RAG: {}", e.getMessage());
                        }
                    });
                    searchTasks.add(ragTask);
                }

                // 2. Recherche par image (si fournie)
                if (request.getImageFile() != null) {
                    CompletableFuture<Void> imageTask = CompletableFuture.runAsync(() -> {
                        try {
                            ImageSearchResponse imageResults = imageSearchService.searchByUploadedImage(
                                    request.getImageFile(), request.getSearchType());
                            response.setImageResults(imageResults);
                        } catch (Exception e) {
                            logger.warn("⚠️ Erreur recherche image: {}", e.getMessage());
                        }
                    });
                    searchTasks.add(imageTask);
                }

                // 3. Recommandations personnalisées
                if (request.getUserId() != null) {
                    CompletableFuture<Void> recommendationTask = CompletableFuture.runAsync(() -> {
                        try {
                            List<RecommendationItem> recommendations =
                                    recommendationService.recommendProductsForUser(request.getUserId(), 10);
                            response.setPersonalizedRecommendations(recommendations);
                        } catch (Exception e) {
                            logger.warn("⚠️ Erreur recommandations: {}", e.getMessage());
                        }
                    });
                    searchTasks.add(recommendationTask);
                }

                // Attendre toutes les recherches
                CompletableFuture.allOf(searchTasks.toArray(new CompletableFuture[0])).join();

                // 4. Fusion intelligente des résultats
                fuseHybridResults(response, request);

                // 5. Enrichissement final avec Gemini
                enrichHybridResponseWithGemini(response, request);

                response.setSuccess(true);
                response.setProcessingTimeMs(System.currentTimeMillis() - startTime);

            } catch (Exception e) {
                logger.error("❌ [AI-ORCHESTRATION] Erreur recherche hybride: {}", e.getMessage());
                response.setSuccess(false);
                response.setErrorMessage(e.getMessage());
            }

            return response;
        });
    }

    // =============== MÉTHODES PRIVÉES D'ORCHESTRATION ===============

    private SearchStrategy determineSearchStrategy(UniversalSearchRequest request) {
        if (request.getImageFile() != null && request.getQuery() != null) {
            return SearchStrategy.HYBRID_TEXT_IMAGE;
        } else if (request.getImageFile() != null) {
            return SearchStrategy.IMAGE_ONLY;
        } else if (request.getUserId() != null && "recommendation".equals(request.getSearchMode())) {
            return SearchStrategy.RECOMMENDATION_BASED;
        } else {
            return SearchStrategy.TEXT_ONLY;
        }
    }

    private void executeTextSearch(UniversalSearchRequest request, UniversalSearchResponse response) {
        try {
            RagSearchResponse ragResults = ragService.searchWithRAG(request.getQuery(), "MIXED");
            response.setRagResults(ragResults);
        } catch (Exception e) {
            logger.warn("⚠️ Erreur recherche textuelle: {}", e.getMessage());
        }
    }

    private void executeImageSearch(UniversalSearchRequest request, UniversalSearchResponse response) {
        try {
            ImageSearchResponse imageResults = imageSearchService.searchByUploadedImage(
                    request.getImageFile(), "MIXED");
            response.setImageResults(imageResults);
        } catch (Exception e) {
            logger.warn("⚠️ Erreur recherche image: {}", e.getMessage());
        }
    }

    private void executeHybridSearch(UniversalSearchRequest request, UniversalSearchResponse response) {
        // Exécuter les deux types de recherche
        executeTextSearch(request, response);
        executeImageSearch(request, response);
    }

    private void executeRecommendationSearch(UniversalSearchRequest request, UniversalSearchResponse response) {
        try {
            List<RecommendationItem> recommendations = recommendationService.recommendProductsForUser(
                    request.getUserId(), 20);
            response.setRecommendations(recommendations);
        } catch (Exception e) {
            logger.warn("⚠️ Erreur recherche recommandations: {}", e.getMessage());
        }
    }

    private void enrichResponseWithAllAI(UniversalSearchRequest request, UniversalSearchResponse response) {
        try {
            // Enrichissement avec Gemini pour l'analyse contextuelle
            String context = buildContextFromResponse(response);
            if (!context.isEmpty()) {
                String aiInsights = geminiAIService.generateAdvancedAnalysis(request.getQuery(), context);
                response.setAiInsights(aiInsights);
            }

            // Ajouter des recommandations complémentaires
            if (request.getUserId() != null) {
                addComplementaryRecommendations(request, response);
            }

        } catch (Exception e) {
            logger.warn("⚠️ Erreur enrichissement IA: {}", e.getMessage());
        }
    }

    private void optimizeResponse(UniversalSearchResponse response) {
        // Déduplication des résultats
        deduplicateResults(response);

        // Tri par pertinence
        sortResultsByRelevance(response);

        // Limitation des résultats
        limitResults(response);
    }

    private List<RecommendationItem> optimizeRecommendations(List<RecommendationItem> recommendations,
                                                             Map<String, Object> context) {
        // Déduplication par ID
        Map<String, RecommendationItem> uniqueRecommendations = new LinkedHashMap<>();

        for (RecommendationItem item : recommendations) {
            String key = item.getId();
            if (uniqueRecommendations.containsKey(key)) {
                // Combiner les scores
                RecommendationItem existing = uniqueRecommendations.get(key);
                existing.setScore(Math.max(existing.getScore(), item.getScore()));
            } else {
                uniqueRecommendations.put(key, item);
            }
        }

        // Tri par score décroissant et limitation
        return uniqueRecommendations.values().stream()
                .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
                .limit(15)
                .collect(Collectors.toList());
    }

    private void validateServices() {
        List<String> missingServices = new ArrayList<>();

        if (imageSearchService == null) missingServices.add("ImageSearchService");
        if (recommendationService == null) missingServices.add("RecommendationService");
        if (ragService == null) missingServices.add("RAGService");
        if (geminiAIService == null) missingServices.add("GeminiAIService");

        if (!missingServices.isEmpty()) {
            logger.warn("⚠️ [AI-ORCHESTRATION] Services manquants: {}", missingServices);
        }
    }

    // Méthodes utilitaires à implémenter selon vos besoins
    private void synthesizeImageAnalysisResults(CompleteImageAnalysisResponse response) {
        // Synthèse intelligente des résultats d'analyse d'image
    }

    private void enrichRecommendationsWithGemini(List<RecommendationItem> recommendations,
                                                 String userId, Map<String, Object> context) {
        // Enrichissement Gemini des recommandations
    }

    private void fuseHybridResults(HybridSearchResponse response, HybridSearchRequest request) {
        // Fusion intelligente des résultats hybrides
    }

    private void enrichHybridResponseWithGemini(HybridSearchResponse response, HybridSearchRequest request) {
        // Enrichissement Gemini de la réponse hybride
    }

    private String buildContextFromResponse(UniversalSearchResponse response) {
        // Construction du contexte pour l'IA
        return "";
    }

    private void addComplementaryRecommendations(UniversalSearchRequest request, UniversalSearchResponse response) {
        // Ajout de recommandations complémentaires
    }

    private void deduplicateResults(UniversalSearchResponse response) {
        // Déduplication des résultats
    }

    private void sortResultsByRelevance(UniversalSearchResponse response) {
        // Tri par pertinence
    }

    private void limitResults(UniversalSearchResponse response) {
        // Limitation des résultats
    }

    private UniversalSearchResponse createErrorResponse(UniversalSearchRequest request, Exception e) {
        UniversalSearchResponse response = new UniversalSearchResponse();
        response.setSuccess(false);
        response.setErrorMessage(e.getMessage());
        response.setQuery(request.getQuery());
        return response;
    }

    // Énumérations et classes utilitaires
    private enum SearchStrategy {
        TEXT_ONLY, IMAGE_ONLY, HYBRID_TEXT_IMAGE, RECOMMENDATION_BASED
    }

    // =============== MÉTHODES PUBLIQUES DE GESTION ===============

    @Async
    public void warmupAIServices() {
        logger.info("🔥 [AI-ORCHESTRATION] Préchauffage des services IA");

        // Préchauffage en arrière-plan
        try {
            if (imageEmbeddingService != null) {
                imageSearchService.refreshProductEmbeddings();
            }

            // Autres opérations de préchauffage...

        } catch (Exception e) {
            logger.warn("⚠️ [AI-ORCHESTRATION] Erreur préchauffage: {}", e.getMessage());
        }
    }

    public Map<String, Object> getOrchestrationStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("aiEnabled", aiEnabled);
        stats.put("parallelProcessingEnabled", parallelProcessingEnabled);
        stats.put("maxConcurrentTasks", maxConcurrentTasks);

        // Ajouter les stats de chaque service
        if (imageSearchService != null) {
            stats.put("imageSearch", imageSearchService.getStatistics());
        }
        if (ragService != null) {
            stats.put("rag", ragService.getStatistics());
        }

        return stats;
    }

    public void clearAllAICaches() {
        logger.info("🗑️ [AI-ORCHESTRATION] Nettoyage de tous les caches IA");

        if (imageSearchService != null) imageSearchService.clearCache();
        if (recommendationService != null) recommendationService.clearCache();
        if (imageEmbeddingService != null) imageEmbeddingService.clearCache();
        if (ragService != null) ragService.clearCache();
    }
}