package com.boutique_catalogue_produits.controller;

import com.boutique_catalogue_produits.dto.ProduitDTO;
import com.boutique_catalogue_produits.model.Boutique;
import com.boutique_catalogue_produits.dto.BoutiqueDTO;
import com.boutique_catalogue_produits.model.Categorie;
import com.boutique_catalogue_produits.dto.VendeurDTO;
import com.boutique_catalogue_produits.service.BoutiqueService;
import com.boutique_catalogue_produits.service.ProduitService;
import io.micrometer.common.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/boutiques")
public class BoutiqueController {
    private static final Logger logger = LoggerFactory.getLogger(BoutiqueController.class);

    @Autowired
    private ProduitService produitService;
    @Autowired
    private BoutiqueService boutiqueService;

    @GetMapping
    public ResponseEntity<List<Boutique>> getAllBoutiques() {
        List<Boutique> boutiques = boutiqueService.getAllBoutiques();
        return ResponseEntity.ok(boutiques);
    }

    /**
     * Ajouter une nouvelle boutique pour un vendeur
     */
    @PostMapping
    public ResponseEntity<?> ajouterBoutique(
            @RequestBody BoutiqueDTO boutiqueDTO,
            @RequestHeader(value = "X-Vendeur-ID", required = false) String idVendeur) {

        // Logs détaillés
        logger.info("===============================================");
        logger.info("DÉBUT traitement requête POST /api/boutiques");
        logger.info("ID Vendeur: {}", idVendeur);
        logger.info("Données reçues: {}", boutiqueDTO);

        // Validation avec contournement pour debug
        if (idVendeur == null || idVendeur.trim().isEmpty()) {
            // Pour le débogage, attribuer un ID vendeur par défaut
            idVendeur = "debug-vendeur-id";
            logger.info("Aucun ID vendeur, utilisation de la valeur par défaut: {}", idVendeur);
        }

        try {
            // Conversion du DTO en entité
            Boutique boutique = Boutique.fromFrontendForm(boutiqueDTO);

            logger.info("Conversion DTO → entité réussie");
            logger.info("Image reçue dans DTO: {}", boutiqueDTO.getBoutique_img());
            logger.info("Image convertie dans l'entité: {}", boutique.getBoutique_img());
            // Définir l'ID du vendeur
            boutique.setVendeurId(idVendeur);

            // Sauvegarde de la boutique
            logger.info("Tentative d'ajout de la boutique...");
            Boutique nouvelleBoutique = boutiqueService.ajouterBoutique(boutique, idVendeur);

            logger.info("Boutique créée avec succès, ID: {}", nouvelleBoutique.getId_boutique());
            logger.info("FIN traitement requête POST /api/boutiques");
            logger.info("===============================================");

            // Réponse de succès
            return ResponseEntity.status(HttpStatus.CREATED).body(nouvelleBoutique);
        } catch (Exception e) {
            logger.error("ERREUR lors de la création de la boutique: {}", e.getMessage(), e);
            logger.info("===============================================");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de la création: " + e.getMessage());
        }
    }

    /**
     * Récupérer toutes les boutiques d'un vendeur
     */
    @GetMapping("/vendeur/{idVendeur}")
    public ResponseEntity<List<Boutique>> getBoutiquesByVendeur(@PathVariable String idVendeur) {
        try {
            logger.info("Récupération des boutiques pour le vendeur: {}", idVendeur);
            List<Boutique> boutiques = boutiqueService.getBoutiquesByVendeur(idVendeur);
            return ResponseEntity.ok(boutiques);
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération des boutiques pour le vendeur {}", idVendeur, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.emptyList());
        }
    }

