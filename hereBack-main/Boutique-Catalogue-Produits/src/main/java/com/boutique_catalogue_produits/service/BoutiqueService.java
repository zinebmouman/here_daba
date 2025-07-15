package com.boutique_catalogue_produits.service;

import com.boutique_catalogue_produits.dto.BoutiqueDTO;
import com.boutique_catalogue_produits.dto.VendeurDTO;
import com.boutique_catalogue_produits.model.Boutique;
import com.boutique_catalogue_produits.model.Categorie;

import com.boutique_catalogue_produits.repository.BoutiqueRepository;
import com.boutique_catalogue_produits.repository.CategorieRepository;
import org.hibernate.Hibernate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class BoutiqueService {
    private static final Logger logger = LoggerFactory.getLogger(BoutiqueService.class);

    @Value("${app.development-mode:false}")
    private boolean developmentMode;
    @Autowired
    private BoutiqueRepository boutiqueRepository;
    @Autowired
    private FileStorageService fileStorageService;
    @Autowired
    private CategorieRepository categorieRepository;

    @Autowired
    private AuthServiceClient authServiceClient;


    public List<Boutique> getAllBoutiques() {
        return boutiqueRepository.findAll();
    }
    // Cache des informations du vendeur
    //@Cacheable(value = "vendeurs", key = "#idVendeur")
    public VendeurDTO getVendeurFromCache(String vendeurId) {
        try {
            logger.info("Tentative de récupération du vendeur avec l'ID: {}", vendeurId);

            VendeurDTO vendeur = authServiceClient.getVendeurById(vendeurId);

            // Log détaillé de la réponse
            logger.info("Vendeur récupéré: {}", vendeur);

            if (vendeur == null) {
                logger.warn("Aucun vendeur trouvé pour l'ID: {}", vendeurId);
                return createDefaultVendeur(vendeurId);
            }

            return vendeur;
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération du vendeur", e);
            return createDefaultVendeur(vendeurId);
        }
    }
    private VendeurDTO createDefaultVendeur(String vendeurId) {
        return new VendeurDTO(
                vendeurId,
                "vendeur",
                "Vendeur Défaut",
                "default-vendeur@exemple.com"
        );
    }


    /**
     * Ajouter une nouvelle boutique pour un vendeur
     */
    @Transactional
    public Boutique ajouterBoutique(Boutique boutique, String vendeurId) {
        // Log détaillé
        System.out.println("Début de création de boutique");
        System.out.println("ID Vendeur: " + vendeurId);
        System.out.println("Nom de la boutique: " + boutique.getNom());

        // Vérifier que l'utilisateur est un vendeur
        VendeurDTO vendeur = getVendeurFromCache(vendeurId);
        if (vendeur == null || !vendeur.getRole().equals("vendeur")) {
            throw new RuntimeException("L'utilisateur n'est pas un vendeur !");
        }

        // Assigner le vendeur à la boutique
        boutique.setVendeurId(vendeurId);

        // Gestion robuste du contact
        if (boutique.getContact() == null) {
            System.out.println("Contact null, initialisation à 0");
            boutique.setContact(0L);
        }

        // Validation supplémentaire
        if (boutique.getNom() == null || boutique.getNom().trim().isEmpty()) {
            throw new IllegalArgumentException("Le nom de la boutique est obligatoire");
        }
        if (boutique.getBoutique_img() != null && !boutique.getBoutique_img().isEmpty()) {
            // Utiliser le FileStorageService pour générer l'URL de l'image
            String imageUrl = fileStorageService.genererUrl(boutique.getBoutique_img());
            boutique.setBoutiqueImgUrl(imageUrl);
            System.out.println("URL d'image générée: " + imageUrl);
        }
        // Log avant sauvegarde
        System.out.println("Contact avant sauvegarde: " + boutique.getContact());
        System.out.println("Contact avant sauvegarde: " + boutique.getContact());
        System.out.println("Image avant sauvegarde: " + boutique.getBoutique_img());
        System.out.println("URL d'image avant sauvegarde: " + boutique.getBoutiqueImgUrl());
        try {
            // Enregistrer la boutique
            Boutique nouvelleBoutique = boutiqueRepository.save(boutique);

            System.out.println("Boutique créée avec succès. ID: " + nouvelleBoutique.getId_boutique());

            return nouvelleBoutique;
        } catch (Exception e) {
            // Log de l'erreur détaillée
            System.err.println("Erreur lors de la sauvegarde de la boutique:");
            e.printStackTrace();

            // Relancer l'exception avec un message plus informatif
            throw new RuntimeException("Impossible de sauvegarder la boutique : " + e.getMessage(), e);
        }
    }
    public Boutique getBoutiqueById(Integer boutiqueId) {
        // Utilisez votre repository pour trouver la boutique
        Optional<Boutique> optionalBoutique = boutiqueRepository.findById(boutiqueId);
        return optionalBoutique.orElse(null);
    }
    // Modifier BoutiqueService.getVendeurIdForBoutique
    public String getVendeurIdForBoutique(Long idBoutique) {
        try {
            // Convertir Long en Integer si nécessaire
            Integer boutiqueId = idBoutique.intValue();

            Boutique boutique = boutiqueRepository.findById(boutiqueId).orElse(null);
            if (boutique == null) {
                logger.warn("Boutique non trouvée pour l'ID: {}", idBoutique);
                return null;
            }
            return boutique.getVendeurId();
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération du vendeur pour la boutique {}: {}",
                    idBoutique, e.getMessage(), e);
            return null;
        }
    }
    /**
     * Récupérer toutes les boutiques d'un vendeur
     */
    // Modifier BoutiqueService.java - Méthode getBoutiquesByVendeur
    @Transactional(readOnly = true)
    public List<Boutique> getBoutiquesByVendeur(String vendeurId) {
        try {
            // Vérification robuste
            if (vendeurId == null || vendeurId.trim().isEmpty()) {
                logger.warn("ID vendeur null ou vide fourni à getBoutiquesByVendeur");
                return Collections.emptyList();
            }

            // Log détaillé pour traçage
            logger.info("Début getBoutiquesByVendeur pour vendeur: {}", vendeurId);

            // Essayer de récupérer le vendeur
            VendeurDTO vendeur = null;
            try {
                vendeur = getVendeurFromCache(vendeurId);
                logger.info("Vendeur récupéré: {}", vendeur);
            } catch (Exception e) {
                logger.error("Erreur lors de la récupération du vendeur, utilisation d'un fallback", e);
                vendeur = createDefaultVendeur(vendeurId);
            }

            // Récupérer les boutiques, même sans vendeur
            List<Boutique> boutiques = boutiqueRepository.findByVendeurId(vendeurId);
            logger.info("Boutiques trouvées: {} pour vendeur: {}", boutiques.size(), vendeurId);

            return boutiques;
        } catch (Exception e) {
            logger.error("Exception non gérée dans getBoutiquesByVendeur pour vendeur: {}", vendeurId, e);
            return Collections.emptyList();
        }
    }


    /**
     * Supprimer une boutique
     */
    @Transactional
    public void supprimerBoutique(Integer boutiqueId, String idVendeur) {
        Boutique boutique = boutiqueRepository.findById(boutiqueId)
                .orElseThrow(() -> new RuntimeException("Boutique non trouvée: " + boutiqueId));
        // Vérifier que la boutique appartient bien au vendeur
        if (!boutique.getVendeurId().equals(idVendeur)) {
            throw new RuntimeException("Cette boutique n'appartient pas à ce vendeur");
        }
        boutiqueRepository.deleteById(boutiqueId);
    }
    /**
     * Mettre à jour une boutique
     */
    @Transactional
    public Boutique mettreAJourBoutique(Integer boutiqueId, Boutique boutique, String idVendeur) {
        logger.info("URL d'image reçue pour mise à jour: {}", boutique.getBoutiqueImgUrl());
        // Débogage détaillé
        System.out.println("=== DÉBUT MISE À JOUR BOUTIQUE ===");
        System.out.println("ID Boutique: " + boutiqueId);
        System.out.println("ID Vendeur: " + idVendeur);
        System.out.println("Nom reçu: " + boutique.getNom());
        System.out.println("Adresse reçue: " + boutique.getAdress());

        // Vérifier l'existence de la boutique dans le service
        Optional<Boutique> existingOptional = boutiqueRepository.findById(boutiqueId);

        if (!existingOptional.isPresent()) {
            System.out.println("ERREUR: Boutique non trouvée avec ID " + boutiqueId);
            throw new RuntimeException("Boutique non trouvée");
        }

        Boutique existing = existingOptional.get();
        System.out.println("Boutique existante trouvée: " + existing.getNom());

        // Vérifier que l'utilisateur est autorisé
        if (!existing.getVendeurId().equals(idVendeur)) {
            System.out.println("ERREUR: Vendeur " + idVendeur + " non autorisé à modifier cette boutique");
            throw new RuntimeException("Non autorisé à modifier cette boutique");
        }

        // IMPORTANT: Conserver les attributs existants si les nouveaux sont null
        // Sauvegarde de tous les attributs existants d'abord
        if (boutique.getAdress() == null) {
            boutique.setAdress(existing.getAdress());
            System.out.println("Préservation de l'adresse existante: " + existing.getAdress());
        }

        if (boutique.getVille() == null) {
            boutique.setVille(existing.getVille());
            System.out.println("Préservation de la ville existante: " + existing.getVille());
        }
        if (boutique.getBoutiqueImgUrl() == null) {
            boutique.setBoutiqueImgUrl(existing.getBoutiqueImgUrl());
            System.out.println("Préservation de l'URL d'image: " + existing.getBoutiqueImgUrl());
        }
        if (boutique.getCodePostal() == null) {
            boutique.setCodePostal(existing.getCodePostal());
            System.out.println("Préservation du code postal existant: " + existing.getCodePostal());
        }
        if (boutique.getPays() == null) {
            boutique.setPays(existing.getPays());
            System.out.println("Préservation du pays existant: " + existing.getPays());
        }
        if (boutique.getContact() == null) {
            boutique.setContact(existing.getContact());
            System.out.println("Préservation du contact existant: " + existing.getContact());
        }
        if (boutique.getHoraire() == null) {
            boutique.setHoraire(existing.getHoraire());
            System.out.println("Préservation de l'horaire existant: " + existing.getHoraire());
        }
        // TRÈS IMPORTANT: Vérifier explicitement le nom
        if (boutique.getNom() == null || boutique.getNom().trim().isEmpty()) {
            System.out.println("ATTENTION: Nom null ou vide, utilisation du nom existant: " + existing.getNom());
            boutique.setNom(existing.getNom());
        } else {
            System.out.println("Mise à jour du nom: " + boutique.getNom());
        }
        // Conserver l'ID de la boutique
        boutique.setId_boutique(boutiqueId);
        // Conserver les valeurs qui ne doivent pas être modifiées
        boutique.setVendeurId(idVendeur); // Toujours maintenir l'ID du vendeur d'origine
        // Conserver les catégories
        boutique.setCategories(existing.getCategories());
        // Valider les champs obligatoires une dernière fois
        if (boutique.getNom() == null || boutique.getNom().trim().isEmpty()) {
            throw new IllegalArgumentException("Le nom de la boutique est obligatoire");
        }
        // Sauvegarder et retourner la boutique mise à jour
        Boutique updated = boutiqueRepository.save(boutique);
        System.out.println("=== FIN MISE À JOUR BOUTIQUE ===");
        System.out.println("Nom final: " + updated.getNom());
        System.out.println("Adresse finale: " + updated.getAdress());
        logger.info("URL d'image après sauvegarde: {}", updated.getBoutiqueImgUrl());
        return updated;
    }

    @Transactional
    public Boutique addCategorieToBoutique(Integer idBoutique, String idCategorie) {
        Boutique boutique = boutiqueRepository.findById(idBoutique)
                .orElseThrow(() -> new RuntimeException("Boutique non trouvée avec l'ID: " + idBoutique));

        Categorie categorie = categorieRepository.findById(idCategorie)
                .orElseThrow(() -> new RuntimeException("Catégorie non trouvée avec l'ID: " + idCategorie));

        return boutiqueRepository.save(boutique);
    }

    @Transactional
    public Boutique removeCategorieFromBoutique(Integer idBoutique, String idCategorie) {
        Boutique boutique = boutiqueRepository.findById(idBoutique)
                .orElseThrow(() -> new RuntimeException("Boutique non trouvée avec l'ID: " + idBoutique));

        Categorie categorie = categorieRepository.findById(idCategorie)
                .orElseThrow(() -> new RuntimeException("Catégorie non trouvée avec l'ID: " + idCategorie));


        return boutiqueRepository.save(boutique);
    }

    public Set<Categorie> getBoutiqueCategories(Integer idBoutique) {
        Boutique boutique = boutiqueRepository.findById(idBoutique)
                .orElseThrow(() -> new RuntimeException("Boutique non trouvée avec l'ID: " + idBoutique));

        return boutique.getCategories();
    }

    public List<String> getImagesByBoutiqueId(Integer boutiqueId) {
        Boutique boutique = boutiqueRepository.findById(boutiqueId)
                .orElseThrow(() -> new RuntimeException("Boutique non trouvée"));

        // Exemple : si boutique a un champ boutique_img qui est une URL
        return boutique.getBoutique_img() == null ? Collections.emptyList()
                : List.of(boutique.getBoutique_img());
    }

    public BoutiqueDTO convertToDTO(Boutique boutique) {
        BoutiqueDTO dto = new BoutiqueDTO();
        dto.setId_boutique(boutique.getId_boutique().toString()); // Convertir Integer en String
        dto.setNom(boutique.getNom());
        dto.setAdress(boutique.getAdress());
        dto.setVille(boutique.getVille());
        dto.setPays(boutique.getPays());
        dto.setBoutique_img(boutique.getBoutique_img());

        // Si contact est un Long ou Integer, convertis aussi
        if (boutique.getContact() != null) {
            dto.setContact(boutique.getContact().toString());
        }

        return dto;
    }

    // AJOUTER CES MÉTHODES DANS VOTRE BoutiqueService.java EXISTANT

