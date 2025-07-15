package com.boutique_catalogue_produits.service;

import com.boutique_catalogue_produits.dto.*;
import com.boutique_catalogue_produits.model.Boutique;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 🎯 Service de recommandations intelligentes
 * Utilise l'IA et l'analyse comportementale pour recommander produits et boutiques
 */
@Service
public class RecommendationService {

    private static final Logger logger = LoggerFactory.getLogger(RecommendationService.class);

    @Autowired
    private ProduitService produitService;

    @Autowired
    private BoutiqueService boutiqueService;

    @Autowired
    private GeminiAIService geminiAIService;

    @Autowired
    private ImageEmbeddingService imageEmbeddingService;

    @Value("${ai.recommendation.max.results:10}")
    private int maxRecommendations;

    @Value("${ai.recommendation.similarity.threshold:0.3}")
    private double similarityThreshold;

    @Value("${ai.recommendation.cache.enabled:true}")
    private boolean cacheEnabled;

    @Value("${ai.recommendation.collaborative.weight:0.4}")
    private double collaborativeWeight;

    @Value("${ai.recommendation.content.weight:0.6}")
    private double contentWeight;

    // Caches pour optimiser les performances
    private final Map<String, List<RecommendationItem>> productRecommendationsCache = new ConcurrentHashMap<>();
    private final Map<String, List<RecommendationItem>> boutiqueRecommendationsCache = new ConcurrentHashMap<>();
    private final Map<String, UserProfile> userProfilesCache = new ConcurrentHashMap<>();

    // Matrices pour le filtrage collaboratif
    private Map<String, Map<String, Double>> userProductMatrix = new ConcurrentHashMap<>();
    private Map<String, Set<String>> productCategoriesMap = new ConcurrentHashMap<>();

    @PostConstruct
    public void initialize() {
        logger.info("🎯 [RECOMMENDATION] Initialisation du service de recommandations IA");

        // Construire la matrice utilisateur-produit pour le filtrage collaboratif
        buildCollaborativeFilteringMatrix();

        // Pré-calculer les catégories de produits
        buildProductCategoriesMap();
    }

    /**
     * 🛍️ Recommandations de produits basées sur l'historique utilisateur
     */
    public List<RecommendationItem> recommendProductsForUser(String userId, int limit) {
        logger.info("🛍️ [RECOMMENDATION] Recommandations produits pour utilisateur: {}", userId);

        try {
            // Vérifier le cache
            String cacheKey = "user_products_" + userId + "_" + limit;
            if (cacheEnabled && productRecommendationsCache.containsKey(cacheKey)) {
                logger.debug("📦 [CACHE] Recommandations trouvées en cache");
                return productRecommendationsCache.get(cacheKey);
            }

            // 1. Construire le profil utilisateur
            UserProfile userProfile = buildUserProfile(userId);

            // 2. Recommandations hybrides (collaborative + content-based)
            List<RecommendationItem> collaborativeRecs = getCollaborativeRecommendations(userId, userProfile);
            List<RecommendationItem> contentRecs = getContentBasedRecommendations(userProfile);

            // 3. Combiner et scorer les recommandations
            List<RecommendationItem> hybridRecs = combineRecommendations(collaborativeRecs, contentRecs);

            // 4. Enrichir avec l'IA
            enrichRecommendationsWithAI(hybridRecs, userProfile);

            // 5. Filtrer et limiter
            List<RecommendationItem> finalRecs = hybridRecs.stream()
                    .filter(rec -> rec.getScore() >= similarityThreshold)
                    .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
                    .limit(limit)
                    .collect(Collectors.toList());

            // Mettre en cache
            if (cacheEnabled) {
                productRecommendationsCache.put(cacheKey, finalRecs);
            }

            logger.info("✅ [RECOMMENDATION] {} recommandations produits générées", finalRecs.size());
            return finalRecs;

        } catch (Exception e) {
            logger.error("❌ [RECOMMENDATION] Erreur recommandations produits: {}", e.getMessage());
            return getFallbackProductRecommendations(limit);
        }
    }

