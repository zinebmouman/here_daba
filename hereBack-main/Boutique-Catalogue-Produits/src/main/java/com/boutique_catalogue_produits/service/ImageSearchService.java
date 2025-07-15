package com.boutique_catalogue_produits.service;

import com.boutique_catalogue_produits.dto.ImageSearchResponse;
import com.boutique_catalogue_produits.dto.ProduitDTO;
import com.boutique_catalogue_produits.dto.SimilarImageResult;
import com.boutique_catalogue_produits.model.Boutique;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;

/**
 * 🖼️ Service de recherche intelligente par image
 * Utilise l'IA pour trouver des produits similaires par analyse d'image
 */
@Service
public class ImageSearchService {

    private static final Logger logger = LoggerFactory.getLogger(ImageSearchService.class);

    @Autowired
    private ImageEmbeddingService imageEmbeddingService;

    @Autowired
    private ProduitService produitService;

    @Autowired
    private BoutiqueService boutiqueService;

    @Autowired
    private MinIOService minioService;

    @Autowired
    private GeminiAIService geminiAIService;

    @Value("${ai.image.similarity.threshold:0.7}")
    private double similarityThreshold;

    @Value("${ai.image.max.results:20}")
    private int maxResults;

    @Value("${ai.image.cache.enabled:true}")
    private boolean cacheEnabled;

    // Cache pour les embeddings d'images
    private final Map<String, float[]> imageEmbeddingsCache = new ConcurrentHashMap<>();
    private final Map<String, List<SimilarImageResult>> searchResultsCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void initialize() {
        logger.info("🖼️ [IMAGE-SEARCH] Initialisation du service de recherche par image");

        // Pré-calculer les embeddings des images produits existantes
        precomputeProductImageEmbeddings();
    }

    /**
     * 🔍 Recherche par image uploadée
     */
    public ImageSearchResponse searchByUploadedImage(MultipartFile imageFile, String searchType) {
        logger.info("🔍 [IMAGE-SEARCH] Recherche par image uploadée (type: {})", searchType);

        long startTime = System.currentTimeMillis();

        try {
            // 1. Validation de l'image
            validateImageFile(imageFile);

            // 2. Extraction des features de l'image de requête
            float[] queryEmbedding = imageEmbeddingService.extractImageFeatures(imageFile);

            // 3. Recherche de similarité
            List<SimilarImageResult> similarImages = findSimilarImages(queryEmbedding, searchType);

            // 4. Enrichissement avec l'IA
            ImageSearchResponse response = buildSearchResponse(similarImages, imageFile.getOriginalFilename(), searchType);

            // 5. Analyse IA de l'image
            enrichWithAIAnalysis(response, imageFile);

            long processingTime = System.currentTimeMillis() - startTime;
            response.setProcessingTimeMs(processingTime);

            logger.info("✅ [IMAGE-SEARCH] {} résultats trouvés en {}ms",
                    response.getSimilarProducts().size(), processingTime);

            return response;

        } catch (Exception e) {
            logger.error("❌ [IMAGE-SEARCH] Erreur recherche par image: {}", e.getMessage(), e);
            return createErrorResponse(imageFile.getOriginalFilename(), searchType, e);
        }
    }

    /**
     * 🔗 Recherche par URL d'image
     */
    public ImageSearchResponse searchByImageUrl(String imageUrl, String searchType) {
        logger.info("🔗 [IMAGE-SEARCH] Recherche par URL: {}", imageUrl);

        try {
            // 1. Télécharger l'image depuis l'URL
            InputStream imageStream = downloadImageFromUrl(imageUrl);

            // 2. Extraction des features
            float[] queryEmbedding = imageEmbeddingService.extractImageFeatures(imageStream);

            // 3. Recherche de similarité
            List<SimilarImageResult> similarImages = findSimilarImages(queryEmbedding, searchType);

            // 4. Construction de la réponse
            ImageSearchResponse response = buildSearchResponse(similarImages, imageUrl, searchType);

            return response;

        } catch (Exception e) {
            logger.error("❌ [IMAGE-SEARCH] Erreur recherche par URL: {}", e.getMessage());
            return createErrorResponse(imageUrl, searchType, e);
        }
    }