// ============= MÉTHODES DE RECHERCHE INTELLIGENTE MANQUANTES =============

    /**
     * 🔍 RECHERCHE CLASSIQUE DE BOUTIQUES
     */
    @Transactional(readOnly = true)
    public List<Boutique> searchBoutiquesClassique(String searchTerm) {
        logger.info("🔍 [BOUTIQUE] Recherche classique: '{}'", searchTerm);

        try {
            if (searchTerm == null || searchTerm.trim().isEmpty()) {
                return Collections.emptyList();
            }

            String searchPattern = "%" + searchTerm.toLowerCase() + "%";

            // Recherche dans le repository par nom, ville, adresse
            List<Boutique> results = boutiqueRepository.findByNomContainingIgnoreCaseOrVilleContainingIgnoreCaseOrAdressContainingIgnoreCase(
                    searchTerm, searchTerm, searchTerm);

            logger.info("✅ [BOUTIQUE] Recherche classique: {} boutiques trouvées", results.size());
            return results;

        } catch (Exception e) {
            logger.error("❌ [BOUTIQUE] Erreur recherche classique: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 🧠 RECHERCHE INTELLIGENTE DE BOUTIQUES
     */
    @Transactional(readOnly = true)
    public List<Boutique> searchBoutiquesIntelligent(String userQuery) {
        logger.info("🧠 [BOUTIQUE] Recherche intelligente: '{}'", userQuery);

        try {
            if (userQuery == null || userQuery.trim().isEmpty()) {
                return Collections.emptyList();
            }

            String queryNormalisee = userQuery.toLowerCase().trim();

            // 1. RECHERCHE EXACTE d'abord
            List<Boutique> results = searchBoutiquesClassique(userQuery);
            if (!results.isEmpty()) {
                logger.info("✅ [BOUTIQUE] Recherche exacte réussie: {} boutiques", results.size());
                return results;
            }

            // 2. CORRECTION INTELLIGENTE
            String queryCorrigee = corrigerRequeteBoutique(queryNormalisee);
            if (!queryCorrigee.equals(queryNormalisee)) {
                logger.info("🔧 [BOUTIQUE] Correction: '{}' → '{}'", userQuery, queryCorrigee);
                results = searchBoutiquesClassique(queryCorrigee);
                if (!results.isEmpty()) {
                    logger.info("✅ [BOUTIQUE] Recherche avec correction réussie: {} boutiques", results.size());
                    return results;
                }
            }

            // 3. RECHERCHE PAR SIMILARITÉ
            results = rechercheParSimilariteBoutique(queryNormalisee);
            if (!results.isEmpty()) {
                logger.info("✅ [BOUTIQUE] Recherche par similarité réussie: {} boutiques", results.size());
                return results;
            }

            // 4. RECHERCHE PAR FRAGMENTS
            results = rechercheParFragmentsBoutique(queryNormalisee);
            logger.info("✅ [BOUTIQUE] Recherche finale: {} boutiques trouvées", results.size());

            return results;

        } catch (Exception e) {
            logger.error("❌ [BOUTIQUE] Erreur recherche intelligente: {}", e.getMessage());
            return searchBoutiquesClassique(userQuery); // Fallback vers recherche classique
        }
    }

    /**
     * 🔧 Correction intelligente des requêtes boutique
     */
    private String corrigerRequeteBoutique(String query) {
        try {
            // Corrections spécifiques pour les boutiques
            Map<String, String> corrections = Map.of(
                    "casa", "casablanca",
                    "rabat", "rabat",
                    "marrakesh", "marrakech",
                    "fès", "fes",
                    "tanger", "tanger",
                    "agadir", "agadir",
                    "shop", "boutique",
                    "store", "magasin",
                    "center", "centre",
                    "mall", "centre commercial"
            );

            String corrected = query;
            for (Map.Entry<String, String> correction : corrections.entrySet()) {
                if (corrected.contains(correction.getKey())) {
                    corrected = corrected.replace(correction.getKey(), correction.getValue());
                    logger.info("🎯 [BOUTIQUE] Correction appliquée: '{}' → '{}'",
                            correction.getKey(), correction.getValue());
                }
            }

            return corrected;

        } catch (Exception e) {
            logger.error("❌ [BOUTIQUE] Erreur correction: {}", e.getMessage());
            return query;
        }
    }

    /**
     * 🎯 Recherche par similarité pour boutiques
     */
    private List<Boutique> rechercheParSimilariteBoutique(String query) {
        try {
            List<Boutique> toutesBoutiques = getAllBoutiques();
            List<BoutiqueMatch> matches = new ArrayList<>();

            for (Boutique boutique : toutesBoutiques) {
                double score = 0.0;

                // Similarité avec le nom
                if (boutique.getNom() != null) {
                    score = Math.max(score, calculerSimilariteBoutique(query, boutique.getNom().toLowerCase()));
                }

                // Similarité avec la ville
                if (boutique.getVille() != null) {
                    score = Math.max(score, calculerSimilariteBoutique(query, boutique.getVille().toLowerCase()) * 0.8);
                }

                // Similarité avec l'adresse
                if (boutique.getAdress() != null) {
                    score = Math.max(score, calculerSimilariteBoutique(query, boutique.getAdress().toLowerCase()) * 0.6);
                }



                // Seuil de similarité pour boutiques (plus permissif)
                if (score > 0.5) {
                    matches.add(new BoutiqueMatch(boutique, score));
                    logger.debug("🎯 [BOUTIQUE] Match: '{}' → '{}' ({}%)",
                            query, boutique.getNom(), Math.round(score * 100));
                }
            }

            // Trier par score décroissant
            matches.sort((a, b) -> Double.compare(b.score, a.score));

            return matches.stream()
                    .limit(15) // Plus de résultats pour les boutiques
                    .map(match -> match.boutique)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            logger.error("❌ [BOUTIQUE] Erreur recherche similarité: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 🔍 Recherche par fragments pour boutiques
     */
    private List<Boutique> rechercheParFragmentsBoutique(String query) {
        try {
            String[] mots = query.split("\\s+");
            Set<Boutique> resultatsUniques = new HashSet<>();

            for (String mot : mots) {
                if (mot.length() >= 2) { // Seuil plus bas pour les boutiques (villes, etc.)
                    List<Boutique> resultatsFragment = searchBoutiquesClassique(mot);
                    resultatsUniques.addAll(resultatsFragment);
                }
            }

            logger.info("🔍 [BOUTIQUE] Recherche par fragments: {} mots → {} boutiques",
                    mots.length, resultatsUniques.size());

            return new ArrayList<>(resultatsUniques);

        } catch (Exception e) {
            logger.error("❌ [BOUTIQUE] Erreur recherche fragments: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Calculer la similarité entre deux chaînes pour boutiques
     */
    private double calculerSimilariteBoutique(String s1, String s2) {
        if (s1 == null || s2 == null) return 0.0;
        if (s1.equals(s2)) return 1.0;

        // Correspondance exacte prioritaire
        if (s1.contains(s2) || s2.contains(s1)) {
            return 0.9;
        }

        // Distance de Levenshtein normalisée
        int distance = calculerDistanceLevenshteinBoutique(s1, s2);
        int maxLength = Math.max(s1.length(), s2.length());

        if (maxLength == 0) return 1.0;

        double similarite = 1.0 - ((double) distance / maxLength);

        // Bonus pour début similaire (important pour les noms de boutiques)
        if (s1.length() > 1 && s2.length() > 1 &&
                s1.substring(0, Math.min(2, Math.min(s1.length(), s2.length())))
                        .equals(s2.substring(0, Math.min(2, Math.min(s1.length(), s2.length()))))) {
            similarite += 0.2;
        }

        return Math.min(1.0, Math.max(0.0, similarite));
    }

    /**
     * Distance de Levenshtein pour boutiques
     */
    private int calculerDistanceLevenshteinBoutique(String a, String b) {
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

    /**
     * 🌍 RECHERCHE GÉOLOCALISÉE DE BOUTIQUES
     */
    @Transactional(readOnly = true)
    public List<Boutique> searchBoutiquesParVille(String ville) {
        logger.info("🌍 [BOUTIQUE] Recherche par ville: '{}'", ville);

        try {
            if (ville == null || ville.trim().isEmpty()) {
                return Collections.emptyList();
            }

            List<Boutique> results = boutiqueRepository.findByVilleContainingIgnoreCase(ville);

            logger.info("✅ [BOUTIQUE] Trouvé {} boutiques dans '{}'", results.size(), ville);
            return results;

        } catch (Exception e) {
            logger.error("❌ [BOUTIQUE] Erreur recherche par ville: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 📊 RECHERCHE AVEC FILTRES AVANCÉS
     */
    @Transactional(readOnly = true)
    public List<Boutique> searchBoutiquesAvecFiltres(String query, String ville, String vendeurId) {
        logger.info("📊 [BOUTIQUE] Recherche avec filtres: query='{}', ville='{}', vendeur='{}'",
                query, ville, vendeurId);

        try {
            List<Boutique> results = getAllBoutiques();

            // Filtrer par query si fournie
            if (query != null && !query.trim().isEmpty()) {
                results = results.stream()
                        .filter(b -> matchBoutiqueQuery(b, query))
                        .collect(Collectors.toList());
            }

            // Filtrer par ville si fournie
            if (ville != null && !ville.trim().isEmpty()) {
                results = results.stream()
                        .filter(b -> b.getVille() != null &&
                                b.getVille().toLowerCase().contains(ville.toLowerCase()))
                        .collect(Collectors.toList());
            }

            // Filtrer par vendeur si fourni
            if (vendeurId != null && !vendeurId.trim().isEmpty()) {
                results = results.stream()
                        .filter(b -> vendeurId.equals(b.getVendeurId()))
                        .collect(Collectors.toList());
            }

            logger.info("✅ [BOUTIQUE] Filtres appliqués: {} boutiques", results.size());
            return results;

        } catch (Exception e) {
            logger.error("❌ [BOUTIQUE] Erreur recherche avec filtres: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Vérifier si une boutique correspond à la requête
     */
    private boolean matchBoutiqueQuery(Boutique boutique, String query) {
        String queryLower = query.toLowerCase();

        return (boutique.getNom() != null && boutique.getNom().toLowerCase().contains(queryLower)) ||
                (boutique.getVille() != null && boutique.getVille().toLowerCase().contains(queryLower)) ||
                (boutique.getAdress() != null && boutique.getAdress().toLowerCase().contains(queryLower));
    }

// ============= CLASSE HELPER POUR BOUTIQUES =============

    /**
     * Classe helper pour les matches de boutiques
     */
    private static class BoutiqueMatch {
        Boutique boutique;
        double score;

        BoutiqueMatch(Boutique boutique, double score) {
            this.boutique = boutique;
            this.score = score;
        }
    }

// ============= MÉTHODES UTILITAIRES SUPPLÉMENTAIRES =============

    /**
     * 📈 OBTENIR LES BOUTIQUES POPULAIRES
     */
    @Transactional(readOnly = true)
    public List<Boutique> getBoutiquesPopulaires(int limite) {
        logger.info("📈 [BOUTIQUE] Récupération des {} boutiques populaires", limite);

        try {
            // Pour l'instant, retourne les boutiques par ordre d'ID (peut être amélioré avec un système de rating)
            List<Boutique> boutiques = getAllBoutiques();

            return boutiques.stream()
                    .filter(b -> b.isValid()) // Seulement les boutiques valides
                    .sorted((b1, b2) -> b2.getId_boutique().compareTo(b1.getId_boutique())) // Plus récentes en premier
                    .limit(limite)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            logger.error("❌ [BOUTIQUE] Erreur boutiques populaires: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 🎯 RECHERCHE PAR PROXIMITÉ (si coordonnées GPS disponibles)
     */
    @Transactional(readOnly = true)
    public List<Boutique> searchBoutiquesParProximite(double latitude, double longitude, int rayonKm) {
        logger.info("🎯 [BOUTIQUE] Recherche par proximité: ({}, {}) rayon {}km",
                latitude, longitude, rayonKm);

        try {
            // TODO: Implémenter quand les coordonnées GPS seront ajoutées au modèle Boutique
            // Pour l'instant, retourner toutes les boutiques
            logger.warn("⚠️ [BOUTIQUE] Recherche par proximité non implémentée - coordonnées GPS manquantes");
            return getAllBoutiques().stream().limit(10).collect(Collectors.toList());

        } catch (Exception e) {
            logger.error("❌ [BOUTIQUE] Erreur recherche proximité: {}", e.getMessage());
            return Collections.emptyList();
        }
    }


}