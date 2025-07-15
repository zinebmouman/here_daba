package com.boutique_catalogue_produits.util;

import com.boutique_catalogue_produits.dto.RagSearchRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class RAGValidator {

    private static final Logger logger = LoggerFactory.getLogger(RAGValidator.class);

    // Mots interdits ou dangereux
    private static final List<String> FORBIDDEN_PATTERNS = Arrays.asList(
            "<script>", "javascript:", "eval(", "alert(", "document.cookie",
            "window.location", "innerHTML", "outerHTML"
    );

    // Caractères autorisés (lettres, chiffres, espaces, caractères arabes/français)
    private static final Pattern ALLOWED_PATTERN = Pattern.compile("^[\\p{L}\\p{N}\\s\\-.,!?'\"()]+$");

    // Longueurs maximales
    private static final int MAX_QUERY_LENGTH = 1000;
    private static final int MAX_VILLE_LENGTH = 100;
    private static final int MAX_CATEGORIE_LENGTH = 100;

    /**
     * Valider une requête de recherche RAG
     */
    public ValidationResult validateSearchRequest(RagSearchRequest request) {
        logger.debug("🔍 [RAG] Validation de la requête: {}", request.getQuery());

        ValidationResult result = new ValidationResult();

        try {
            // 1. VALIDATION QUERY OBLIGATOIRE
            if (request.getQuery() == null || request.getQuery().trim().isEmpty()) {
                result.addError("La requête de recherche est obligatoire");
                return result;
            }

            // 2. VALIDATION LONGUEUR
            String query = request.getQuery().trim();
            if (query.length() > MAX_QUERY_LENGTH) {
                result.addError("Requête trop longue (max " + MAX_QUERY_LENGTH + " caractères)");
            }

            // 3. VALIDATION CARACTÈRES DANGEREUX
            for (String forbidden : FORBIDDEN_PATTERNS) {
                if (query.toLowerCase().contains(forbidden.toLowerCase())) {
                    result.addError("Caractères interdits détectés: " + forbidden);
                }
            }

            // 4. VALIDATION PATTERN GÉNÉRAL
            if (!ALLOWED_PATTERN.matcher(query).matches()) {
                result.addWarning("Caractères spéciaux détectés - nettoyage automatique appliqué");
                request.setQuery(cleanQuery(query));
            }

            // 5. VALIDATION TYPE DE RECHERCHE
            if (request.getSearchType() != null) {
                String searchType = request.getSearchType().toUpperCase();
                if (!Arrays.asList("PRODUIT", "BOUTIQUE", "MIXED").contains(searchType)) {
                    result.addWarning("Type de recherche invalide - utilisation de MIXED par défaut");
                    request.setSearchType("MIXED");
                }
            } else {
                request.setSearchType("MIXED");
            }

            // 6. VALIDATION VILLE
            if (request.getVille() != null) {
                if (request.getVille().length() > MAX_VILLE_LENGTH) {
                    result.addError("Nom de ville trop long (max " + MAX_VILLE_LENGTH + " caractères)");
                }
                request.setVille(cleanText(request.getVille()));
            }

            // 7. VALIDATION CATÉGORIE
            if (request.getCategorie() != null) {
                if (request.getCategorie().length() > MAX_CATEGORIE_LENGTH) {
                    result.addError("Nom de catégorie trop long (max " + MAX_CATEGORIE_LENGTH + " caractères)");
                }
                request.setCategorie(cleanText(request.getCategorie()));
            }

            // 8. VALIDATION PRIX
            if (request.getPrixMin() != null && request.getPrixMax() != null) {
                if (request.getPrixMin() > request.getPrixMax()) {
                    result.addError("Prix minimum ne peut pas être supérieur au prix maximum");
                }
                if (request.getPrixMin() < 0) {
                    result.addError("Prix minimum ne peut pas être négatif");
                }
            }

            // 9. VALIDATION RAYON
            if (request.getRayonKm() != null) {
                if (request.getRayonKm() < 0 || request.getRayonKm() > 1000) {
                    result.addError("Rayon doit être entre 0 et 1000 km");
                }
            }

            // 10. CORRECTION AUTOMATIQUE DES ERREURS MINEURES
            autoCorrectRequest(request, result);

            if (result.hasErrors()) {
                logger.warn("⚠️ [RAG] Validation échouée: {} erreurs", result.getErrors().size());
            } else {
                logger.debug("✅ [RAG] Validation réussie");
            }

        } catch (Exception e) {
            logger.error("❌ [RAG] Erreur lors de la validation: {}", e.getMessage());
            result.addError("Erreur de validation: " + e.getMessage());
        }

        return result;
    }

    /**
     * Nettoyer une requête de recherche
     */
    public String cleanQuery(String query) {
        if (query == null) return "";

        // Supprimer les caractères dangereux
        for (String forbidden : FORBIDDEN_PATTERNS) {
            query = query.replaceAll("(?i)" + Pattern.quote(forbidden), "");
        }

        // Nettoyer les caractères spéciaux en conservant les accents
        query = query.replaceAll("[^\\p{L}\\p{N}\\s\\-.,!?'\"()]", " ");

        // Normaliser les espaces
        query = query.replaceAll("\\s+", " ").trim();

        return query;
    }

    /**
     * Nettoyer un texte général
     */
    public String cleanText(String text) {
        if (text == null) return null;

        return text.replaceAll("[<>\"'&]", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    /**
     * Correction automatique des erreurs mineures
     */
    private void autoCorrectRequest(RagSearchRequest request, ValidationResult result) {
        // Correction de la requête vide après nettoyage
        if (request.getQuery().trim().isEmpty()) {
            request.setQuery("recherche générale");
            result.addWarning("Requête vide corrigée en 'recherche générale'");
        }

        // Correction des prix négatifs
        if (request.getPrixMin() != null && request.getPrixMin() < 0) {
            request.setPrixMin(0.0);
            result.addWarning("Prix minimum corrigé à 0");
        }

        // Correction du rayon par défaut
        if (request.getRayonKm() == null && request.getVille() != null) {
            request.setRayonKm(50); // 50km par défaut
            result.addInfo("Rayon par défaut de 50km appliqué pour la ville");
        }
    }

    /**
     * Classe pour les résultats de validation
     */
    public static class ValidationResult {
        private List<String> errors = new java.util.ArrayList<>();
        private List<String> warnings = new java.util.ArrayList<>();
        private List<String> infos = new java.util.ArrayList<>();

        public void addError(String error) { errors.add(error); }
        public void addWarning(String warning) { warnings.add(warning); }
        public void addInfo(String info) { infos.add(info); }

        public boolean hasErrors() { return !errors.isEmpty(); }
        public boolean hasWarnings() { return !warnings.isEmpty(); }
        public boolean isValid() { return errors.isEmpty(); }

        public List<String> getErrors() { return errors; }
        public List<String> getWarnings() { return warnings; }
        public List<String> getInfos() { return infos; }

        @Override
        public String toString() {
            return "ValidationResult{" +
                    "errors=" + errors.size() +
                    ", warnings=" + warnings.size() +
                    ", infos=" + infos.size() +
                    '}';
        }
    }
}