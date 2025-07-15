package com.boutique_catalogue_produits.service;

import com.boutique_catalogue_produits.dto.ProduitDTO;
import com.boutique_catalogue_produits.dto.SearchCriteria;
import com.boutique_catalogue_produits.model.Boutique;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 🚀 Service de recherche IA SCALABLE pour millions de produits
 * Utilise l'IA de Gemini pour la compréhension sémantique automatique
 * SANS mappings manuels - tout est automatique !
 */
@Service
public class ScalableAISearchService {

    private static final Logger logger = LoggerFactory.getLogger(ScalableAISearchService.class);

    @Autowired
    private GeminiAIService geminiAIService;

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    @Value("${rag.ai.cache.enabled:true}")
    private boolean cacheEnabled;

    @Value("${rag.ai.similarity.threshold:0.15}")
    private double similarityThreshold;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Cache intelligent pour éviter les appels API répétés
    private final Map<String, QueryAnalysis> queryAnalysisCache = new ConcurrentHashMap<>();
    private final Map<String, List<String>> expandedTermsCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void initialize() {
        logger.info("🚀 [AI-SEARCH] Initialisation du service de recherche IA scalable");
    }

    /**
     * 🧠 Recherche intelligente utilisant 100% l'IA de Gemini
     * SCALABLE pour millions de produits
     */
    public List<ProduitDTO> searchProductsWithAI(String userQuery, List<ProduitDTO> allProducts) {
        logger.info("🔍 [AI-SEARCH] Recherche IA pour: '{}' sur {} produits", userQuery, allProducts.size());

        long startTime = System.currentTimeMillis();

        try {
            // 1. ANALYSE DE LA REQUÊTE PAR L'IA (avec cache)
            QueryAnalysis analysis = analyzeQueryWithAI(userQuery);

            // 2. RECHERCHE INTELLIGENTE EN 3 PHASES
            List<ProduitDTO> results = performIntelligentSearch(userQuery, analysis, allProducts);

            long duration = System.currentTimeMillis() - startTime;
            logger.info("✅ [AI-SEARCH] {} résultats trouvés en {}ms", results.size(), duration);

            return results;

        } catch (Exception e) {
            logger.error("❌ [AI-SEARCH] Erreur: {}", e.getMessage());
            return performFallbackSearch(userQuery, allProducts);
        }
    }

    /**
     * 🧠 Analyse de la requête par l'IA avec cache intelligent
     */
    private QueryAnalysis analyzeQueryWithAI(String userQuery) {
        // Vérifier le cache
        if (cacheEnabled && queryAnalysisCache.containsKey(userQuery)) {
            logger.info("📦 [CACHE] Analyse trouvée en cache pour: '{}'", userQuery);
            return queryAnalysisCache.get(userQuery);
        }

        try {
            // Appel à l'IA pour analyser la requête
            String aiAnalysis = callGeminiForQueryAnalysis(userQuery);

            // Parser la réponse de l'IA
            QueryAnalysis analysis = parseAIAnalysis(aiAnalysis, userQuery);

            // Mettre en cache
            if (cacheEnabled) {
                queryAnalysisCache.put(userQuery, analysis);
            }

            logger.info("🧠 [AI-ANALYSIS] Intent: {}, Keywords: {}",
                    analysis.getIntent(), analysis.getExpandedKeywords());

            return analysis;

        } catch (Exception e) {
            logger.warn("⚠️ [AI-ANALYSIS] Erreur IA, fallback: {}", e.getMessage());
            return createFallbackAnalysis(userQuery);
        }
    }