    /**
     * Récupérer toutes les boutiques du vendeur connecté - version corrigée
     */
    @GetMapping("/mes-boutiques")
    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public ResponseEntity<?> getMesBoutiques(
            @RequestHeader(value = "X-Vendeur-ID", required = false) String idVendeurHeader,
            @RequestParam(value = "vendeurId", required = false) String idVendeurParam) {

        logger.info("===== DÉBUT TRAITEMENT getMesBoutiques =====");
        logger.info("Headers reçus - X-Vendeur-ID: {}", idVendeurHeader);
        logger.info("Paramètres reçus - vendeurId: {}", idVendeurParam);

        try {
            // Utiliser le header ou le paramètre, selon ce qui est disponible
            String idVendeur = idVendeurHeader;

            if (StringUtils.isBlank(idVendeur)) {
                idVendeur = idVendeurParam;
                logger.info("Utilisation du paramètre vendeurId: {}", idVendeur);
            }

            // Toujours utiliser une valeur par défaut pour le débogage
            if (StringUtils.isBlank(idVendeur)) {
                idVendeur = "debug-vendeur-id";
                logger.info("Aucun ID vendeur fourni, utilisation de la valeur par défaut: {}", idVendeur);
            }

            // Récupérer les boutiques
            List<Boutique> boutiques = boutiqueService.getBoutiquesByVendeur(idVendeur);

            if (boutiques.isEmpty()) {
                logger.info("Aucune boutique trouvée pour le vendeur {}", idVendeur);
                logger.info("===== FIN TRAITEMENT getMesBoutiques (vide) =====");
                return ResponseEntity.ok(Collections.emptyList());
            }

            logger.info("Nombre de boutiques trouvées: {}", boutiques.size());
            logger.info("===== FIN TRAITEMENT getMesBoutiques (succès) =====");
            return ResponseEntity.ok(boutiques);

        } catch (Exception e) {
            logger.error("Erreur lors de la récupération des boutiques", e);
            logger.error("===== FIN TRAITEMENT getMesBoutiques (erreur) =====");

            // Forcer la fin de la transaction
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de la récupération des boutiques : " + e.getMessage());
        }
    }
    /**
     * Endpoint de diagnostic pour tester la connectivité
     */
    @GetMapping("/diagnostic")
    public ResponseEntity<Map<String, Object>> diagnostic(
            @RequestHeader(value = "X-Vendeur-ID", required = false) String idVendeur) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "boutique-service");
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("X-Vendeur-ID reçu", idVendeur != null ? idVendeur : "non fourni");

        return ResponseEntity.ok(response);
    }
    /**
     * Endpoint simplifié pour le débogage
     */
    @GetMapping("/boutiques-vendeur/{idVendeur}")
    public ResponseEntity<?> getBoutiquesParVendeur(@PathVariable String idVendeur) {
        logger.info("Appel à getBoutiquesParVendeur avec idVendeur: {}", idVendeur);

        try {
            List<Boutique> boutiques = boutiqueService.getBoutiquesByVendeur(idVendeur);
            return ResponseEntity.ok(boutiques);
        } catch (Exception e) {
            logger.error("Erreur dans getBoutiquesParVendeur", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur: " + e.getMessage());
        }
    }

    /**
     * Supprimer une boutique
     */
    @DeleteMapping("/{boutiqueId}")
    public ResponseEntity<?> supprimerBoutique(
            @PathVariable Integer boutiqueId,
            @RequestHeader(value = "X-Vendeur-ID", required = false) String idVendeur) {
        try {
            // Log détaillé pour le débogage
            logger.info("Tentative de suppression de la boutique {} par le vendeur {}", boutiqueId, idVendeur);

            // Fallback pour le débogage
            if (StringUtils.isBlank(idVendeur)) {
                idVendeur = "debug-vendeur-id";
                logger.info("Aucun ID vendeur fourni, utilisation de la valeur par défaut: {}", idVendeur);
            }

            boutiqueService.supprimerBoutique(boutiqueId, idVendeur);
            return ResponseEntity.ok().body("Boutique supprimée avec succès");
        } catch (Exception e) {
            // Log détaillé de l'erreur
            logger.error("Erreur lors de la suppression de la boutique {}", boutiqueId, e);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de la suppression: " + e.getMessage());
        }
    }

    /**
     * Mettre à jour une boutique
     */
    @PutMapping("/{boutiqueId}")
    public ResponseEntity<?> mettreAJourBoutique(
            @PathVariable Integer boutiqueId,
            @RequestBody Boutique boutique,
            @RequestHeader(value = "X-Vendeur-ID", required = false) String idVendeur) {

        // Débogage détaillé
        logger.info("===============================================");
        logger.info("DÉBUT TRAITEMENT REQUÊTE PUT /api/boutiques/{}", boutiqueId);
        logger.info("ID Boutique: {}", boutiqueId);
        logger.info("ID Vendeur: {}", idVendeur);
        logger.info("Boutique reçue: {}", boutique);

        // Validation avec contournement pour debug
        if (idVendeur == null || idVendeur.trim().isEmpty()) {
            // Pour le débogage, attribuer un ID vendeur par défaut
            idVendeur = "debug-vendeur-id";
            logger.info("Aucun ID vendeur, utilisation de la valeur par défaut: {}", idVendeur);
        }

        // Vérification du nom
        if (boutique.getNom() == null || boutique.getNom().trim().isEmpty()) {
            logger.error("ERREUR: Nom manquant ou vide");
            return ResponseEntity.badRequest().body("Le nom de la boutique est obligatoire");
        }

        try {
            // Associer l'ID boutique et vendeur
            boutique.setId_boutique(boutiqueId);
            boutique.setVendeurId(idVendeur);

            logger.info("Tentative de mise à jour de la boutique...");
            Boutique updatedBoutique = boutiqueService.mettreAJourBoutique(boutiqueId, boutique, idVendeur);

            logger.info("Boutique mise à jour avec succès");
            logger.info("FIN TRAITEMENT REQUÊTE PUT /api/boutiques/{}", boutiqueId);
            logger.info("===============================================");

            return ResponseEntity.ok(updatedBoutique);
        } catch (Exception e) {
            logger.error("ERREUR pendant la mise à jour: {}", e.getMessage(), e);
            logger.info("===============================================");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de la mise à jour: " + e.getMessage());
        }
    }

    /**
     * Obtenir les catégories d'une boutique
     */
    @GetMapping("/{boutiqueId}/categories")
    public ResponseEntity<Set<Categorie>> getBoutiqueCategories(@PathVariable Integer boutiqueId) {
        try {
            return ResponseEntity.ok(boutiqueService.getBoutiqueCategories(boutiqueId));
        } catch (RuntimeException e) {
            logger.error("Erreur lors de la récupération des catégories pour la boutique {}", boutiqueId, e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Ajouter une catégorie à une boutique
     */
    @PostMapping("/{boutiqueId}/categories/{categorieId}")
    public ResponseEntity<Boutique> addCategorieToBoutique(
            @PathVariable Integer boutiqueId,
            @PathVariable String categorieId,
            @RequestHeader(value = "X-Vendeur-ID", required = false) String idVendeur) {

        // Fallback pour le débogage
        if (StringUtils.isBlank(idVendeur)) {
            idVendeur = "debug-vendeur-id";
            logger.info("Aucun ID vendeur fourni, utilisation de la valeur par défaut: {}", idVendeur);
        }

        // Vérifier que le vendeur est propriétaire de la boutique
        try {
            Boutique boutique = boutiqueService.getBoutiquesByVendeur(idVendeur)
                    .stream()
                    .filter(b -> b.getId_boutique().equals(boutiqueId))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Boutique non trouvée ou vous n'êtes pas le propriétaire"));

            return ResponseEntity.ok(boutiqueService.addCategorieToBoutique(boutiqueId, categorieId));
        } catch (RuntimeException e) {
            logger.error("Erreur lors de l'ajout de la catégorie {} à la boutique {}", categorieId, boutiqueId, e);
            return ResponseEntity.notFound().build();
        }
    }
    // Ajouter à BoutiqueController.java
    @GetMapping("/{idBoutique}/vendeur-id")
    public ResponseEntity<String> getVendeurIdForBoutique(@PathVariable Long idBoutique) {
        logger.info("GET /api/boutiques/{}/vendeur-id - Récupération de l'ID vendeur", idBoutique);

        try {
            // Convertir Long en Integer si nécessaire (adapter selon votre modèle)
            Integer boutiqueId = idBoutique.intValue();

            String vendeurId = boutiqueService.getVendeurIdForBoutique(idBoutique);
            if (vendeurId == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(vendeurId);
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération de l'ID vendeur pour la boutique {}", idBoutique, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur: " + e.getMessage());
        }
    }

    // Ajouter dans BoutiqueController.java
    @GetMapping("/{idBoutique}/vendeur")
    public ResponseEntity<String> getVendeurIdByBoutiqueId(@PathVariable Long idBoutique) {
        logger.info("GET /api/boutiques/{}/vendeur - Récupération de l'ID vendeur", idBoutique);

        try {
            // Convertir Long en Integer car la structure interne utilise Integer
            Integer boutiqueId = idBoutique.intValue();

            // Rechercher la boutique par ID
            Boutique boutique = boutiqueService.getBoutiqueById(boutiqueId);

            if (boutique == null) {
                logger.warn("Boutique non trouvée avec l'ID: {}", idBoutique);
                return ResponseEntity.notFound().build();
            }

            logger.info("ID vendeur trouvé pour la boutique {}: {}", idBoutique, boutique.getVendeurId());
            return ResponseEntity.ok(boutique.getVendeurId());

        } catch (Exception e) {
            logger.error("Erreur lors de la récupération de l'ID vendeur pour la boutique {}: {}",
                    idBoutique, e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Erreur: " + e.getMessage());
        }
    }

    // Ajouter également les autres endpoints manquants
    @GetMapping("/vendeurs/{idVendeur}/boutiques-ids")
    public ResponseEntity<List<Long>> getBoutiqueIdsByVendeurId(@PathVariable String idVendeur) {
        logger.info("GET /api/boutiques/vendeurs/{}/boutiques-ids - Récupération des IDs de boutiques", idVendeur);

        try {
            // Récupérer les boutiques du vendeur
            List<Boutique> boutiques = boutiqueService.getBoutiquesByVendeur(idVendeur);

            if (boutiques.isEmpty()) {
                logger.info("Aucune boutique trouvée pour le vendeur: {}", idVendeur);
                return ResponseEntity.ok(Collections.emptyList());
            }

            // Convertir les IDs en Long pour assurer la compatibilité
            List<Long> boutiqueIds = boutiques.stream()
                    .map(b -> Long.valueOf(b.getId_boutique()))
                    .collect(Collectors.toList());

            logger.info("Boutiques trouvées pour le vendeur {}: {}", idVendeur, boutiqueIds.size());
            return ResponseEntity.ok(boutiqueIds);

        } catch (Exception e) {
            logger.error("Erreur lors de la récupération des IDs de boutiques pour le vendeur {}: {}",
                    idVendeur, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Collections.emptyList());
        }
    }
    /**
     * Supprimer une catégorie d'une boutique
     */
    @DeleteMapping("/{boutiqueId}/categories/{categorieId}")
    public ResponseEntity<Boutique> removeCategorieFromBoutique(
            @PathVariable Integer boutiqueId,
            @PathVariable String categorieId,
            @RequestHeader(value = "X-Vendeur-ID", required = false) String idVendeur) {

        // Fallback pour le débogage
        if (StringUtils.isBlank(idVendeur)) {
            idVendeur = "debug-vendeur-id";
            logger.info("Aucun ID vendeur fourni, utilisation de la valeur par défaut: {}", idVendeur);
        }

        // Vérifier que le vendeur est propriétaire de la boutique
        try {
            Boutique boutique = boutiqueService.getBoutiquesByVendeur(idVendeur)
                    .stream()
                    .filter(b -> b.getId_boutique().equals(boutiqueId))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Boutique non trouvée ou vous n'êtes pas le propriétaire"));

            return ResponseEntity.ok(boutiqueService.removeCategorieFromBoutique(boutiqueId, categorieId));
        } catch (RuntimeException e) {
            logger.error("Erreur lors de la suppression de la catégorie {} de la boutique {}", categorieId, boutiqueId, e);
            return ResponseEntity.notFound().build();
        }
    }
// Copie cette méthode complète dans ta classe BoutiqueController

    @GetMapping("/search-with-products")
    public ResponseEntity<Map<String, Object>> searchBoutiquesWithProducts(@RequestParam String q) {
        logger.info("🔍 RECHERCHE INTELLIGENTE: {}", q);

        try {
            // 1. RECHERCHE INTELLIGENTE DES PRODUITS avec Gemini AI
            // Remplace dans searchBoutiquesWithProducts :
            List<ProduitDTO> produitsTrouves = produitService.searchProduitsIntelligent(q);
            logger.info("🛍️ Produits trouvés: {}", produitsTrouves.size());

            // 2. EXTRAIRE LES IDs DE STOCKS des produits trouvés
            Set<Long> stockIds = produitsTrouves.stream()
                    .map(ProduitDTO::getIdStock)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            logger.info("📦 Stocks concernés: {}", stockIds);

            // 3. TROUVER LES BOUTIQUES QUI POSSÈDENT CES STOCKS
            List<Boutique> toutesLesBoutiques = boutiqueService.getAllBoutiques();
            List<Map<String, Object>> boutiquesAvecProduits = new ArrayList<>();

            for (Boutique boutique : toutesLesBoutiques) {
                // Récupérer les stocks de cette boutique
                Set<Long> stocksDeLaBoutique = getStockIdsDeBoutique(boutique);

                // Trouver l'intersection : quels stocks de la boutique contiennent nos produits ?
                Set<Long> stocksCommuns = stockIds.stream()
                        .filter(stocksDeLaBoutique::contains)
                        .collect(Collectors.toSet());

                if (!stocksCommuns.isEmpty()) {
                    // Cette boutique a des produits qui matchent !
                    List<ProduitDTO> produitsDeCetteBoutique = produitsTrouves.stream()
                            .filter(p -> stocksCommuns.contains(p.getIdStock()))
                            .collect(Collectors.toList());

                    Map<String, Object> boutiqueInfo = new HashMap<>();
                    boutiqueInfo.put("boutique", boutique);
                    boutiqueInfo.put("produits", produitsDeCetteBoutique);
                    boutiqueInfo.put("nombreProduits", produitsDeCetteBoutique.size());

                    boutiquesAvecProduits.add(boutiqueInfo);

                    logger.info("🏪 Boutique '{}' contient {} produit(s) matchant",
                            boutique.getNom(), produitsDeCetteBoutique.size());
                }
            }

            // 4. CRÉER LA RÉPONSE INTELLIGENTE
            Map<String, Object> response = new HashMap<>();
            response.put("query", q);
            response.put("aiEnhanced", true);
            response.put("timestamp", LocalDateTime.now());

            // Données principales
            response.put("produits", produitsTrouves);
            response.put("boutiquesAvecProduits", boutiquesAvecProduits);

            // Statistiques
            response.put("nombreProduits", produitsTrouves.size());
            response.put("nombreBoutiques", boutiquesAvecProduits.size());
            response.put("nombreStocks", stockIds.size());

            // Message intelligent
            String message = createIntelligentMessage(q, produitsTrouves.size(), boutiquesAvecProduits.size());
            response.put("message", message);

            logger.info("✅ {}", message);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("❌ Erreur recherche intelligente: {}", e.getMessage(), e);
            return createFallbackResponse(q, e);
        }
    }

// Méthodes helper à ajouter aussi dans BoutiqueController

    private Set<Long> getStockIdsDeBoutique(Boutique boutique) {
        try {
            // VERSION SIMPLE POUR COMMENCER - À AMÉLIORER SELON TA STRUCTURE
            // Cette version assume que tous les produits peuvent être dans toutes les boutiques
            // Tu devras la remplacer par ta vraie logique Boutique->Stock

            List<ProduitDTO> tousProduits = produitService.getAllProduits();
            return tousProduits.stream()
                    .map(ProduitDTO::getIdStock)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            // TODO: Remplace par ta vraie logique, par exemple :
            // return stockService.getStockIdsByBoutiqueId(boutique.getId_boutique());

        } catch (Exception e) {
            logger.error("Erreur récupération stocks boutique {}: {}", boutique.getId_boutique(), e.getMessage());
            return Collections.emptySet();
        }
    }

    private String createIntelligentMessage(String query, int nombreProduits, int nombreBoutiques) {
        if (nombreProduits == 0) {
            return String.format("🔍 Aucun produit trouvé pour '%s'. Essayez d'autres mots-clés.", query);
        } else if (nombreBoutiques == 0) {
            return String.format("🛍️ %d produit(s) trouvé(s) pour '%s', mais aucune boutique disponible.", nombreProduits, query);
        } else if (nombreBoutiques == 1) {
            return String.format("🎯 Parfait ! %d produit(s) trouvé(s) dans 1 boutique pour '%s'", nombreProduits, query);
        } else {
            return String.format("🎯 Excellent ! %d produit(s) trouvé(s) dans %d boutiques pour '%s'", nombreProduits, nombreBoutiques, query);
        }
    }

    private ResponseEntity<Map<String, Object>> createFallbackResponse(String query, Exception e) {
        try {
            List<ProduitDTO> fallbackProduits = produitService.searchProduits(query);

            Map<String, Object> response = new HashMap<>();
            response.put("query", query);
            response.put("produits", fallbackProduits);
            response.put("boutiquesAvecProduits", Collections.emptyList());
            response.put("aiEnhanced", false);
            response.put("error", "IA non disponible, recherche normale utilisée");
            response.put("nombreProduits", fallbackProduits.size());
            response.put("nombreBoutiques", 0);
            response.put("message", String.format("🔄 Recherche simple: %d produit(s) trouvé(s)", fallbackProduits.size()));

            return ResponseEntity.ok(response);

        } catch (Exception fallbackError) {
            logger.error("❌ Erreur fallback aussi: {}", fallbackError.getMessage());

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("query", query);
            errorResponse.put("error", "Erreur complète de recherche: " + e.getMessage());
            errorResponse.put("produits", Collections.emptyList());
            errorResponse.put("nombreProduits", 0);
            errorResponse.put("nombreBoutiques", 0);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PostMapping("/{id}/images/url")
    public ResponseEntity<?> associerImageBoutique(
            @PathVariable("id") Integer boutiqueId,
            @RequestBody Map<String, String> payload,
            @RequestHeader(value = "X-Vendeur-ID", required = false) String vendeurId) {

        String imageUrl = payload.get("url");
        if (imageUrl == null || imageUrl.isEmpty()) {
            return ResponseEntity.badRequest().body("URL de l'image manquante");
        }

        try {
            logger.info("Association image [{}] à la boutique {}", imageUrl, boutiqueId);

            Boutique boutique = boutiqueService.getBoutiqueById(boutiqueId);
            if (boutique == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Boutique introuvable");
            }

            // Optionnel : vérifier que le vendeur correspond
            if (vendeurId != null && !vendeurId.equals(boutique.getVendeurId())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Vous n'êtes pas propriétaire de cette boutique");
            }

            // Associer l'image
            boutique.setBoutique_img(imageUrl);
            boutiqueService.mettreAJourBoutique(boutiqueId, boutique, vendeurId);


            return ResponseEntity.ok(Collections.singletonMap("message", "Image associée avec succès"));

        } catch (Exception e) {
            logger.error("Erreur lors de l'association de l'image à la boutique", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erreur: " + e.getMessage());
        }
    }

    @GetMapping("/{boutiqueId}/images")
    public ResponseEntity<List<String>> getBoutiqueImages(@PathVariable Integer boutiqueId) {
        try {
            // Exemple : récupérer l'URL des images de la boutique
            List<String> images = boutiqueService.getImagesByBoutiqueId(boutiqueId);
            return ResponseEntity.ok(images);
        } catch (Exception e) {
            logger.error("Erreur récupération images boutique {}", boutiqueId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.emptyList());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<BoutiqueDTO> getBoutiqueById(@PathVariable Integer id) {
        try {
            Boutique boutique = boutiqueService.getBoutiqueById(id);
            BoutiqueDTO dto = boutiqueService.convertToDTO(boutique);
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération de la boutique {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }


}