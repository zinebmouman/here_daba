package com.boutique_catalogue_produits.service;

import com.boutique_catalogue_produits.client.StockServiceClient;
import com.boutique_catalogue_produits.dto.ProduitDTO;
import com.boutique_catalogue_produits.dto.ProduitImageDTO;
import com.boutique_catalogue_produits.dto.SearchCriteria;
import com.boutique_catalogue_produits.model.Categorie;
import com.boutique_catalogue_produits.model.Produit;
import com.boutique_catalogue_produits.model.ProduitImage;
import com.boutique_catalogue_produits.repository.CategorieRepository;
import com.boutique_catalogue_produits.repository.ProduitImageRepository;
import com.boutique_catalogue_produits.repository.ProduitRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProduitService {
    private static final Logger logger = LoggerFactory.getLogger(ProduitService.class);

    @Autowired
    private ProduitRepository produitRepository;
    @Autowired
    private ProduitImageRepository produitImageRepository;
    @Autowired
    private StockServiceClient stockClient;
    @Autowired
    private GeminiAIService geminiAIService;
    @Autowired
    private CategorieRepository categorieRepository;

    @Transactional(readOnly = true)
    public List<ProduitDTO> getAllProduits() {
        List<Produit> produits = produitRepository.findAll();
        return produits.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<ProduitDTO> getProduitById(Long id) {
        return produitRepository.findById(id)
                .map(this::convertToDTO);
    }

    @Transactional(readOnly = true)
    public List<ProduitDTO> getProduitsByStock(Long idStock) {
        return produitRepository.findByIdStock(idStock).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProduitDTO> getProduitsByCategorie(String idCategorie) {
        return produitRepository.findByIdCategorie(idCategorie).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProduitDTO> getProduitsEnAlerte() {
        logger.error("===== DEBUT RECHERCHE PRODUITS EN ALERTE =====");
        List<Produit> produitsEnAlerte = produitRepository.findProduitsEnAlerte();
        logger.error("Nombre de produits en alerte BRUT : {}", produitsEnAlerte.size());

        for (Produit produit : produitsEnAlerte) {
            logger.error("PRODUIT EN ALERTE DETAILS:");
            logger.error("ID: {}", produit.getId());
            logger.error("Nom: {}", produit.getNomProduit());
            logger.error("Quantité: {}", produit.getQuantite());
            logger.error("Seuil critique: {}", produit.getSeuilCritique());
        }

        List<ProduitDTO> produitsEnAlerteDTO = produitsEnAlerte.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        logger.error("Nombre de produits en alerte DTO : {}", produitsEnAlerteDTO.size());
        logger.error("===== FIN RECHERCHE PRODUITS EN ALERTE =====");
        return produitsEnAlerteDTO;
    }

    @Transactional(readOnly = true)
    public List<ProduitDTO> getProduitsProcheExpiration() {
        LocalDate now = LocalDate.now();
        LocalDate oneWeekLater = now.plusWeeks(1);
        logger.info("Recherche des produits proches de l'expiration");
        logger.info("Période : {} - {}", now, oneWeekLater);
        List<Produit> produitsProcheExpiration = produitRepository.findProduitsProcheExpiration(now, oneWeekLater);
        logger.info("Nombre de produits proches de l'expiration : {}", produitsProcheExpiration.size());

        return produitsProcheExpiration.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public ProduitDTO createProduit(ProduitDTO produitDTO) {
        Produit produit = convertToEntity(produitDTO);
        Produit savedProduit = produitRepository.save(produit);

        if (savedProduit.getIdStock() != null && savedProduit.getQuantite() > 0) {
            Map<String, Object> transaction = new HashMap<>();
            transaction.put("productId", savedProduit.getId());
            transaction.put("stockId", savedProduit.getIdStock());
            transaction.put("type", "ADD");
            transaction.put("quantity", savedProduit.getQuantite());
            transaction.put("notes", "Ajout initial du produit");

            try {
                stockClient.createTransaction(transaction);
            } catch (Exception e) {
                System.err.println("Erreur lors de la création de la transaction de stock: " + e.getMessage());
            }
        }

        return convertToDTO(savedProduit);
    }

    @Transactional
    public Optional<ProduitDTO> updateProduit(Long id, ProduitDTO produitDTO) {
        return produitRepository.findById(id)
                .map(existingProduit -> {
                    Integer ancienneQuantite = existingProduit.getQuantite();
                    Produit produit = convertToEntity(produitDTO);
                    produit.setId(id);
                    Produit updatedProduit = produitRepository.save(produit);

                    if (updatedProduit.getIdStock() != null && !updatedProduit.getQuantite().equals(ancienneQuantite)) {
                        int difference = updatedProduit.getQuantite() - ancienneQuantite;
                        if (difference != 0) {
                            Map<String, Object> transaction = new HashMap<>();
                            transaction.put("productId", updatedProduit.getId());
                            transaction.put("stockId", updatedProduit.getIdStock());
                            transaction.put("type", difference > 0 ? "ADD" : "REMOVE");
                            transaction.put("quantity", Math.abs(difference));
                            transaction.put("notes", "Ajustement lors de la modification du produit");

                            try {
                                stockClient.createTransaction(transaction);
                            } catch (Exception e) {
                                System.err.println("Erreur lors de la création de la transaction de stock: " + e.getMessage());
                            }
                        }
                    }

                    return convertToDTO(updatedProduit);
                });
    }

    @Transactional
    public Optional<ProduitDTO> ajusterStock(Long id, Integer quantiteAjustement) {
        return produitRepository.findById(id)
                .map(produit -> {
                    int nouvelleQuantite = produit.getQuantite() + quantiteAjustement;
                    if (nouvelleQuantite < 0) {
                        throw new IllegalArgumentException("La quantité ne peut pas devenir négative");
                    }
                    produit.setQuantite(nouvelleQuantite);
                    Produit updatedProduit = produitRepository.save(produit);

                    if (updatedProduit.getIdStock() != null) {
                        Map<String, Object> transaction = new HashMap<>();
                        transaction.put("productId", updatedProduit.getId());
                        transaction.put("stockId", updatedProduit.getIdStock());
                        transaction.put("type", quantiteAjustement > 0 ? "ADD" : "REMOVE");
                        transaction.put("quantity", Math.abs(quantiteAjustement));
                        transaction.put("notes", "Ajustement de stock: " + (quantiteAjustement > 0 ? "+" : "") + quantiteAjustement);

                        try {
                            stockClient.createTransaction(transaction);

                            if (nouvelleQuantite <= produit.getSeuilCritique()) {
                                logger.info("Stock critique détecté pour le produit {} : {} (seuil : {})",
                                        updatedProduit.getId(), nouvelleQuantite, produit.getSeuilCritique());
                            }
                        } catch (Exception e) {
                            logger.error("Erreur lors de la création de la transaction de stock: {}", e.getMessage(), e);
                        }
                    }

                    return convertToDTO(updatedProduit);
                });
    }

    @Transactional
    public boolean deleteProduit(Long id) {
        if (produitRepository.existsById(id)) {
            produitRepository.deleteById(id);
            return true;
        }
        return false;
    }

    // ============= RECHERCHE INTELLIGENTE - LA PARTIE MAGIQUE =============

    @Transactional(readOnly = true)
    public List<ProduitDTO> searchProduits(String searchTerm) {
        List<Produit> produits = produitRepository.searchByNom(searchTerm);
        return produits.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 🧠 RECHERCHE SUPER INTELLIGENTE - VERSION FINALE QUI FONCTIONNE
     */
    @Transactional(readOnly = true)
    public List<ProduitDTO> searchProduitsIntelligent(String userQuery) {
        logger.info("🧠 RECHERCHE SUPER INTELLIGENTE pour: '{}'", userQuery);

        String queryNormalisee = userQuery.toLowerCase().trim();

        // 1. RECHERCHE EXACTE d'abord
        List<ProduitDTO> results = searchProduits(userQuery);
        if (!results.isEmpty()) {
            logger.info("✅ Recherche exacte réussie: {} produits", results.size());
            return results;
        }

        // 2. CORRECTION INTELLIGENTE basée sur vos vrais produits
        String queryCorrigee = corrigerAvecVraisProduits(queryNormalisee);
        if (!queryCorrigee.equals(queryNormalisee)) {
            logger.info("🔧 Correction intelligente: '{}' → '{}'", userQuery, queryCorrigee);
            results = searchProduits(queryCorrigee);
            if (!results.isEmpty()) {
                logger.info("✅ Recherche avec correction réussie: {} produits", results.size());
                return results;
            }
        }

        // 3. RECHERCHE PAR SIMILARITÉ AVANCÉE
        results = rechercheParSimilarite(queryNormalisee);
        if (!results.isEmpty()) {
            logger.info("✅ Recherche par similarité réussie: {} produits", results.size());
            return results;
        }

        // 4. RECHERCHE PAR FRAGMENTS DE MOTS
        results = rechercheParFragments(queryNormalisee);
        logger.info("✅ Recherche finale: {} produits trouvés", results.size());

        return results;
    }

    /**
     * 🔧 Correction intelligente basée sur vos vrais produits
     */
    private String corrigerAvecVraisProduits(String query) {
        try {
            List<ProduitDTO> tousProduits = getAllProduits();
            Set<String> motsProduits = new HashSet<>();

            // Extraire tous les mots significatifs de vos produits
            for (ProduitDTO produit : tousProduits) {
                String[] mots = produit.getNomProduit().toLowerCase().split("\\s+");
                for (String mot : mots) {
                    if (mot.length() >= 3) { // Mots de 3+ caractères
                        motsProduits.add(mot.replaceAll("[^a-zA-ZÀ-ÿ]", ""));
                    }
                }

                if (produit.getDescription() != null) {
                    String[] motsDesc = produit.getDescription().toLowerCase().split("\\s+");
                    for (String mot : motsDesc) {
                        if (mot.length() >= 4) { // Mots de 4+ caractères pour description
                            motsProduits.add(mot.replaceAll("[^a-zA-ZÀ-ÿ]", ""));
                        }
                    }
                }
            }

            // Chercher les meilleures corrections
            String[] motsQuery = query.split("\\s+");
            boolean correctionTrouvee = false;

            for (int i = 0; i < motsQuery.length; i++) {
                String motQuery = motsQuery[i].replaceAll("[^a-zA-ZÀ-ÿ]", "");
                if (motQuery.length() >= 3) {

                    String meilleurCorrection = motQuery;
                    double meilleureSimilarite = 0.0;

                    for (String motProduit : motsProduits) {
                        double similarite = calculerSimilariteSimple(motQuery, motProduit);
                        if (similarite > meilleureSimilarite && similarite > 0.75) {
                            meilleureSimilarite = similarite;
                            meilleurCorrection = motProduit;
                        }
                    }

                    if (!meilleurCorrection.equals(motQuery)) {
                        logger.info("🎯 Correction: '{}' → '{}' ({}%)",
                                motQuery, meilleurCorrection, Math.round(meilleureSimilarite * 100));
                        motsQuery[i] = meilleurCorrection;
                        correctionTrouvee = true;
                    }
                }
            }

            return correctionTrouvee ? String.join(" ", motsQuery) : query;

        } catch (Exception e) {
            logger.error("Erreur correction intelligente: {}", e.getMessage());
            return query;
        }
    }

    /**
     * 🎯 Recherche par similarité avancée
     */
    private List<ProduitDTO> rechercheParSimilarite(String query) {
        try {
            List<ProduitDTO> tousProduits = getAllProduits();
            List<ProductMatch> matches = new ArrayList<>();

            for (ProduitDTO produit : tousProduits) {
                // Similarité avec le nom
                double similariteNom = calculerSimilariteAvancee(query, produit.getNomProduit().toLowerCase());

                // Similarité avec la description
                double similariteDesc = 0.0;
                if (produit.getDescription() != null) {
                    similariteDesc = calculerSimilariteAvancee(query, produit.getDescription().toLowerCase());
                }

                // Prendre la meilleure similarité
                double meilleureSimilarite = Math.max(similariteNom, similariteDesc);

                // Seuil à 60% pour être inclusif
                if (meilleureSimilarite > 0.6) {
                    matches.add(new ProductMatch(produit, meilleureSimilarite));
                    logger.info("🎯 Match trouvé: '{}' → '{}' ({}%)",
                            query, produit.getNomProduit(), Math.round(meilleureSimilarite * 100));
                }
            }

            // Trier par similarité décroissante
            matches.sort((a, b) -> Double.compare(b.similarite, a.similarite));

            return matches.stream()
                    .limit(10)
                    .map(match -> match.produit)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            logger.error("Erreur recherche similarité: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 🔍 Recherche par fragments de mots
     */
    private List<ProduitDTO> rechercheParFragments(String query) {
        try {
            String[] mots = query.split("\\s+");
            Set<ProduitDTO> resultatsUniques = new HashSet<>();

            for (String mot : mots) {
                if (mot.length() >= 3) {
                    List<ProduitDTO> resultatsFragment = searchProduits(mot);
                    resultatsUniques.addAll(resultatsFragment);
                }
            }

            logger.info("🔍 Recherche par fragments: {} mots → {} produits", mots.length, resultatsUniques.size());
            return new ArrayList<>(resultatsUniques);

        } catch (Exception e) {
            logger.error("Erreur recherche fragments: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    // ============= ALGORITHMES DE SIMILARITÉ =============

    private double calculerSimilariteSimple(String s1, String s2) {
        if (s1.equals(s2)) return 1.0;

        int distance = 0;
        int minLen = Math.min(s1.length(), s2.length());
        int maxLen = Math.max(s1.length(), s2.length());

        for (int i = 0; i < minLen; i++) {
            if (s1.charAt(i) != s2.charAt(i)) {
                distance++;
            }
        }
        distance += Math.abs(s1.length() - s2.length());

        double similarite = 1.0 - ((double) distance / maxLen);

        // Bonus si ça commence pareil
        if (s1.length() > 2 && s2.length() > 2 &&
                s1.substring(0, 2).equals(s2.substring(0, 2))) {
            similarite += 0.1;
        }

        return Math.max(0, Math.min(1.0, similarite));
    }

    private double calculerSimilariteAvancee(String s1, String s2) {
        if (s1 == null || s2 == null) return 0.0;
        if (s1.equals(s2)) return 1.0;

        // Correspondance partielle forte
        if (s1.contains(s2) || s2.contains(s1)) {
            return 0.85;
        }

        // Distance de Levenshtein normalisée
        int distance = calculerDistanceLevenshtein(s1, s2);
        int maxLength = Math.max(s1.length(), s2.length());

        if (maxLength == 0) return 1.0;

        double similarite = 1.0 - ((double) distance / maxLength);

        // Bonus pour début similaire
        if (s1.length() > 2 && s2.length() > 2 &&
                s1.substring(0, 3).equals(s2.substring(0, 3))) {
            similarite += 0.15;
        }

        return Math.min(1.0, similarite);
    }

    private int calculerDistanceLevenshtein(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];

        for (int i = 0; i <= a.length(); i++) {
            for (int j = 0; j <= b.length(); j++) {
                if (i == 0) {
                    dp[i][j] = j;
                } else if (j == 0) {
                    dp[i][j] = i;
                } else {
                    dp[i][j] = Math.min(
                            Math.min(
                                    dp[i - 1][j] + 1,
                                    dp[i][j - 1] + 1
                            ),
                            dp[i - 1][j - 1] + (a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1)
                    );
                }
            }
        }

        return dp[a.length()][b.length()];
    }

    // ============= CLASSES HELPER =============

    private static class ProductMatch {
        ProduitDTO produit;
        double similarite;

        ProductMatch(ProduitDTO produit, double similarite) {
            this.produit = produit;
            this.similarite = similarite;
        }
    }

    // ============= MÉTHODES EXISTANTES =============

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAllProduitsAvecStock() {
        List<Produit> produits = produitRepository.findAll();
        return produits.stream()
                .map(produit -> {
                    Map<String, Object> produitMap = new HashMap<>();
                    produitMap.put("id", produit.getId());
                    produitMap.put("nom", produit.getNomProduit());
                    produitMap.put("quantite", produit.getQuantite());
                    produitMap.put("seuilCritique", produit.getSeuilCritique());
                    return produitMap;
                })
                .collect(Collectors.toList());
    }

    private ProduitDTO convertToDTO(Produit produit) {
        ProduitDTO dto = new ProduitDTO();

        dto.setId(produit.getId());
        dto.setNomProduit(produit.getNomProduit());
        dto.setQuantite(produit.getQuantite());
        dto.setDescription(produit.getDescription());
        dto.setDetail(produit.getDetail());
        dto.setSeuilCritique(produit.getSeuilCritique());
        dto.setPrix(produit.getPrix());
        dto.setDateExpiration(produit.getDateExpiration());
        dto.setIdStock(produit.getIdStock());
        dto.setIdCategorie(produit.getIdCategorie());
        dto.setIdReduction(produit.getIdReduction());

        try {
            List<ProduitImage> images = produitImageRepository.findByProduitId(produit.getId());
            List<ProduitImageDTO> imageDTOs = images.stream()
                    .map(image -> {
                        ProduitImageDTO imageDTO = new ProduitImageDTO();
                        imageDTO.setId(image.getId());
                        imageDTO.setCheminFichier(image.getCheminFichier());
                        imageDTO.setUrl(image.getUrl());
                        imageDTO.setImagePrincipale(image.getImagePrincipale());
                        return imageDTO;
                    })
                    .collect(Collectors.toList());

            dto.setImages(imageDTOs);
        } catch (Exception e) {
            logger.error("Erreur lors de l'accès aux images du produit: " + e.getMessage(), e);
            dto.setImages(new ArrayList<>());
        }

        return dto;
    }

    private Produit convertToEntity(ProduitDTO dto) {
        Produit produit = new Produit();
        produit.setId(dto.getId());
        produit.setNomProduit(dto.getNomProduit());
        produit.setQuantite(dto.getQuantite());
        produit.setDescription(dto.getDescription());
        produit.setDetail(dto.getDetail());
        produit.setSeuilCritique(dto.getSeuilCritique());
        produit.setPrix(dto.getPrix());
        produit.setDateExpiration(dto.getDateExpiration());
        produit.setIdStock(dto.getIdStock());
        produit.setIdCategorie(dto.getIdCategorie());
        produit.setIdReduction(dto.getIdReduction());
        return produit;
    }

    @Transactional(readOnly = true)
    public List<ProduitDTO> getProduitsByBoutique(Integer idBoutique) {
        List<String> idCategories = categorieRepository.findByBoutiqueId(idBoutique)
                .stream()
                .map(Categorie::getIdCategorie)
                .collect(Collectors.toList());

        List<Produit> produits = produitRepository.findByIdCategorieIn(idCategories);

        return produits.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }



}