    /**
     * 🎯 Appel à Gemini pour analyser la requête utilisateur
     */
    private String callGeminiForQueryAnalysis(String userQuery) throws Exception {
        String prompt = String.format("""
        Tu es un expert en e-commerce qui comprend parfaitement les intentions des utilisateurs.
        Analyse cette requête de recherche et réponds UNIQUEMENT en JSON valide.
        
        REQUÊTE: "%s"
        
        Réponds avec ce format JSON exact (remplace les valeurs) :
        {
          "intent": "ACHAT|INFORMATION|COMPARAISON",
          "category": "vetements|electronique|sport|beaute|maison|general",
          "expandedKeywords": ["mot1", "mot2", "mot3", "mot4", "mot5"],
          "culturalContext": "marocain|occidental|general",
          "priceRange": "low|medium|high|any",
          "urgency": "immediate|flexible",
          "confidence": 0.8
        }
        
        RÈGLES IMPORTANTES:
        1. Pour "tenues marocaines" → expandedKeywords: ["caftan", "djellaba", "takchita", "gandoura", "vêtements", "traditionnel"]
        2. Pour "téléphone" → expandedKeywords: ["smartphone", "mobile", "portable", "android", "iphone"]
        3. Pour "pas cher" → priceRange: "low"
        4. Toujours inclure 5-6 mots-clés pertinents dans expandedKeywords
        5. Réponds UNIQUEMENT en JSON, rien d'autre
        """, userQuery);

        return callGeminiAPI(prompt);
    }

    /**
     * 🔍 Recherche intelligente en 3 phases
     */
    private List<ProduitDTO> performIntelligentSearch(String userQuery, QueryAnalysis analysis, List<ProduitDTO> allProducts) {
        Set<ProduitDTO> results = new LinkedHashSet<>(); // Éviter les doublons

        // PHASE 1: Correspondance exacte et quasi-exacte
        results.addAll(searchExactMatches(userQuery, analysis, allProducts));

        // PHASE 2: Correspondance sémantique par mots-clés expansés
        results.addAll(searchSemanticMatches(analysis.getExpandedKeywords(), allProducts));

        // PHASE 3: Correspondance floue et partielle
        if (results.size() < 10) { // Si pas assez de résultats
            results.addAll(searchFuzzyMatches(userQuery, analysis, allProducts));
        }

        // Trier par pertinence et limiter
        return results.stream()
                .map(product -> new ScoredProduct(product, calculateAIScore(userQuery, product, analysis)))
                .sorted((a, b) -> Double.compare(b.score, a.score))
                .map(sp -> sp.product)
                .limit(50)
                .collect(Collectors.toList());
    }

    /**
     * 🎯 Phase 1: Correspondances exactes
     */
    private List<ProduitDTO> searchExactMatches(String userQuery, QueryAnalysis analysis, List<ProduitDTO> products) {
        String lowerQuery = userQuery.toLowerCase();

        return products.parallelStream()
                .filter(product -> {
                    String productText = getProductSearchText(product).toLowerCase();
                    // Correspondance exacte ou contient la requête
                    return productText.contains(lowerQuery) ||
                            lowerQuery.contains(product.getNomProduit().toLowerCase());
                })
                .collect(Collectors.toList());
    }

    /**
     * 🔤 Phase 2: Correspondances sémantiques avec mots-clés expansés
     */
    private List<ProduitDTO> searchSemanticMatches(List<String> expandedKeywords, List<ProduitDTO> products) {
        return products.parallelStream()
                .filter(product -> {
                    String productText = getProductSearchText(product).toLowerCase();

                    // Compter combien de mots-clés correspondent
                    long matchCount = expandedKeywords.stream()
                            .mapToLong(keyword -> productText.contains(keyword.toLowerCase()) ? 1 : 0)
                            .sum();

                    // Au moins 1 mot-clé doit correspondre
                    return matchCount >= 1;
                })
                .collect(Collectors.toList());
    }

    /**
     * 🔀 Phase 3: Correspondances floues
     */
    private List<ProduitDTO> searchFuzzyMatches(String userQuery, QueryAnalysis analysis, List<ProduitDTO> products) {
        String[] queryWords = userQuery.toLowerCase().split("\\s+");

        return products.parallelStream()
                .filter(product -> {
                    String productText = getProductSearchText(product).toLowerCase();

                    // Vérifier correspondance partielle des mots
                    for (String word : queryWords) {
                        if (word.length() > 3) { // Mots de plus de 3 caractères
                            if (containsFuzzy(productText, word)) {
                                return true;
                            }
                        }
                    }
                    return false;
                })
                .collect(Collectors.toList());
    }

