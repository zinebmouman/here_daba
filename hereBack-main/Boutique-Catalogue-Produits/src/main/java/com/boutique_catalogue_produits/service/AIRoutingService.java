package com.boutique_catalogue_produits.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Service
public class AIRoutingService {

    @Value("${ai.python.microservice.enabled:false}")
    private boolean pythonMicroserviceEnabled;

    @Value("${ai.recommendations.default.algorithm:current}")
    private String defaultRecommendationAlgorithm;

    public String chooseRecommendationEndpoint(String userId, Map<String, Object> context) {
        // Logique de routage intelligent
        if (!pythonMicroserviceEnabled) {
            return "current";
        }

        // Critères pour choisir l'algorithme
        if ("ml".equals(defaultRecommendationAlgorithm)) {
            return "ml";
        }

        if ("hybrid".equals(defaultRecommendationAlgorithm)) {
            return "hybrid";
        }

        return "current"; // Par défaut
    }

    public String chooseImageSearchEndpoint(MultipartFile image, String searchType) {
        if (!pythonMicroserviceEnabled) {
            return "gemini";
        }

        // Si l'image est complexe, utiliser CNN
        if (image.getSize() > 1024 * 1024) { // > 1MB
            return "cnn";
        }

        return "hybrid"; // Par défaut
    }
}