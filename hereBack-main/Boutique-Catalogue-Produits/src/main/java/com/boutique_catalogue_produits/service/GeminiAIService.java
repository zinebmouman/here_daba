package com.boutique_catalogue_produits.service;

import com.boutique_catalogue_produits.dto.PriceRange;
import com.boutique_catalogue_produits.dto.ProduitDTO;
import com.boutique_catalogue_produits.dto.SearchCriteria;
import com.boutique_catalogue_produits.model.Boutique;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class GeminiAIService {

    private static final Logger logger = LoggerFactory.getLogger(GeminiAIService.class);

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SearchCriteria analyzeSearchQuery(String userQuery) {
        logger.info("🔍 Analyse de la requête utilisateur: {}", userQuery);

        try {
            String prompt = createAdvancedSearchPrompt(userQuery);
            String geminiResponse = callGeminiAPI(prompt);
            SearchCriteria criteria = parseAdvancedGeminiResponse(geminiResponse);

            logger.info("✅ Critères générés: {}", criteria);
            return criteria;

        } catch (Exception e) {
            logger.error("❌ Erreur Gemini: {}", e.getMessage());
            return createFallbackCriteria(userQuery);
        }
    }

    private String createAdvancedSearchPrompt(String userQuery) {
        return String.format("""
        Tu es un assistant IA expert pour analyser des requêtes de recherche dans un marketplace marocain.
        Tu dois TOUJOURS essayer de trouver quelque chose de pertinent, même si ce n'est pas exact.
        
        RÈGLES IMPORTANTES:
        1. Ne jamais répondre "vide" - trouve toujours la meilleure approximation
        2. Si tu ne trouves pas exactement, propose ce qui est le plus proche
        3. Sois intelligent dans tes déductions
        4. Utilise tes connaissances pour deviner intelligemment
        
        Requête utilisateur: "%s"
        
        Réponds dans ce format (trouve TOUJOURS quelque chose de pertinent):
        
        === PRODUITS ===
        KEYWORDS: mots-clés produits les plus pertinents (même approchés)
        PRICE_MIN: prix minimum déduit (ou 0 si aucune indication)
        PRICE_MAX: prix maximum déduit (ou estime selon le produit)
        CATEGORY: catégorie la plus probable (electronique, vetements, sport, maison, informatique, automobile, beaute, livre, jeux, alimentaire)
        QUANTITY_MIN: quantité minimum (1 si mention stock, sinon 0)
        BRAND: marque détectée ou la plus probable
        IN_STOCK: true si mention disponibilité, false sinon
        
        === BOUTIQUES ===
        BOUTIQUE_NAME: nom exact de la boutique (ou approximatif si proche)
        VENDEUR_NAME: nom du vendeur (ou approximatif)
        
        === GÉOLOCALISATION ===
        VILLE: ville marocaine (Casablanca, Rabat, Marrakech, Fes, Tanger, Agadir, Meknes, Oujda, Kenitra, Tetouan, Safi, Mohammedia)
        QUARTIER: quartier ou zone
        PAYS: Maroc (par défaut pour ce marketplace)
        RAYON_KM: distance en km (5 par défaut si "près de moi")
        
        === TYPE RECHERCHE ===
        SEARCH_TYPE: PRODUIT (si cherche un produit) ou BOUTIQUE (si cherche une boutique) ou MIXED (les deux)
        
        EXEMPLES D'INTELLIGENCE:
        
        1) "telefon samsugn pas chère" (avec fautes) →
        KEYWORDS: téléphone,samsung
        PRICE_MAX: 1000
        CATEGORY: electronique
        BRAND: Samsung
        SEARCH_TYPE: PRODUIT
        
        2) "je cherche quelque chose pour mon ordinateur" →
        KEYWORDS: ordinateur,accessoire,périphérique
        CATEGORY: informatique
        SEARCH_TYPE: PRODUIT
        
        3) "boutique proche" →
        VILLE: Casablanca (ville par défaut au Maroc)
        RAYON_KM: 5
        SEARCH_TYPE: BOUTIQUE
        
        4) "trucs cool pour gamer" →
        KEYWORDS: gaming,jeux,accessoire
        CATEGORY: informatique
        SEARCH_TYPE: PRODUIT
        
        5) "cadeaux anniversaire fille 20 ans" →
        KEYWORDS: cadeau,femme,jeune
        CATEGORY: beaute
        SEARCH_TYPE: PRODUIT
        
        6) "magasin TechShop" →
        BOUTIQUE_NAME: TechShop
        SEARCH_TYPE: BOUTIQUE
        
        LOGIQUE INTELLIGENTE:
        - "pas cher" = PRICE_MAX: 1000
        - "cher" ou "haut de gamme" = PRICE_MIN: 2000
        - "gaming" = CATEGORY: informatique + KEYWORDS: gaming
        - "femme" = déduis catégorie (vêtements, beauté)
        - "homme" = déduis catégorie (vêtements, sport)
        - "enfant" = déduis catégorie (jeux, vêtements)
        - Noms de marques connus: Apple, Samsung, Nike, Adidas, Sony, HP, Dell, etc.
        - Si aucune ville mentionnée mais "près de moi" = VILLE: Casablanca (ville principale)
        
        CORRECTION AUTOMATIQUE DE FAUTES:
        - "telefon" → "téléphone"
        - "samsugn" → "samsung"
        - "ordinater" → "ordinateur"
        - "chausure" → "chaussure"
        - "voituer" → "voiture"
        
        SOIS CRÉATIF ET INTELLIGENT ! Trouve toujours quelque chose même si c'est approximatif.
        """, userQuery);
    }

    private SearchCriteria parseAdvancedGeminiResponse(String response) {
        SearchCriteria criteria = new SearchCriteria();

        logger.info("🔍 Parsing réponse Gemini: {}", response);

        try {
            // === PRODUITS ===
            criteria.setKeywords(extractAndCleanSmart(response, "KEYWORDS:"));
            criteria.setPrixMin(extractDoubleSmart(response, "PRICE_MIN:"));
            criteria.setPrixMax(extractPriceMaxSmart(response, "PRICE_MAX:"));
            criteria.setCategorie(extractAndCleanSmart(response, "CATEGORY:"));
            criteria.setQuantiteMin(extractIntegerSmart(response, "QUANTITY_MIN:"));
            criteria.setMarque(extractAndCleanSmart(response, "BRAND:"));
            criteria.setEnStock(extractBooleanSmart(response, "IN_STOCK:"));

            // === BOUTIQUES ===
            criteria.setBoutiqueName(extractAndCleanSmart(response, "BOUTIQUE_NAME:"));
            criteria.setVendeurName(extractAndCleanSmart(response, "VENDEUR_NAME:"));

            // === GÉOLOCALISATION ===
            criteria.setVille(extractAndCleanSmart(response, "VILLE:"));
            criteria.setQuartier(extractAndCleanSmart(response, "QUARTIER:"));
            criteria.setPays(extractAndCleanSmart(response, "PAYS:"));
            criteria.setRayonKm(extractIntegerSmart(response, "RAYON_KM:"));

            // === TYPE RECHERCHE ===
            criteria.setSearchType(extractAndCleanSmart(response, "SEARCH_TYPE:"));

            // Post-traitement intelligent
            criteria = applyIntelligentDefaults(criteria);

        } catch (Exception e) {
            logger.error("❌ Erreur parsing: {}", e.getMessage());
        }

        return criteria;
    }

    private String extractAndClean(String text, String key) {
        String value = extractValue(text, key);
        if (value.isEmpty() || value.equalsIgnoreCase("vide") || value.equalsIgnoreCase("null")) {
            return null;
        }
        return value.trim();
    }

    private Double extractDouble(String text, String key) {
        String value = extractAndClean(text, key);
        if (value == null) return null;

        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Double extractPriceMax(String text, String key) {
        String value = extractAndClean(text, key);
        if (value == null) return null;

        // Gérer les termes comme "économique", "pas cher"
        value = value.toLowerCase();
        if (value.contains("économique") || value.contains("pas cher") || value.contains("abordable")) {
            return 1000.0; // Prix max pour "pas cher"
        }

        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer extractInteger(String text, String key) {
        String value = extractAndClean(text, key);
        if (value == null) return null;

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
    private String extractAndCleanSmart(String text, String key) {
        String value = extractValue(text, key);

        // Ne pas accepter "vide", "null", "aucun", etc.
        if (value.isEmpty() ||
                value.equalsIgnoreCase("vide") ||
                value.equalsIgnoreCase("null") ||
                value.equalsIgnoreCase("aucun") ||
                value.equalsIgnoreCase("non") ||
                value.equalsIgnoreCase("rien")) {
            return null;
        }

        return value.trim();
    }
    private Double extractDoubleSmart(String text, String key) {
        String value = extractAndCleanSmart(text, key);
        if (value == null) return null;

        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            // Essayer de deviner un prix basé sur des mots
            value = value.toLowerCase();
            if (value.contains("élevé") || value.contains("cher")) {
                return 2000.0;
            }
            return null;
        }
    }

    private Double extractPriceMaxSmart(String text, String key) {
        String value = extractAndCleanSmart(text, key);
        if (value == null) return null;

        value = value.toLowerCase();

        // Gérer les termes intelligemment
        if (value.contains("économique") || value.contains("pas cher") || value.contains("abordable")) {
            return 1000.0;
        } else if (value.contains("moyen") || value.contains("raisonnable")) {
            return 3000.0;
        } else if (value.contains("élevé") || value.contains("premium") || value.contains("haut de gamme")) {
            return 10000.0;
        }

        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer extractIntegerSmart(String text, String key) {
        String value = extractAndCleanSmart(text, key);
        if (value == null) return null;

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            // Essayer de deviner selon le contexte
            value = value.toLowerCase();
            if (value.contains("proche") || value.contains("près")) {
                return 5; // 5km par défaut
            }
            return null;
        }
    }

    private Boolean extractBooleanSmart(String text, String key) {
        String value = extractAndCleanSmart(text, key);
        if (value == null) return false; // Par défaut false au lieu de null

        value = value.toLowerCase();
        return value.contains("true") || value.equals("1") ||
                value.contains("oui") || value.contains("disponible");
    }

    private SearchCriteria applyIntelligentDefaults(SearchCriteria criteria) {
        // Appliquer des valeurs par défaut intelligentes

        // Si aucun pays spécifié, c'est le Maroc
        if (criteria.getPays() == null) {
            criteria.setPays("Maroc");
        }

        // Si aucune ville mais rayon spécifié, utiliser Casablanca par défaut
        if (criteria.getVille() == null && criteria.getRayonKm() != null) {
            criteria.setVille("Casablanca");
        }

        // Si pas de type de recherche, deviner selon les critères
        if (criteria.getSearchType() == null) {
            if (criteria.getBoutiqueName() != null && criteria.getKeywords() == null) {
                criteria.setSearchType("BOUTIQUE");
            } else if (criteria.getBoutiqueName() != null && criteria.getKeywords() != null) {
                criteria.setSearchType("MIXED");
            } else {
                criteria.setSearchType("PRODUIT");
            }
        }

        return criteria;
    }


    private Boolean extractBoolean(String text, String key) {
        String value = extractAndClean(text, key);
        if (value == null) return null;

        return value.equalsIgnoreCase("true") || value.equals("1");
    }

    private String extractValue(String text, String key) {
        Pattern pattern = Pattern.compile(key + "\\s*([^\\n\\r]*)");
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "";
    }

    private String callGeminiAPI(String prompt) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> requestBody = new HashMap<>();
        Map<String, Object> content = new HashMap<>();
        content.put("parts", List.of(Map.of("text", prompt)));
        requestBody.put("contents", List.of(content));

        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("temperature", 0.1);
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

    private SearchCriteria createFallbackCriteria(String userQuery) {
        SearchCriteria criteria = new SearchCriteria();
        criteria.setKeywords(userQuery.toLowerCase());
        criteria.setSearchType("PRODUIT");
        logger.info("🔄 Fallback utilisé pour: {}", userQuery);
        return criteria;
    }

    public List<Double> generateEmbedding(String text) {
        try {
            String prompt = String.format("""
                Génère un embedding vectoriel pour le texte suivant.
                Retourne uniquement une liste de nombres en JSON, sans autre texte.
                
                Texte: "%s"
                """, text);

            String response = callGeminiAPI(prompt);
            JsonNode jsonNode = objectMapper.readTree(response);
            return objectMapper.convertValue(jsonNode, new TypeReference<List<Double>>() {});

        } catch (Exception e) {
            logger.error("Erreur génération embedding: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    public String generateAdvancedAnalysis(String query, String context) throws Exception {
        String prompt = String.format("""
            En tant qu'expert du e-commerce marocain, analyse cette recherche:
            
            Requête: %s
            Contexte: %s
            
            Donne une analyse structurée avec:
            1. Une interprétation de l'intention de recherche
            2. Les catégories les plus pertinentes
            3. Des conseils pour affiner si besoin
            4. Les tendances détectées
            """, query, context);

        return callGeminiAPI(prompt);
    }

    public String generateRecommendations(String query, List<ProduitDTO> produits, List<Boutique> boutiques) throws Exception {
        String productsStr = produits.stream()
                .map(p -> p.getNomProduit() + " (" + p.getPrix() + " DH)")
                .collect(Collectors.joining("\n- "));

        String boutiquesStr = boutiques.stream()
                .map(b -> b.getNom() + " - " + b.getVille())
                .collect(Collectors.joining("\n- "));

        String prompt = String.format("""
            Sur la base de la recherche "%s", génère des recommandations pertinentes.
            
            Produits disponibles:
            - %s
            
            Boutiques disponibles:
            - %s
            
            Suggestions:
            1. Recommande 3 produits similaires ou complémentaires
            2. Propose 2 boutiques pertinentes
            3. Donne des conseils d'achat
            """, query, productsStr, boutiquesStr);

        return callGeminiAPI(prompt);
    }

    // Extension du GeminiAIService existant pour supporter l'analyse d'images

// Ajouter ces méthodes à votre classe GeminiAIService existante

    /**
     * 🖼️ Analyser le contenu d'une image avec Gemini Vision
     */
    public String analyzeImageContent(MultipartFile imageFile) throws Exception {
        logger.info("🖼️ [GEMINI-VISION] Analyse contenu image: {}", imageFile.getOriginalFilename());

        try {
            // Convertir l'image en base64
            String base64Image = encodeImageToBase64(imageFile);

            String prompt = """
        Analysez cette image de produit en détail et fournissez une description structurée.
        
        Incluez dans votre analyse:
        1. Type de produit principal
        2. Couleurs dominantes
        3. Style et design
        4. Matériaux visibles
        5. Catégorie suggérée
        6. Mots-clés pour la recherche
        7. Public cible estimé
        
        Répondez en français avec un style professionnel pour un marketplace.
        """;

            return callGeminiVisionAPI(prompt, base64Image);

        } catch (Exception e) {
            logger.error("❌ [GEMINI-VISION] Erreur analyse image: {}", e.getMessage());
            throw new Exception("Erreur analyse Gemini Vision: " + e.getMessage(), e);
        }
    }

    /**
     * 🎯 Détecter la catégorie d'un produit depuis une image
     */
    public String detectProductCategory(MultipartFile imageFile) throws Exception {
        logger.info("🎯 [GEMINI-VISION] Détection catégorie image: {}", imageFile.getOriginalFilename());

        try {
            String base64Image = encodeImageToBase64(imageFile);

            String prompt = """
        Analysez cette image et déterminez la catégorie de produit la plus appropriée.
        
        Catégories disponibles:
        - electronique (téléphones, ordinateurs, gadgets)
        - vetements (vêtements, chaussures, accessoires mode)
        - sport (équipements sportifs, fitness)
        - beaute (cosmétiques, parfums, soins)
        - maison (décoration, électroménager, meubles)
        - automobile (pièces auto, accessoires voiture)
        - livre (livres, magazines)
        - jeux (jeux vidéo, jouets)
        - alimentaire (nourriture, boissons)
        
        Répondez uniquement avec le nom de la catégorie la plus appropriée.
        """;

            String response = callGeminiVisionAPI(prompt, base64Image);
            return response.toLowerCase().trim();

        } catch (Exception e) {
            logger.error("❌ [GEMINI-VISION] Erreur détection catégorie: {}", e.getMessage());
            return "general"; // Catégorie par défaut
        }
    }

    /**
     * 🏷️ Extraire les tags et mots-clés d'une image
     */
    public List<String> extractImageTags(MultipartFile imageFile) throws Exception {
        logger.info("🏷️ [GEMINI-VISION] Extraction tags image: {}", imageFile.getOriginalFilename());

        try {
            String base64Image = encodeImageToBase64(imageFile);

            String prompt = """
        Analysez cette image et extrayez 10-15 mots-clés pertinents pour la recherche.
        
        Concentrez-vous sur:
        - Type de produit
        - Marque (si visible)
        - Couleurs
        - Style
        - Matériaux
        - Caractéristiques distinctives
        
        Répondez avec une liste de mots-clés séparés par des virgules.
        Utilisez des termes en français couramment utilisés dans le e-commerce.
        """;

            String response = callGeminiVisionAPI(prompt, base64Image);

            // Parser la réponse en liste de tags
            return Arrays.stream(response.split(","))
                    .map(String::trim)
                    .filter(tag -> !tag.isEmpty())
                    .collect(Collectors.toList());

        } catch (Exception e) {
            logger.error("❌ [GEMINI-VISION] Erreur extraction tags: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 💰 Estimer la gamme de prix d'un produit depuis son image
     */
    public PriceRange estimatePriceFromImage(MultipartFile imageFile) throws Exception {
        logger.info("💰 [GEMINI-VISION] Estimation prix image: {}", imageFile.getOriginalFilename());

        try {
            String base64Image = encodeImageToBase64(imageFile);

            String prompt = """
        Analysez cette image de produit et estimez sa gamme de prix probable en dirhams marocains (DH).
        
        Considérez:
        - Type de produit
        - Qualité apparente
        - Marque (si identifiable)
        - Finition et matériaux
        - Complexité du produit
        
        Répondez au format JSON:
        {
          "prixMin": 100,
          "prixMax": 500,
          "prixMoyen": 300,
          "confiance": 0.8,
          "justification": "Produit de milieu de gamme basé sur..."
        }
        """;

            String response = callGeminiVisionAPI(prompt, base64Image);
            return parsePriceEstimation(response);

        } catch (Exception e) {
            logger.error("❌ [GEMINI-VISION] Erreur estimation prix: {}", e.getMessage());
            return new PriceRange(0.0, 1000.0, 500.0); // Gamme par défaut
        }
    }

    /**
     * 🔍 Recherche de produits similaires par description IA
     */
    public List<String> generateSimilarProductQueries(MultipartFile imageFile) throws Exception {
        logger.info("🔍 [GEMINI-VISION] Génération requêtes similaires: {}", imageFile.getOriginalFilename());

        try {
            String base64Image = encodeImageToBase64(imageFile);

            String prompt = """
        Analysez cette image et générez 5 requêtes de recherche différentes qui pourraient 
        aider à trouver des produits similaires.
        
        Variez les approches:
        1. Description directe du produit
        2. Recherche par catégorie et caractéristiques
        3. Recherche par style/design
        4. Recherche par usage/fonction
        5. Recherche par alternatives similaires
        
        Répondez avec une liste de requêtes séparées par des points-virgules.
        """;

            String response = callGeminiVisionAPI(prompt, base64Image);

            return Arrays.stream(response.split(";"))
                    .map(String::trim)
                    .filter(query -> !query.isEmpty())
                    .collect(Collectors.toList());

        } catch (Exception e) {
            logger.error("❌ [GEMINI-VISION] Erreur génération requêtes: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 🎨 Analyser le style et les tendances d'un produit
     */
    public Map<String, Object> analyzeProductStyle(MultipartFile imageFile) throws Exception {
        logger.info("🎨 [GEMINI-VISION] Analyse style produit: {}", imageFile.getOriginalFilename());

        try {
            String base64Image = encodeImageToBase64(imageFile);

            String prompt = """
        Analysez le style et les tendances de ce produit.
        
        Évaluez:
        - Style (moderne, vintage, classique, etc.)
        - Tendance actuelle (haute, moyenne, faible)
        - Public cible (âge, genre, style de vie)
        - Saison appropriée
        - Occasion d'usage
        
        Répondez au format JSON avec ces informations.
        """;

            String response = callGeminiVisionAPI(prompt, base64Image);
            return parseStyleAnalysis(response);

        } catch (Exception e) {
            logger.error("❌ [GEMINI-VISION] Erreur analyse style: {}", e.getMessage());
            return new HashMap<>();
        }
    }

// =============== MÉTHODES UTILITAIRES POUR GEMINI VISION ===============

    /**
     * 📸 Encoder une image en base64
     */
    private String encodeImageToBase64(MultipartFile imageFile) throws IOException {
        byte[] imageBytes = imageFile.getBytes();
        return Base64.getEncoder().encodeToString(imageBytes);
    }

    /**
     * 🔌 Appel API Gemini Vision
     */
    private String callGeminiVisionAPI(String prompt, String base64Image) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Construction de la requête pour Gemini Vision
        Map<String, Object> requestBody = new HashMap<>();

        // Contenu avec texte et image
        List<Map<String, Object>> parts = new ArrayList<>();
        parts.add(Map.of("text", prompt));
        parts.add(Map.of("inline_data", Map.of(
                "mime_type", "image/jpeg",
                "data", base64Image
        )));

        Map<String, Object> content = new HashMap<>();
        content.put("parts", parts);
        requestBody.put("contents", List.of(content));

        // Configuration de génération
        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("temperature", 0.1);
        generationConfig.put("maxOutputTokens", 1000);
        requestBody.put("generationConfig", generationConfig);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        // Utiliser l'endpoint Gemini Vision
        String visionUrl = apiUrl.replace("generateContent", "generateContent") + "?key=" + apiKey;

        ResponseEntity<String> response = restTemplate.postForEntity(visionUrl, entity, String.class);

        if (response.getStatusCode() == HttpStatus.OK) {
            JsonNode jsonNode = objectMapper.readTree(response.getBody());
            return jsonNode.path("candidates").get(0)
                    .path("content").path("parts").get(0)
                    .path("text").asText();
        } else {
            throw new RuntimeException("Erreur API Gemini Vision: " + response.getStatusCode());
        }
    }

    /**
     * 💰 Parser l'estimation de prix JSON
     */
    private PriceRange parsePriceEstimation(String jsonResponse) {
        try {
            // Nettoyer la réponse JSON
            String cleanJson = jsonResponse.trim();
            if (cleanJson.startsWith("```json")) {
                cleanJson = cleanJson.substring(7);
            }
            if (cleanJson.endsWith("```")) {
                cleanJson = cleanJson.substring(0, cleanJson.length() - 3);
            }

            JsonNode jsonNode = objectMapper.readTree(cleanJson);

            double prixMin = jsonNode.path("prixMin").asDouble(0.0);
            double prixMax = jsonNode.path("prixMax").asDouble(1000.0);
            double prixMoyen = jsonNode.path("prixMoyen").asDouble((prixMin + prixMax) / 2);

            return new PriceRange(prixMin, prixMax, prixMoyen);

        } catch (Exception e) {
            logger.warn("⚠️ [GEMINI-VISION] Erreur parsing prix: {}", e.getMessage());
            return new PriceRange(0.0, 1000.0, 500.0);
        }
    }

    /**
     * 🎨 Parser l'analyse de style JSON
     */
    private Map<String, Object> parseStyleAnalysis(String jsonResponse) {
        try {
            String cleanJson = jsonResponse.trim();
            if (cleanJson.startsWith("```json")) {
                cleanJson = cleanJson.substring(7);
            }
            if (cleanJson.endsWith("```")) {
                cleanJson = cleanJson.substring(0, cleanJson.length() - 3);
            }

            JsonNode jsonNode = objectMapper.readTree(cleanJson);

            Map<String, Object> styleInfo = new HashMap<>();
            styleInfo.put("style", jsonNode.path("style").asText("moderne"));
            styleInfo.put("tendance", jsonNode.path("tendance").asText("moyenne"));
            styleInfo.put("publicCible", jsonNode.path("publicCible").asText("general"));
            styleInfo.put("saison", jsonNode.path("saison").asText("toute"));
            styleInfo.put("occasion", jsonNode.path("occasion").asText("quotidienne"));

            return styleInfo;

        } catch (Exception e) {
            logger.warn("⚠️ [GEMINI-VISION] Erreur parsing style: {}", e.getMessage());
            return Map.of(
                    "style", "moderne",
                    "tendance", "moyenne",
                    "publicCible", "general"
            );
        }
    }

    /**
     * 🔄 Traitement par lots d'images
     */
    public List<Map<String, Object>> batchAnalyzeImages(List<MultipartFile> imageFiles) {
        logger.info("🔄 [GEMINI-VISION] Analyse par lots: {} images", imageFiles.size());

        List<Map<String, Object>> results = new ArrayList<>();

        for (MultipartFile imageFile : imageFiles) {
            try {
                Map<String, Object> analysis = new HashMap<>();
                analysis.put("fileName", imageFile.getOriginalFilename());
                analysis.put("description", analyzeImageContent(imageFile));
                analysis.put("category", detectProductCategory(imageFile));
                analysis.put("tags", extractImageTags(imageFile));
                analysis.put("priceRange", estimatePriceFromImage(imageFile));
                analysis.put("success", true);

                results.add(analysis);

            } catch (Exception e) {
                logger.error("❌ [GEMINI-VISION] Erreur analyse batch {}: {}",
                        imageFile.getOriginalFilename(), e.getMessage());

                Map<String, Object> errorResult = new HashMap<>();
                errorResult.put("fileName", imageFile.getOriginalFilename());
                errorResult.put("success", false);
                errorResult.put("error", e.getMessage());
                results.add(errorResult);
            }
        }

        return results;
    }

    /**
     * 🚀 Analyse rapide d'image (version optimisée)
     */
    public Map<String, Object> quickImageAnalysis(MultipartFile imageFile) throws Exception {
        logger.info("🚀 [GEMINI-VISION] Analyse rapide: {}", imageFile.getOriginalFilename());

        try {
            String base64Image = encodeImageToBase64(imageFile);

            String prompt = """
        Analyse rapide de cette image de produit.
        
        Fournissez UNIQUEMENT au format JSON:
        {
          "categorie": "nom_categorie",
          "description": "description courte",
          "mots_cles": ["mot1", "mot2", "mot3"],
          "prix_estime": 500,
          "confiance": 0.8
        }
        """;

            String response = callGeminiVisionAPI(prompt, base64Image);

            // Parser directement en Map
            String cleanJson = response.trim();
            if (cleanJson.startsWith("```json")) {
                cleanJson = cleanJson.substring(7);
            }
            if (cleanJson.endsWith("```")) {
                cleanJson = cleanJson.substring(0, cleanJson.length() - 3);
            }

            return objectMapper.readValue(cleanJson, Map.class);

        } catch (Exception e) {
            logger.error("❌ [GEMINI-VISION] Erreur analyse rapide: {}", e.getMessage());
            throw e;
        }
    }
}