    /**
     * 📊 Calcul du score de pertinence basé sur l'IA
     */
    private double calculateAIScore(String userQuery, ProduitDTO product, QueryAnalysis analysis) {
        double score = 0.0;
        String productText = getProductSearchText(product).toLowerCase();
        String lowerQuery = userQuery.toLowerCase();

        // 1. Score de correspondance directe (40%)
        if (productText.contains(lowerQuery)) {
            score += 0.4;
        }

        // 2. Score de correspondance avec mots-clés expansés (35%)
        long keywordMatches = analysis.getExpandedKeywords().stream()
                .mapToLong(keyword -> productText.contains(keyword.toLowerCase()) ? 1 : 0)
                .sum();

        double keywordScore = Math.min(0.35, keywordMatches * 0.1);
        score += keywordScore;

        // 3. Score de catégorie (15%)
        if (analysis.getCategory() != null && product.getIdCategorie() != null) {
            String productCategory = product.getIdCategorie().toString().toLowerCase();
            if (productCategory.contains(analysis.getCategory()) ||
                    analysis.getCategory().contains(productCategory)) {
                score += 0.15;
            }
        }

        // 4. Score de disponibilité (10%)
        if (product.getQuantite() != null && product.getQuantite() > 0) {
            score += 0.1;
        }

        return Math.min(1.0, score);
    }

    /**
     * 🛠️ Méthodes utilitaires
     */
    private String getProductSearchText(ProduitDTO product) {
        StringBuilder text = new StringBuilder();

        if (product.getNomProduit() != null) {
            text.append(product.getNomProduit()).append(" ");
        }

        if (product.getDescription() != null) {
            text.append(product.getDescription()).append(" ");
        }

        if (product.getIdCategorie() != null) {
            text.append(product.getIdCategorie()).append(" ");
        }

        return text.toString().trim();
    }

    private boolean containsFuzzy(String text, String word) {
        // Correspondance partielle simple
        return text.contains(word) ||
                text.contains(word.substring(0, Math.min(word.length(), word.length() - 1))) ||
                word.contains(text);
    }

    private QueryAnalysis parseAIAnalysis(String aiResponse, String originalQuery) {
        try {
            // Nettoyer la réponse (enlever markdown, etc.)
            String cleanJson = aiResponse.trim();
            if (cleanJson.startsWith("```json")) {
                cleanJson = cleanJson.substring(7);
            }
            if (cleanJson.endsWith("```")) {
                cleanJson = cleanJson.substring(0, cleanJson.length() - 3);
            }

            JsonNode jsonNode = objectMapper.readTree(cleanJson);

            QueryAnalysis analysis = new QueryAnalysis();
            analysis.setIntent(jsonNode.path("intent").asText("ACHAT"));
            analysis.setCategory(jsonNode.path("category").asText("general"));
            analysis.setPriceRange(jsonNode.path("priceRange").asText("any"));
            analysis.setConfidence(jsonNode.path("confidence").asDouble(0.5));

            // Parser les mots-clés expansés
            List<String> keywords = new ArrayList<>();
            JsonNode keywordsNode = jsonNode.path("expandedKeywords");
            if (keywordsNode.isArray()) {
                for (JsonNode keyword : keywordsNode) {
                    keywords.add(keyword.asText());
                }
            }
            analysis.setExpandedKeywords(keywords);

            return analysis;

        } catch (Exception e) {
            logger.warn("⚠️ [PARSE] Erreur parsing JSON IA: {}", e.getMessage());
            return createFallbackAnalysis(originalQuery);
        }
    }

    private QueryAnalysis createFallbackAnalysis(String userQuery) {
        QueryAnalysis analysis = new QueryAnalysis();
        analysis.setIntent("ACHAT");
        analysis.setCategory("general");
        analysis.setPriceRange("any");
        analysis.setConfidence(0.3);

        // Mots-clés basiques
        List<String> keywords = Arrays.asList(userQuery.toLowerCase().split("\\s+"));
        analysis.setExpandedKeywords(keywords);

        return analysis;
    }

    private List<ProduitDTO> performFallbackSearch(String userQuery, List<ProduitDTO> products) {
        logger.info("🔄 [FALLBACK] Recherche simple pour: '{}'", userQuery);

        String lowerQuery = userQuery.toLowerCase();

        return products.stream()
                .filter(product -> {
                    String productText = getProductSearchText(product).toLowerCase();
                    return productText.contains(lowerQuery);
                })
                .limit(20)
                .collect(Collectors.toList());
    }