    /**
     * 🏪 Recommandations de boutiques pour un utilisateur
     */
    public List<RecommendationItem> recommendBoutiquesForUser(String userId, String ville, int limit) {
        logger.info("🏪 [RECOMMENDATION] Recommandations boutiques pour utilisateur: {} (ville: {})",
                userId, ville);

        try {
            String cacheKey = "user_boutiques_" + userId + "_" + ville + "_" + limit;
            if (cacheEnabled && boutiqueRecommendationsCache.containsKey(cacheKey)) {
                return boutiqueRecommendationsCache.get(cacheKey);
            }

            // 1. Profil utilisateur
            UserProfile userProfile = buildUserProfile(userId);

            // 2. Filtrer boutiques par localisation
            List<Boutique> boutiquesInCity = boutiqueService.getAllBoutiques().stream()
                    .filter(Boutique::isValid)
                    .filter(b -> ville == null || ville.equalsIgnoreCase(b.getVille()))
                    .collect(Collectors.toList());

            // 3. Scorer les boutiques selon les préférences utilisateur
            List<RecommendationItem> recommendations = scoreBoutiques(boutiquesInCity, userProfile);

            // 4. Enrichir avec analyse IA
            enrichBoutiqueRecommendationsWithAI(recommendations, userProfile, ville);

            // 5. Finaliser
            List<RecommendationItem> finalRecs = recommendations.stream()
                    .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
                    .limit(limit)
                    .collect(Collectors.toList());

            if (cacheEnabled) {
                boutiqueRecommendationsCache.put(cacheKey, finalRecs);
            }

            logger.info("✅ [RECOMMENDATION] {} recommandations boutiques générées", finalRecs.size());
            return finalRecs;

        } catch (Exception e) {
            logger.error("❌ [RECOMMENDATION] Erreur recommandations boutiques: {}", e.getMessage());
            return getFallbackBoutiqueRecommendations(ville, limit);
        }
    }

