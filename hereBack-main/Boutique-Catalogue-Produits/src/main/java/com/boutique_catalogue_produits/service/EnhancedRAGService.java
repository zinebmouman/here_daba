package com.boutique_catalogue_produits.service;

import com.boutique_catalogue_produits.dto.*;
import com.boutique_catalogue_produits.model.Boutique;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class EnhancedRAGService {

    private static final Logger logger = LoggerFactory.getLogger(EnhancedRAGService.class);

    @Autowired
    private GeminiAIService geminiAIService;

    @Autowired
    private ProduitService produitService;

    @Autowired
    private BoutiqueService boutiqueService;

    @Value("${rag.semantic.threshold:0.82}")
    private double semanticThreshold;


    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RagSearchResponse semanticSearch(String query, String searchType) {
        logger.info("🧠 Recherche sémantique avancée: {}", query);

        try {
            // 1. Analyse sémantique avancée avec Gemini
            SearchCriteria criteria = geminiAIService.analyzeSearchQuery(query);

            // 2. Génération d'embeddings pour la requête
            List<Double> queryEmbedding = geminiAIService.generateEmbedding(query);

            // 3. Récupération des données
            List<ProduitDTO> produits = produitService.getAllProduits();
            List<Boutique> boutiques = boutiqueService.getAllBoutiques();

            // 4. Filtrage sémantique
            List<ProduitDTO> relevantProduits = filterProductsSemantically(produits, queryEmbedding, criteria);
            List<Boutique> relevantBoutiques = filterBoutiquesSemantically(boutiques, queryEmbedding, criteria);

            // 5. Génération de la réponse enrichie
            RagSearchResponse response = buildEnhancedResponse(query, searchType, relevantProduits, relevantBoutiques);

            // 6. Analyse IA supplémentaire
            enhanceWithAIInsights(response, query, relevantProduits, relevantBoutiques);

            return response;

        } catch (Exception e) {
            logger.error("Erreur recherche sémantique: {}", e.getMessage());
            return createFallbackResponse(query, searchType);
        }
    }

    private List<ProduitDTO> filterProductsSemantically(List<ProduitDTO> produits, List<Double> queryEmbedding, SearchCriteria criteria) {
        return produits.stream()
                .filter(p -> isProductSemanticallyRelevant(p, queryEmbedding, criteria))
                .sorted((p1, p2) -> Double.compare(
                        calculateSemanticRelevance(p2, queryEmbedding),
                        calculateSemanticRelevance(p1, queryEmbedding)
                ))
                .limit(50)
                .collect(Collectors.toList());
    }

    private List<Boutique> filterBoutiquesSemantically(List<Boutique> boutiques, List<Double> queryEmbedding, SearchCriteria criteria) {
        return boutiques.stream()
                .filter(b -> isBoutiqueSemanticallyRelevant(b, queryEmbedding, criteria))
                .sorted((b1, b2) -> Double.compare(
                        calculateBoutiqueSemanticRelevance(b2, queryEmbedding),
                        calculateBoutiqueSemanticRelevance(b1, queryEmbedding)
                ))
                .limit(20)
                .collect(Collectors.toList());
    }

    private boolean isProductSemanticallyRelevant(ProduitDTO produit, List<Double> queryEmbedding, SearchCriteria criteria) {
        try {
            // 1. Vérifier la similarité sémantique
            double semanticScore = calculateSemanticRelevance(produit, queryEmbedding);
            if (semanticScore < semanticThreshold) {
                return false;
            }

            // 2. Vérifier les critères supplémentaires (avec conversion BigDecimal)
            if (criteria.getPrixMin() != null &&
                    produit.getPrix().compareTo(BigDecimal.valueOf(criteria.getPrixMin())) < 0) {
                return false;
            }

            if (criteria.getPrixMax() != null &&
                    produit.getPrix().compareTo(BigDecimal.valueOf(criteria.getPrixMax())) > 0) {
                return false;
            }

            if (criteria.getCategorie() != null &&
                    !produit.getIdCategorie().equalsIgnoreCase(criteria.getCategorie())) {
                return false;
            }

            return true;

        } catch (Exception e) {
            logger.error("Erreur vérification pertinence sémantique: {}", e.getMessage());
            return false;
        }
    }

    private boolean isBoutiqueSemanticallyRelevant(Boutique boutique, List<Double> queryEmbedding, SearchCriteria criteria) {
        try {
            // 1. Vérifier la similarité sémantique
            double semanticScore = calculateBoutiqueSemanticRelevance(boutique, queryEmbedding);
            if (semanticScore < semanticThreshold) {
                return false;
            }

            // 2. Vérifier les critères supplémentaires
            if (criteria.getVille() != null &&
                    !criteria.getVille().equalsIgnoreCase(boutique.getVille())) {
                return false;
            }

            return true;

        } catch (Exception e) {
            logger.error("Erreur vérification pertinence boutique: {}", e.getMessage());
            return false;
        }
    }

    private double calculateSemanticRelevance(ProduitDTO produit, List<Double> queryEmbedding) {
        try {
            // Générer ou récupérer l'embedding du produit
            List<Double> productEmbedding = getProductEmbedding(produit);

            // Calculer la similarité cosinus
            return cosineSimilarity(queryEmbedding, productEmbedding);
        } catch (Exception e) {
            logger.error("Erreur calcul similarité: {}", e.getMessage());
            return 0;
        }
    }

    private double calculateBoutiqueSemanticRelevance(Boutique boutique, List<Double> queryEmbedding) {
        try {
            // Générer l'embedding de la boutique
            List<Double> boutiqueEmbedding = getBoutiqueEmbedding(boutique);

            // Calculer la similarité cosinus
            return cosineSimilarity(queryEmbedding, boutiqueEmbedding);
        } catch (Exception e) {
            logger.error("Erreur calcul similarité boutique: {}", e.getMessage());
            return 0;
        }
    }

    private List<Double> getProductEmbedding(ProduitDTO produit) {
        String productText = String.format("%s %s %s",
                produit.getNomProduit(),
                produit.getDescription(),
                produit.getIdCategorie());

        return geminiAIService.generateEmbedding(productText);
    }

    private List<Double> getBoutiqueEmbedding(Boutique boutique) {
        String boutiqueText = String.format("%s %s %s",
                boutique.getNom(),
                boutique.getVille(),
                boutique.getAdress());

        return geminiAIService.generateEmbedding(boutiqueText);
    }

    private double cosineSimilarity(List<Double> vectorA, List<Double> vectorB) {
        if (vectorA == null || vectorB == null || vectorA.size() != vectorB.size()) {
            return 0;
        }

        double dotProduct = 0;
        double normA = 0;
        double normB = 0;

        for (int i = 0; i < vectorA.size(); i++) {
            dotProduct += vectorA.get(i) * vectorB.get(i);
            normA += Math.pow(vectorA.get(i), 2);
            normB += Math.pow(vectorB.get(i), 2);
        }

        // Correction importante - ajout de parenthèses
        double denominator = (Math.sqrt(normA) * Math.sqrt(normB));
        return denominator != 0 ? dotProduct / denominator : 0;
    }

    private RagSearchResponse buildEnhancedResponse(String query, String searchType,
                                                    List<ProduitDTO> produits, List<Boutique> boutiques) {
        RagSearchResponse response = new RagSearchResponse();
        response.setQuery(query);
        response.setSearchType(searchType);
        response.setRagEnhanced(true);
        response.setTimestamp(new Date());

        response.setProduits(produits);
        response.setBoutiques(boutiques);
        response.setNombreProduits(produits.size());
        response.setNombreBoutiques(boutiques.size());
        response.setTotalResults(produits.size() + boutiques.size());

        return response;
    }

    private void enhanceWithAIInsights(RagSearchResponse response, String query,
                                       List<ProduitDTO> produits, List<Boutique> boutiques) {
        try {
            // Construire le contexte pour l'analyse
            StringBuilder context = new StringBuilder();
            context.append("Requête originale: ").append(query).append("\n\n");

            context.append("Produits pertinents trouvés (").append(produits.size()).append("):\n");
            produits.forEach(p -> context.append("- ").append(p.getNomProduit()).append(" (").append(p.getPrix()).append(" DH)\n"));

            context.append("\nBoutiques pertinentes trouvées (").append(boutiques.size()).append("):\n");
            boutiques.forEach(b -> context.append("- ").append(b.getNom()).append(" (").append(b.getVille()).append(")\n"));

            // Appeler Gemini pour une analyse avancée
            String analysis = geminiAIService.generateAdvancedAnalysis(query, context.toString());
            response.setAnalysis(analysis);

            // Générer des recommandations
            String recommendations = geminiAIService.generateRecommendations(query, produits, boutiques);
            response.setSummary(recommendations);

        } catch (Exception e) {
            logger.error("Erreur enrichissement IA: {}", e.getMessage());
            response.setAnalysis("Analyse basique: " + response.getTotalResults() + " résultats trouvés");
            response.setSummary("Essayez d'affiner votre recherche");
        }
    }

    private RagSearchResponse createFallbackResponse(String query, String searchType) {
        RagSearchResponse response = new RagSearchResponse();
        response.setQuery(query);
        response.setSearchType(searchType);
        response.setRagEnhanced(false);
        response.setTimestamp(new Date());
        response.setAnalysis("Recherche basique effectuée");
        response.setSummary("Service IA temporairement indisponible");

        try {
            // Fallback simple basé sur le texte
            List<ProduitDTO> produits = produitService.getAllProduits().stream()
                    .filter(p -> p.getNomProduit().toLowerCase().contains(query.toLowerCase()))
                    .limit(20)
                    .collect(Collectors.toList());

            List<Boutique> boutiques = boutiqueService.getAllBoutiques().stream()
                    .filter(b -> b.getNom().toLowerCase().contains(query.toLowerCase()))
                    .limit(10)
                    .collect(Collectors.toList());

            response.setProduits(produits);
            response.setBoutiques(boutiques);
            response.setNombreProduits(produits.size());
            response.setNombreBoutiques(boutiques.size());
            response.setTotalResults(produits.size() + boutiques.size());

        } catch (Exception e) {
            logger.error("Erreur fallback: {}", e.getMessage());
            response.setProduits(new ArrayList<>());
            response.setBoutiques(new ArrayList<>());
            response.setTotalResults(0);
        }

        return response;
    }
}