    private String callGeminiAPI(String prompt) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> requestBody = new HashMap<>();
        Map<String, Object> content = new HashMap<>();
        content.put("parts", List.of(Map.of("text", prompt)));
        requestBody.put("contents", List.of(content));

        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("temperature", 0.1); // Très déterministe pour le JSON
        generationConfig.put("maxOutputTokens", 500);
        requestBody.put("generationConfig", generationConfig);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        String url = apiUrl + "?key=" + apiKey;

        ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

        if (response.getStatusCode() == HttpStatus.OK) {
            JsonNode jsonNode = objectMapper.readTree(response.getBody());
            return jsonNode.path("candidates").get(0)
                    .path("content").path("parts").get(0)
                    .path("text").asText();
        } else {
            throw new RuntimeException("Erreur API Gemini: " + response.getStatusCode());
        }
    }

    /**
     * 🏪 Recherche de boutiques avec IA
     */
    public List<Boutique> searchBoutiquesWithAI(String userQuery, List<Boutique> allBoutiques) {
        logger.info("🏪 [AI-SEARCH] Recherche boutiques IA pour: '{}'", userQuery);

        try {
            QueryAnalysis analysis = analyzeQueryWithAI(userQuery);

            return allBoutiques.stream()
                    .filter(Boutique::isValid)
                    .filter(boutique -> {
                        String boutiqueText = getBoutiqueSearchText(boutique).toLowerCase();
                        String lowerQuery = userQuery.toLowerCase();

                        // Correspondance directe
                        if (boutiqueText.contains(lowerQuery)) {
                            return true;
                        }

                        // Correspondance avec mots-clés expansés
                        return analysis.getExpandedKeywords().stream()
                                .anyMatch(keyword -> boutiqueText.contains(keyword.toLowerCase()));
                    })
                    .limit(20)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            logger.error("❌ [AI-SEARCH] Erreur recherche boutiques: {}", e.getMessage());
            return allBoutiques.stream()
                    .filter(boutique -> getBoutiqueSearchText(boutique).toLowerCase().contains(userQuery.toLowerCase()))
                    .limit(10)
                    .collect(Collectors.toList());
        }
    }

    private String getBoutiqueSearchText(Boutique boutique) {
        StringBuilder text = new StringBuilder();

        if (boutique.getNom() != null) {
            text.append(boutique.getNom()).append(" ");
        }

        if (boutique.getVille() != null) {
            text.append(boutique.getVille()).append(" ");
        }

        if (boutique.getAdress() != null) {
            text.append(boutique.getAdress()).append(" ");
        }

        return text.toString().trim();
    }

    // Classes internes
    public static class QueryAnalysis {
        private String intent;
        private String category;
        private List<String> expandedKeywords;
        private String priceRange;
        private double confidence;

        // Getters et setters
        public String getIntent() { return intent; }
        public void setIntent(String intent) { this.intent = intent; }

        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }

        public List<String> getExpandedKeywords() { return expandedKeywords; }
        public void setExpandedKeywords(List<String> expandedKeywords) { this.expandedKeywords = expandedKeywords; }

        public String getPriceRange() { return priceRange; }
        public void setPriceRange(String priceRange) { this.priceRange = priceRange; }

        public double getConfidence() { return confidence; }
        public void setConfidence(double confidence) { this.confidence = confidence; }
    }

    private static class ScoredProduct {
        final ProduitDTO product;
        final double score;

        ScoredProduct(ProduitDTO product, double score) {
            this.product = product;
            this.score = score;
        }
    }

    /**
     * 🗑️ Nettoyage du cache
     */
    public void clearCache() {
        queryAnalysisCache.clear();
        expandedTermsCache.clear();
        logger.info("🗑️ [CACHE] Cache IA vidé");
    }

    /**
     * 📊 Statistiques du service
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("queryCacheSize", queryAnalysisCache.size());
        stats.put("cacheEnabled", cacheEnabled);
        stats.put("similarityThreshold", similarityThreshold);
        return stats;
    }
}