    /**
     * 🎯 Recherche de produits similaires basée sur un produit existant
     */
    public List<SimilarImageResult> findSimilarProducts(String productId, int limit) {
        logger.info("🎯 [IMAGE-SEARCH] Recherche produits similaires pour: {}", productId);

        try {
            // 1. Récupérer le produit de référence (Optional)
            Optional<ProduitDTO> optionalProduct = produitService.getProduitById(Long.parseLong(productId));
            if (optionalProduct.isEmpty()) {
                return new ArrayList<>();
            }

            ProduitDTO referenceProduct = optionalProduct.get();

            // 2. Obtenir l'embedding de l'image du produit
            float[] referenceEmbedding = getOrComputeProductImageEmbedding(referenceProduct);

            // 3. Trouver les produits similaires
            List<SimilarImageResult> similarProducts = findSimilarImages(referenceEmbedding, "PRODUIT");

            // 4. Filtrer le produit de référence et limiter les résultats
            return similarProducts.stream()
                    .filter(result -> !productId.equals(result.getProductId()))
                    .limit(limit)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            logger.error("❌ [IMAGE-SEARCH] Erreur recherche produits similaires: {}", e.getMessage());
            return new ArrayList<>();
        }
    }


    /**
     * 🧠 Analyse d'image avec IA pour extraction de métadonnées
     */
    public Map<String, Object> analyzeImageWithAI(MultipartFile imageFile) {
        logger.info("🧠 [IMAGE-SEARCH] Analyse IA de l'image: {}", imageFile.getOriginalFilename());

        Map<String, Object> analysis = new HashMap<>();

        try {
            // 1. Analyse technique de l'image
            BufferedImage image = ImageIO.read(imageFile.getInputStream());
            analysis.put("width", image.getWidth());
            analysis.put("height", image.getHeight());
            analysis.put("format", getImageFormat(imageFile));
            analysis.put("size", imageFile.getSize());

            // 2. Extraction des couleurs dominantes
            List<String> dominantColors = extractDominantColors(image);
            analysis.put("dominantColors", dominantColors);

            // 3. Détection des objets/catégories avec l'IA
            String aiDescription = geminiAIService.analyzeImageContent(imageFile);
            analysis.put("aiDescription", aiDescription);

            // 4. Classification automatique
            String suggestedCategory = classifyImageCategory(aiDescription);
            analysis.put("suggestedCategory", suggestedCategory);

            // 5. Extraction des tags
            List<String> tags = extractTagsFromDescription(aiDescription);
            analysis.put("tags", tags);

            return analysis;

        } catch (Exception e) {
            logger.error("❌ [IMAGE-SEARCH] Erreur analyse IA: {}", e.getMessage());
            analysis.put("error", e.getMessage());
            return analysis;
        }
    }

    // =============== MÉTHODES PRIVÉES ===============

    private void precomputeProductImageEmbeddings() {
        logger.info("🔄 [IMAGE-SEARCH] Pré-calcul des embeddings produits...");

        try {
            List<ProduitDTO> allProducts = produitService.getAllProduits();
            int processedCount = 0;

            for (ProduitDTO product : allProducts) {
                try {
                    if (product.getImageUrl() != null && !product.getImageUrl().isEmpty()) {
                        String cacheKey = "product_" + product.getId();

                        if (!imageEmbeddingsCache.containsKey(cacheKey)) {
                            float[] embedding = imageEmbeddingService.extractImageFeaturesFromUrl(product.getImageUrl());
                            imageEmbeddingsCache.put(cacheKey, embedding);
                            processedCount++;
                        }
                    }
                } catch (Exception e) {
                    logger.warn("⚠️ [IMAGE-SEARCH] Erreur embedding produit {}: {}",
                            product.getId(), e.getMessage());
                }
            }

            logger.info("✅ [IMAGE-SEARCH] {} embeddings produits pré-calculés", processedCount);

        } catch (Exception e) {
            logger.error("❌ [IMAGE-SEARCH] Erreur pré-calcul embeddings: {}", e.getMessage());
        }
    }