    /**
     * 🔍 Recommandations basées sur un produit spécifique
     */
    public List<RecommendationItem> recommendSimilarProducts(String productId, int limit) {
        logger.info("🔍 [RECOMMENDATION] Produits similaires à: {}", productId);

        try {
            // 1. Récupérer le produit de référence (Optional)
            Optional<ProduitDTO> optionalProduct = produitService.getProduitById(Long.parseLong(productId));
            if (optionalProduct.isEmpty()) {
                return new ArrayList<>();
            }

            ProduitDTO referenceProduct = optionalProduct.get();

            // 2. Recommandations basées sur le contenu
            List<RecommendationItem> contentSimilar = findContentSimilarProducts(referenceProduct);

            // 3. Recommandations basées sur l'image (si disponible)
            List<RecommendationItem> imageSimilar = findImageSimilarProducts(referenceProduct);

            // 4. Combiner les résultats
            List<RecommendationItem> combined = combineProductSimilarityRecommendations(
                    contentSimilar, imageSimilar);

            // 5. Enrichir avec l'IA
            enrichSimilarProductsWithAI(combined, referenceProduct);

            return combined.stream()
                    .filter(rec -> !productId.equals(rec.getId()))
                    .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
                    .limit(limit)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            logger.error("❌ [RECOMMENDATION] Erreur produits similaires: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 🎨 Recommandations basées sur une recherche par image
     */
    public List<RecommendationItem> recommendFromImageSearch(List<SimilarImageResult> imageResults,
                                                             String userId) {
        logger.info("🎨 [RECOMMENDATION] Recommandations depuis recherche image pour: {}", userId);

        try {
            List<RecommendationItem> recommendations = new ArrayList<>();

            // 1. Convertir les résultats d'image en recommandations
            for (SimilarImageResult imageResult : imageResults) {
                RecommendationItem item = new RecommendationItem();
                item.setId(imageResult.getProductId());
                item.setTitle(imageResult.getProductName());
                item.setType("PRODUIT");
                item.setScore(imageResult.getSimilarityScore());
                item.setReason("Similaire visuellement à votre recherche");
                item.setCategory(imageResult.getCategory());

                Map<String, Object> metadata = new HashMap<>();
                metadata.put("imageUrl", imageResult.getImageUrl());
                metadata.put("price", imageResult.getPrice());
                metadata.put("visualSimilarity", imageResult.getSimilarityScore());
                item.setMetadata(metadata);

                recommendations.add(item);
            }

            // 2. Enrichir avec le profil utilisateur si disponible
            if (userId != null) {
                UserProfile userProfile = buildUserProfile(userId);
                personalizeImageRecommendations(recommendations, userProfile);
            }

            // 3. Ajouter des recommandations complémentaires
            addComplementaryRecommendations(recommendations, userId);

            return recommendations.stream()
                    .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
                    .limit(maxRecommendations)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            logger.error("❌ [RECOMMENDATION] Erreur recommandations image: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 🧠 Recommandations intelligentes contextuelles
     */
    public List<RecommendationItem> getContextualRecommendations(String userId, String context,
                                                                 Map<String, Object> parameters) {
        logger.info("🧠 [RECOMMENDATION] Recommandations contextuelles: {} (contexte: {})", userId, context);

        try {
            UserProfile userProfile = buildUserProfile(userId);
            List<RecommendationItem> recommendations = new ArrayList<>();

            switch (context.toLowerCase()) {
                case "trending":
                    recommendations = getTrendingRecommendations(userProfile, parameters);
                    break;
                case "seasonal":
                    recommendations = getSeasonalRecommendations(userProfile, parameters);
                    break;
                case "budget":
                    recommendations = getBudgetRecommendations(userProfile, parameters);
                    break;
                case "location":
                    recommendations = getLocationBasedRecommendations(userProfile, parameters);
                    break;
                default:
                    recommendations = getGeneralRecommendations(userProfile, parameters);
            }

            // Enrichir avec l'IA contextuelle
            enrichContextualRecommendationsWithAI(recommendations, userProfile, context, parameters);

            return recommendations.stream()
                    .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
                    .limit(maxRecommendations)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            logger.error("❌ [RECOMMENDATION] Erreur recommandations contextuelles: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    // =============== MÉTHODES PRIVÉES DE CONSTRUCTION DU PROFIL ===============

    private UserProfile buildUserProfile(String userId) {
        // Vérifier le cache
        if (cacheEnabled && userProfilesCache.containsKey(userId)) {
            return userProfilesCache.get(userId);
        }

        UserProfile profile = new UserProfile();
        profile.setUserId(userId);

        try {
            // 1. Analyser l'historique d'achats/vues (simulation)
            Map<String, Double> categoryPreferences = analyzeUserCategoryPreferences(userId);
            profile.setCategoryPreferences(categoryPreferences);

            // 2. Analyser les préférences de prix
            PriceRange priceRange = analyzeUserPriceRange(userId);
            profile.setPriceRange(priceRange);

            // 3. Analyser les marques préférées
            Set<String> preferredBrands = analyzeUserBrandPreferences(userId);
            profile.setPreferredBrands(preferredBrands);

            // 4. Analyser la géolocalisation
            String preferredLocation = analyzeUserLocationPreference(userId);
            profile.setPreferredLocation(preferredLocation);

            // 5. Calculer le score de diversité
            double diversityScore = calculateUserDiversityScore(userId);
            profile.setDiversityScore(diversityScore);

            // Mettre en cache
            if (cacheEnabled) {
                userProfilesCache.put(userId, profile);
            }

        } catch (Exception e) {
            logger.warn("⚠️ [RECOMMENDATION] Erreur construction profil {}: {}", userId, e.getMessage());
            // Retourner un profil par défaut
            profile = createDefaultUserProfile(userId);
        }

        return profile;
    }

    private Map<String, Double> analyzeUserCategoryPreferences(String userId) {
        Map<String, Double> preferences = new HashMap<>();

        // Simulation - en production, analyser les vraies données utilisateur
        preferences.put("electronique", 0.4);
        preferences.put("vetements", 0.3);
        preferences.put("sport", 0.2);
        preferences.put("beaute", 0.1);

        return preferences;
    }

    private PriceRange analyzeUserPriceRange(String userId) {
        // Simulation - analyser l'historique d'achats réel
        return new PriceRange(100.0, 1000.0, 300.0); // min, max, moyenne
    }

    private Set<String> analyzeUserBrandPreferences(String userId) {
        // Simulation
        return Set.of("Samsung", "Nike", "Apple");
    }

    private String analyzeUserLocationPreference(String userId) {
        // Simulation
        return "Casablanca";
    }

    private double calculateUserDiversityScore(String userId) {
        // Score de 0 (très spécialisé) à 1 (très diversifié)
        return 0.6; // Simulation
    }

    // =============== MÉTHODES DE RECOMMANDATION COLLABORATIVE ===============

    private List<RecommendationItem> getCollaborativeRecommendations(String userId, UserProfile profile) {
        List<RecommendationItem> recommendations = new ArrayList<>();

        try {
            // 1. Trouver des utilisateurs similaires
            List<String> similarUsers = findSimilarUsers(userId, profile);

            // 2. Analyser leurs préférences
            Map<String, Double> productScores = new HashMap<>();

            for (String similarUser : similarUsers) {
                Map<String, Double> userPrefs = userProductMatrix.get(similarUser);
                if (userPrefs != null) {
                    for (Map.Entry<String, Double> entry : userPrefs.entrySet()) {
                        productScores.merge(entry.getKey(), entry.getValue(), Double::sum);
                    }
                }
            }

            // 3. Convertir en recommandations
            for (Map.Entry<String, Double> entry : productScores.entrySet()) {
                try {
                    Optional<ProduitDTO> optionalProduct = produitService.getProduitById(Long.parseLong(entry.getKey()));
                    if (optionalProduct.isPresent()) {
                        ProduitDTO product = optionalProduct.get();
                        RecommendationItem item = createRecommendationFromProduct(product);
                        item.setScore(entry.getValue() * collaborativeWeight);
                        item.setReason("Recommandé par des utilisateurs similaires");
                        recommendations.add(item);
                    }
                } catch (Exception e) {
                    logger.debug("Erreur produit collaborative: {}", e.getMessage());
                }
            }


        } catch (Exception e) {
            logger.warn("⚠️ [RECOMMENDATION] Erreur collaborative: {}", e.getMessage());
        }

        return recommendations;
    }

    private List<String> findSimilarUsers(String userId, UserProfile profile) {
        // Simulation de recherche d'utilisateurs similaires
        // En production, utiliser des algorithmes comme cosine similarity
        return Arrays.asList("user1", "user2", "user3");
    }

    // =============== MÉTHODES DE RECOMMANDATION CONTENT-BASED ===============

    private List<RecommendationItem> getContentBasedRecommendations(UserProfile profile) {
        List<RecommendationItem> recommendations = new ArrayList<>();

        try {
            List<ProduitDTO> allProducts = produitService.getAllProduits();

            for (ProduitDTO product : allProducts) {
                double score = calculateContentScore(product, profile);

                if (score >= similarityThreshold) {
                    RecommendationItem item = createRecommendationFromProduct(product);
                    item.setScore(score * contentWeight);
                    item.setReason("Correspond à vos préférences");
                    recommendations.add(item);
                }
            }

        } catch (Exception e) {
            logger.warn("⚠️ [RECOMMENDATION] Erreur content-based: {}", e.getMessage());
        }

        return recommendations;
    }

    private double calculateContentScore(ProduitDTO product, UserProfile profile) {
        double score = 0.0;

        // 1. Score de catégorie
        String productCategory = product.getIdCategorie() != null ? product.getIdCategorie().toString() : "general";
        Double categoryPref = profile.getCategoryPreferences().get(productCategory);
        if (categoryPref != null) {
            score += categoryPref * 0.4;
        }

        // 2. Score de prix
        if (product.getPrix() != null && profile.getPriceRange() != null) {
            if (product.getPrix().doubleValue() >= profile.getPriceRange().getMin() &&
                    product.getPrix().doubleValue() <= profile.getPriceRange().getMax()) {
                score += 0.3;
            }
        }



        // 4. Score de disponibilité
        if (product.getQuantite() != null && product.getQuantite() > 0) {
            score += 0.1;
        }

        return Math.min(1.0, score);
    }

    // =============== MÉTHODES UTILITAIRES ===============

    private List<RecommendationItem> combineRecommendations(List<RecommendationItem> list1,
                                                            List<RecommendationItem> list2) {
        Map<String, RecommendationItem> combined = new HashMap<>();

        // Ajouter la première liste
        for (RecommendationItem item : list1) {
            combined.put(item.getId(), item);
        }

        // Combiner avec la seconde liste
        for (RecommendationItem item : list2) {
            if (combined.containsKey(item.getId())) {
                // Combiner les scores
                RecommendationItem existing = combined.get(item.getId());
                existing.setScore(existing.getScore() + item.getScore());
                existing.setReason(existing.getReason() + " + " + item.getReason());
            } else {
                combined.put(item.getId(), item);
            }
        }

        return new ArrayList<>(combined.values());
    }

    private RecommendationItem createRecommendationFromProduct(ProduitDTO product) {
        RecommendationItem item = new RecommendationItem();
        item.setId(product.getId().toString());
        item.setTitle(product.getNomProduit());
        item.setType("PRODUIT");
        item.setCategory(product.getIdCategorie() != null ? product.getIdCategorie().toString() : "general");
        item.setDescription(product.getDescription());

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("price", product.getPrix());
        metadata.put("quantity", product.getQuantite());
        metadata.put("imageUrl", product.getImageUrl());
        item.setMetadata(metadata);

        return item;
    }

    private void enrichRecommendationsWithAI(List<RecommendationItem> recommendations, UserProfile profile) {
        try {
            if (!recommendations.isEmpty()) {
                // Demander à l'IA d'analyser et d'améliorer les recommandations
                String context = buildContextForAI(recommendations, profile);
                String aiInsights = geminiAIService.generateRecommendations(
                        "Améliorez ces recommandations", new ArrayList<>(), new ArrayList<>());

                // Appliquer les insights IA (simulation)
                for (RecommendationItem item : recommendations) {
                    // Ajuster le score basé sur l'analyse IA
                    item.setScore(item.getScore() * 1.1); // Boost IA
                }
            }
        } catch (Exception e) {
            logger.debug("Erreur enrichissement IA: {}", e.getMessage());
        }
    }

    private String buildContextForAI(List<RecommendationItem> recommendations, UserProfile profile) {
        StringBuilder context = new StringBuilder();
        context.append("Profil utilisateur: ").append(profile.getCategoryPreferences()).append("\n");
        context.append("Recommandations actuelles: ");

        for (RecommendationItem item : recommendations.stream().limit(5).collect(Collectors.toList())) {
            context.append(item.getTitle()).append(" (").append(item.getScore()).append("), ");
        }

        return context.toString();
    }

    // Méthodes de fallback et classes utilitaires à implémenter...
    private List<RecommendationItem> getFallbackProductRecommendations(int limit) {
        return new ArrayList<>(); // Implémentation simplifiée
    }

    private List<RecommendationItem> getFallbackBoutiqueRecommendations(String ville, int limit) {
        return new ArrayList<>(); // Implémentation simplifiée
    }

    private UserProfile createDefaultUserProfile(String userId) {
        UserProfile profile = new UserProfile();
        profile.setUserId(userId);
        profile.setCategoryPreferences(Map.of("general", 1.0));
        profile.setPriceRange(new PriceRange(0.0, 10000.0, 500.0));
        profile.setPreferredBrands(new HashSet<>());
        profile.setDiversityScore(0.5);
        return profile;
    }

    // Autres méthodes à implémenter selon vos besoins...

    private void buildCollaborativeFilteringMatrix() {
        // Construction de la matrice utilisateur-produit
        logger.info("🔄 [RECOMMENDATION] Construction matrice collaborative");
    }

    private void buildProductCategoriesMap() {
        // Construction de la map des catégories
        logger.info("🔄 [RECOMMENDATION] Construction map catégories");
    }

    // Classes pour les autres méthodes mentionnées mais non détaillées
    private List<RecommendationItem> findContentSimilarProducts(ProduitDTO reference) { return new ArrayList<>(); }
    private List<RecommendationItem> findImageSimilarProducts(ProduitDTO reference) { return new ArrayList<>(); }
    private List<RecommendationItem> combineProductSimilarityRecommendations(List<RecommendationItem> a, List<RecommendationItem> b) { return new ArrayList<>(a); }
    private void enrichSimilarProductsWithAI(List<RecommendationItem> recs, ProduitDTO ref) {}
    private List<RecommendationItem> scoreBoutiques(List<Boutique> boutiques, UserProfile profile) { return new ArrayList<>(); }
    private void enrichBoutiqueRecommendationsWithAI(List<RecommendationItem> recs, UserProfile profile, String ville) {}
    private void personalizeImageRecommendations(List<RecommendationItem> recs, UserProfile profile) {}
    private void addComplementaryRecommendations(List<RecommendationItem> recs, String userId) {}
    private List<RecommendationItem> getTrendingRecommendations(UserProfile profile, Map<String, Object> params) { return new ArrayList<>(); }
    private List<RecommendationItem> getSeasonalRecommendations(UserProfile profile, Map<String, Object> params) { return new ArrayList<>(); }
    private List<RecommendationItem> getBudgetRecommendations(UserProfile profile, Map<String, Object> params) { return new ArrayList<>(); }
    private List<RecommendationItem> getLocationBasedRecommendations(UserProfile profile, Map<String, Object> params) { return new ArrayList<>(); }
    private List<RecommendationItem> getGeneralRecommendations(UserProfile profile, Map<String, Object> params) { return new ArrayList<>(); }
    private void enrichContextualRecommendationsWithAI(List<RecommendationItem> recs, UserProfile profile, String context, Map<String, Object> params) {}

    public void clearCache() {
        productRecommendationsCache.clear();
        boutiqueRecommendationsCache.clear();
        userProfilesCache.clear();
        logger.info("🗑️ [RECOMMENDATION] Tous les caches vidés");
    }
}