    private List<SimilarImageResult> findSimilarImages(float[] queryEmbedding, String searchType) {
        List<SimilarImageResult> results = new ArrayList<>();

        try {
            if ("PRODUIT".equals(searchType) || "MIXED".equals(searchType)) {
                results.addAll(findSimilarProducts(queryEmbedding));
            }

            if ("BOUTIQUE".equals(searchType) || "MIXED".equals(searchType)) {
                results.addAll(findSimilarBoutiques(queryEmbedding));
            }

            // Trier par score de similarité décroissant
            results.sort((a, b) -> Double.compare(b.getSimilarityScore(), a.getSimilarityScore()));

            // Limiter les résultats
            return results.stream()
                    .filter(result -> result.getSimilarityScore() >= similarityThreshold)
                    .limit(maxResults)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            logger.error("❌ [IMAGE-SEARCH] Erreur recherche similarité: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private List<SimilarImageResult> findSimilarProducts(float[] queryEmbedding) {
        List<SimilarImageResult> similarProducts = new ArrayList<>();

        for (Map.Entry<String, float[]> entry : imageEmbeddingsCache.entrySet()) {
            if (entry.getKey().startsWith("product_")) {
                double similarity = calculateCosineSimilarity(queryEmbedding, entry.getValue());

                if (similarity >= similarityThreshold) {
                    String productId = entry.getKey().replace("product_", "");

                    try {
                        Optional<ProduitDTO> optionalProduct = produitService.getProduitById(Long.parseLong(productId));
                        if (optionalProduct.isPresent()) {
                            ProduitDTO product = optionalProduct.get();

                            SimilarImageResult result = new SimilarImageResult();
                            result.setProductId(productId);
                            result.setProductName(product.getNomProduit());
                            result.setImageUrl(product.getImageUrl());
                            result.setSimilarityScore(similarity);
                            result.setPrice(product.getPrix() != null ? product.getPrix().doubleValue() : null);
                            result.setCategory(product.getIdCategorie() != null ? product.getIdCategorie().toString() : "");
                            result.setType("PRODUIT");

                            similarProducts.add(result);
                        }
                    } catch (Exception e) {
                        logger.warn("⚠️ [IMAGE-SEARCH] Erreur récupération produit {}: {}", productId, e.getMessage());
                    }
                }
            }
        }

        return similarProducts;
    }


    private List<SimilarImageResult> findSimilarBoutiques(float[] queryEmbedding) {
        List<SimilarImageResult> similarBoutiques = new ArrayList<>();

        // Logique similaire pour les boutiques
        // À implémenter selon vos besoins

        return similarBoutiques;
    }

    private double calculateCosineSimilarity(float[] vectorA, float[] vectorB) {
        if (vectorA.length != vectorB.length) {
            throw new IllegalArgumentException("Les vecteurs doivent avoir la même taille");
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < vectorA.length; i++) {
            dotProduct += vectorA[i] * vectorB[i];
            normA += Math.pow(vectorA[i], 2);
            normB += Math.pow(vectorB[i], 2);
        }

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private float[] getOrComputeProductImageEmbedding(ProduitDTO product) throws Exception {
        String cacheKey = "product_" + product.getId();

        if (imageEmbeddingsCache.containsKey(cacheKey)) {
            return imageEmbeddingsCache.get(cacheKey);
        }

        // Calculer l'embedding si pas en cache
        if (product.getImageUrl() != null && !product.getImageUrl().isEmpty()) {
            float[] embedding = imageEmbeddingService.extractImageFeaturesFromUrl(product.getImageUrl());
            imageEmbeddingsCache.put(cacheKey, embedding);
            return embedding;
        }

        throw new Exception("Aucune image disponible pour le produit " + product.getId());
    }

    private ImageSearchResponse buildSearchResponse(List<SimilarImageResult> similarImages,
                                                    String queryImage, String searchType) {
        ImageSearchResponse response = new ImageSearchResponse();
        response.setQueryImage(queryImage);
        response.setSearchType(searchType);
        response.setSimilarProducts(similarImages);
        response.setTotalResults(similarImages.size());
        response.setTimestamp(new Date());
        response.setSuccess(true);

        // Ajouter des statistiques
        Map<String, Object> stats = new HashMap<>();
        stats.put("averageSimilarity", calculateAverageSimilarity(similarImages));
        stats.put("topSimilarity", similarImages.isEmpty() ? 0.0 : similarImages.get(0).getSimilarityScore());
        stats.put("categoriesFound", getCategoriesFromResults(similarImages));
        response.setStatistics(stats);

        return response;
    }

    private void enrichWithAIAnalysis(ImageSearchResponse response, MultipartFile imageFile) {
        try {
            Map<String, Object> aiAnalysis = analyzeImageWithAI(imageFile);
            response.setAiAnalysis(aiAnalysis);

            // Ajouter des recommandations basées sur l'analyse
            String aiDescription = (String) aiAnalysis.get("aiDescription");
            if (aiDescription != null) {
                List<String> recommendations = generateRecommendationsFromAI(aiDescription);
                response.setRecommendations(recommendations);
            }

        } catch (Exception e) {
            logger.warn("⚠️ [IMAGE-SEARCH] Erreur enrichissement IA: {}", e.getMessage());
        }
    }

    private List<String> generateRecommendationsFromAI(String aiDescription) {
        List<String> recommendations = new ArrayList<>();

        // Analyser la description IA pour générer des recommandations
        if (aiDescription.toLowerCase().contains("vêtement")) {
            recommendations.add("Explorez notre collection de vêtements");
            recommendations.add("Découvrez les tendances mode");
        }

        if (aiDescription.toLowerCase().contains("électronique")) {
            recommendations.add("Consultez nos derniers gadgets tech");
            recommendations.add("Comparez les prix avant d'acheter");
        }

        if (recommendations.isEmpty()) {
            recommendations.add("Affinez votre recherche avec des filtres");
            recommendations.add("Consultez des produits similaires");
        }

        return recommendations;
    }

    private void validateImageFile(MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new Exception("Fichier image requis");
        }

        if (file.getSize() > 10 * 1024 * 1024) { // 10MB max
            throw new Exception("Fichier trop volumineux (max 10MB)");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new Exception("Type de fichier non supporté: " + contentType);
        }
    }

    private InputStream downloadImageFromUrl(String imageUrl) throws Exception {
        // Implémentation pour télécharger l'image depuis l'URL
        // Peut utiliser RestTemplate ou un client HTTP
        throw new UnsupportedOperationException("Téléchargement URL à implémenter");
    }

    private String getImageFormat(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType != null) {
            return contentType.substring(contentType.lastIndexOf("/") + 1);
        }
        return "unknown";
    }

    private List<String> extractDominantColors(BufferedImage image) {
        // Implémentation simplifiée - à améliorer avec une vraie analyse des couleurs
        List<String> colors = new ArrayList<>();
        colors.add("#RGB_DOMINANT_1");
        colors.add("#RGB_DOMINANT_2");
        colors.add("#RGB_DOMINANT_3");
        return colors;
    }

    private String classifyImageCategory(String aiDescription) {
        if (aiDescription == null) return "general";

        String desc = aiDescription.toLowerCase();
        if (desc.contains("vêtement") || desc.contains("clothing")) return "vetements";
        if (desc.contains("électronique") || desc.contains("electronic")) return "electronique";
        if (desc.contains("sport")) return "sport";
        if (desc.contains("beauté") || desc.contains("beauty")) return "beaute";
        if (desc.contains("maison") || desc.contains("home")) return "maison";

        return "general";
    }

    private List<String> extractTagsFromDescription(String description) {
        if (description == null) return new ArrayList<>();

        // Extraction simple de tags - à améliorer avec NLP
        return Arrays.stream(description.split("\\s+"))
                .filter(word -> word.length() > 3)
                .limit(10)
                .collect(Collectors.toList());
    }

    private double calculateAverageSimilarity(List<SimilarImageResult> results) {
        return results.stream()
                .mapToDouble(SimilarImageResult::getSimilarityScore)
                .average()
                .orElse(0.0);
    }

    private Set<String> getCategoriesFromResults(List<SimilarImageResult> results) {
        return results.stream()
                .map(SimilarImageResult::getCategory)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private ImageSearchResponse createErrorResponse(String queryImage, String searchType, Exception e) {
        ImageSearchResponse response = new ImageSearchResponse();
        response.setQueryImage(queryImage);
        response.setSearchType(searchType);
        response.setSimilarProducts(new ArrayList<>());
        response.setTotalResults(0);
        response.setSuccess(false);
        response.setErrorMessage(e.getMessage());
        response.setTimestamp(new Date());
        return response;
    }

    // =============== MÉTHODES PUBLIQUES DE GESTION ===============

    public void clearCache() {
        imageEmbeddingsCache.clear();
        searchResultsCache.clear();
        logger.info("🗑️ [IMAGE-SEARCH] Cache vidé");
    }

    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("embeddingsCacheSize", imageEmbeddingsCache.size());
        stats.put("searchResultsCacheSize", searchResultsCache.size());
        stats.put("similarityThreshold", similarityThreshold);
        stats.put("maxResults", maxResults);
        stats.put("cacheEnabled", cacheEnabled);
        return stats;
    }

    public void refreshProductEmbeddings() {
        logger.info("🔄 [IMAGE-SEARCH] Actualisation des embeddings produits...");
        imageEmbeddingsCache.entrySet().removeIf(entry -> entry.getKey().startsWith("product_"));
        precomputeProductImageEmbeddings();
    }
}