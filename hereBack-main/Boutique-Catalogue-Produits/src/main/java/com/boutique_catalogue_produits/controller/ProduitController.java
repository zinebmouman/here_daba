package com.boutique_catalogue_produits.controller;

import com.boutique_catalogue_produits.dto.ProduitDTO;
import com.boutique_catalogue_produits.dto.SearchCriteria;
import com.boutique_catalogue_produits.model.Categorie;
import com.boutique_catalogue_produits.service.FileStorageService;
import com.boutique_catalogue_produits.service.GeminiAIService;
import com.boutique_catalogue_produits.service.ProduitImageService;
import com.boutique_catalogue_produits.service.ProduitService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/produits")
public class ProduitController {
    private static final Logger logger = LoggerFactory.getLogger(ProduitController.class);
    @Autowired
    private FileStorageService fileStorageService;
    @Autowired
    private GeminiAIService geminiAIService;
    @Autowired
    private ProduitImageService produitImageService;
    @Autowired
    private ProduitService produitService;

    // GET tous les produits
    @GetMapping
    public ResponseEntity<List<ProduitDTO>> getAllProduits() {
        List<ProduitDTO> produits = produitService.getAllProduits();
        return ResponseEntity.ok(produits);
    }

    // GET un produit par ID
    @GetMapping("/{id}")
    public ResponseEntity<ProduitDTO> getProduitById(@PathVariable Long id) {
        return produitService.getProduitById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // GET produits par stock
    @GetMapping("/stock/{idStock}")
    public ResponseEntity<List<ProduitDTO>> getProduitsByStock(@PathVariable Long idStock) {
        List<ProduitDTO> produits = produitService.getProduitsByStock(idStock);
        return ResponseEntity.ok(produits);
    }
    @GetMapping("/proche-expiration")
    public ResponseEntity<List<ProduitDTO>> getProduitsProcheExpiration() {
        List<ProduitDTO> produitsProcheExpiration = produitService.getProduitsProcheExpiration();
        return ResponseEntity.ok(produitsProcheExpiration);
    }

    // GET produits par catégorie
    @GetMapping("/categorie/{idCategorie}")
    public ResponseEntity<List<ProduitDTO>> getProduitsByCategorie(@PathVariable String idCategorie) {
        List<ProduitDTO> produits = produitService.getProduitsByCategorie(idCategorie);
        return ResponseEntity.ok(produits);
    }
    // GET produits en alerte de stock
    @GetMapping("/en-alerte")
    public ResponseEntity<List<ProduitDTO>> getProduitsEnAlerte() {
        List<ProduitDTO> produits = produitService.getProduitsEnAlerte();
        return ResponseEntity.ok(produits);
    }

    // POST créer un produit
    @PostMapping
    public ResponseEntity<ProduitDTO> createProduit(@RequestBody ProduitDTO produitDTO) {
        ProduitDTO createdProduit = produitService.createProduit(produitDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdProduit);
    }

    // POST créer un produit avec images
    @PostMapping(value = "/avec-images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProduitDTO> creerProduitAvecImages(
            @RequestParam("nomProduit") String nomProduit,
            @RequestParam("quantite") Integer quantite,
            @RequestParam("description") String description,
            @RequestParam("detail") String detail,
            @RequestParam("seuilCritique") Double seuilCritique,
            @RequestParam("prix") Double prix,
            @RequestParam(value = "dateExpiration", required = false) String dateExpirationStr,
            @RequestParam("idStock") Long idStock,
            @RequestParam("idCategorie") String idCategorie,
            @RequestParam(value = "idReduction", required = false) Long idReduction,
            @RequestParam(value = "images", required = false) MultipartFile[] fichiers,
            @RequestParam(value = "imagePrincipale", required = false, defaultValue = "0") Integer indexImagePrincipale) {

        try {
            // Créer l'objet produit
            ProduitDTO produitDTO = new ProduitDTO();
            produitDTO.setNomProduit(nomProduit);
            produitDTO.setQuantite(quantite);
            produitDTO.setDescription(description);
            produitDTO.setDetail(detail);
            produitDTO.setSeuilCritique(seuilCritique);
            produitDTO.setPrix(new BigDecimal(prix));

            if (dateExpirationStr != null && !dateExpirationStr.isEmpty()) {
                produitDTO.setDateExpiration(LocalDate.parse(dateExpirationStr));
            }

            produitDTO.setIdStock(idStock);

            // Stocke l'ID de catégorie tel quel, sans conversion
            produitDTO.setIdCategorie(idCategorie);

            produitDTO.setIdReduction(idReduction);

            // Enregistrer le produit sans images d'abord
            ProduitDTO produitCree = produitService.createProduit(produitDTO);

            // Ajouter les images
            if (fichiers != null && fichiers.length > 0) {
                for (int i = 0; i < fichiers.length; i++) {
                    MultipartFile fichier = fichiers[i];
                    if (fichier != null && !fichier.isEmpty()) {
                        boolean estPrincipale = (i == indexImagePrincipale);

                        // Appeler le service pour stocker l'image et l'associer au produit
                        String nomFichier = fileStorageService.stockerFichier(fichier);
                        String url = fileStorageService.genererUrl(nomFichier);

                        produitImageService.ajouterImageAuProduit(
                                produitCree.getId(), nomFichier, url, estPrincipale);
                    }
                }

                // Récupérer le produit avec ses images
                produitCree = produitService.getProduitById(produitCree.getId()).orElse(produitCree);
            }

            return ResponseEntity.status(HttpStatus.CREATED).body(produitCree);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

    // PUT mettre à jour un produit
    @PutMapping("/{id}")
    public ResponseEntity<ProduitDTO> updateProduit(
            @PathVariable Long id,
            @RequestBody ProduitDTO produitDTO) {
        return produitService.updateProduit(id, produitDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // PUT ajuster le stock d'un produit
    @PutMapping("/{id}/ajuster-stock")
    public ResponseEntity<ProduitDTO> ajusterStock(
            @PathVariable Long id,
            @RequestParam Integer quantiteAjustement) {
        return produitService.ajusterStock(id, quantiteAjustement)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    @PatchMapping("/{id}/ajustement")
    public ResponseEntity<ProduitDTO> ajusterQuantite(
            @PathVariable Long id,
            @RequestParam Integer quantite) {
        return produitService.ajusterStock(id, quantite)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // DELETE supprimer un produit
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduit(@PathVariable Long id) {
        boolean deleted = produitService.deleteProduit(id);
        if (deleted) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    // AJOUTE cette méthode dans ton ProduitController
    @GetMapping(params = "search")
    public ResponseEntity<List<ProduitDTO>> searchProduits(@RequestParam("search") String search) {
        List<ProduitDTO> produits = produitService.searchProduits(search);
        return ResponseEntity.ok(produits);
    }

    @PostMapping("/search-intelligent")
    public ResponseEntity<Map<String, Object>> searchIntelligent(@RequestParam String query) {
        logger.info("🚀 Recherche intelligente reçue: {}", query);

        try {
            List<ProduitDTO> results = produitService.searchProduitsIntelligent(query);

            Map<String, Object> response = new HashMap<>();
            response.put("query", query);
            response.put("results", results);
            response.put("count", results.size());
            response.put("aiEnhanced", true);
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("❌ Erreur recherche intelligente: {}", e.getMessage());

            // Fallback vers recherche normale
            List<ProduitDTO> fallbackResults = produitService.searchProduits(query);

            Map<String, Object> response = new HashMap<>();
            response.put("query", query);
            response.put("results", fallbackResults);
            response.put("count", fallbackResults.size());
            response.put("aiEnhanced", false);
            response.put("error", "IA non disponible, recherche normale utilisée");

            return ResponseEntity.ok(response);
        }
    }
    // Ajoute cette méthode simple dans ProduitController pour tester l'IA :

    @GetMapping("/test-gemini")
    public ResponseEntity<Map<String, Object>> testGemini(@RequestParam String q) {
        logger.info("🧪 Test simple Gemini: {}", q);

        Map<String, Object> response = new HashMap<>();

        try {
            // Test direct du service Gemini
            SearchCriteria criteria = geminiAIService.analyzeSearchQuery(q);

            response.put("query", q);
            response.put("success", true);
            response.put("geminiWorking", true);
            response.put("keywords", criteria.getKeywords());
            response.put("category", criteria.getCategorie());
            response.put("searchType", criteria.getSearchType());

            logger.info("✅ Gemini fonctionne ! Keywords: {}", criteria.getKeywords());

        } catch (Exception e) {
            response.put("query", q);
            response.put("success", false);
            response.put("geminiWorking", false);
            response.put("error", e.getMessage());

            logger.error("❌ Gemini ne fonctionne pas: {}", e.getMessage());
        }

        return ResponseEntity.ok(response);
    }
    @GetMapping("/search-intelligent")
    public ResponseEntity<Map<String, Object>> searchIntelligentGet(@RequestParam String q) {
        return searchIntelligent(q);
    }
    @GetMapping("/test-analyze")
    public ResponseEntity<Map<String, Object>> testAnalyze(@RequestParam String query) {
        logger.info("🧪 Test d'analyse: {}", query);

        try {
            SearchCriteria criteria = geminiAIService.analyzeSearchQuery(query);
            Map<String, Object> response = new HashMap<>();
            response.put("query", query);
            response.put("criteria", criteria);
            response.put("success", true);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            error.put("query", query);
            return ResponseEntity.status(500).body(error);
        }
    }


    @GetMapping("/debug-similarity")
    public ResponseEntity<Map<String, Object>> debugSimilarity(@RequestParam String q) {
        logger.info("🔬 Debug similarité pour: {}", q);

        Map<String, Object> response = new HashMap<>();

        try {
            // Test 1: Compter tous les produits
            List<ProduitDTO> tousProduits = produitService.getAllProduits();
            response.put("totalProduits", tousProduits.size());

            // Test 2: Tester la similarité directement
            double similariteAvecCaftan = 0.0;
            String nomCaftan = "";

            for (ProduitDTO produit : tousProduits) {
                if (produit.getNomProduit().toLowerCase().contains("caftan")) {
                    similariteAvecCaftan = calculerSimilariteTest(q.toLowerCase(), "caftan");
                    nomCaftan = produit.getNomProduit();
                    break;
                }
            }

            // Test 3: Appeler la méthode intelligente
            List<ProduitDTO> produitsTrouves = produitService.searchProduitsIntelligent(q);

            response.put("query", q);
            response.put("similariteAvecCaftan", Math.round(similariteAvecCaftan * 100) + "%");
            response.put("produitCaftanTrouve", nomCaftan);
            response.put("seuilAtteint", similariteAvecCaftan > 0.6 ? "OUI" : "NON");
            response.put("resultatsIntelligents", produitsTrouves.size()); // FIX ICI !

            // Test 4: Détails des produits trouvés
            List<String> nomsProduitsIntelligents = produitsTrouves.stream() // FIX ICI !
                    .map(ProduitDTO::getNomProduit)
                    .collect(Collectors.toList());
            response.put("produitsIntelligentsTrouves", nomsProduitsIntelligents);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            error.put("query", q);
            return ResponseEntity.status(500).body(error);
        }
    }
    // Méthode de test simple pour la similarité
    private double calculerSimilariteTest(String s1, String s2) {
        if (s1.equals(s2)) return 1.0;

        // Calcul simple de distance
        int distance = 0;
        int minLen = Math.min(s1.length(), s2.length());
        int maxLen = Math.max(s1.length(), s2.length());

        for (int i = 0; i < minLen; i++) {
            if (s1.charAt(i) != s2.charAt(i)) {
                distance++;
            }
        }
        distance += Math.abs(s1.length() - s2.length());

        return 1.0 - ((double) distance / maxLen);
    }

    @GetMapping("/{produitId}/images/serve/{filename:.+}")
    public ResponseEntity<Resource> serveImage(
            @PathVariable Long produitId,
            @PathVariable String filename) {

        logger.info("🖼️ Demande d'image: {} pour produit: {}", filename, produitId);

        try {
            // Chemin vers les images
            Path imagePath = Paths.get("uploads", "images", filename).toAbsolutePath().normalize();

            logger.info("📁 Chemin complet: {}", imagePath);

            Resource resource = new UrlResource(imagePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                // Déterminer le type de contenu
                String contentType = Files.probeContentType(imagePath);
                if (contentType == null) {
                    if (filename.toLowerCase().endsWith(".jpg") || filename.toLowerCase().endsWith(".jpeg")) {
                        contentType = "image/jpeg";
                    } else if (filename.toLowerCase().endsWith(".png")) {
                        contentType = "image/png";
                    } else {
                        contentType = "application/octet-stream";
                    }
                }

                logger.info("✅ Image trouvée et servie: {}", filename);

                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                logger.warn("❌ Image non trouvée: {}", imagePath);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception ex) {
            logger.error("💥 Erreur lors du service de l'image {}: {}", filename, ex.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    // Méthode de diagnostic pour les images
    @GetMapping("/images/test-path")
    public ResponseEntity<Map<String, Object>> testImagePath() {
        Map<String, Object> response = new HashMap<>();

        try {
            Path uploadsPath = Paths.get("uploads", "images").toAbsolutePath().normalize();
            boolean exists = Files.exists(uploadsPath);

            response.put("imagePath", uploadsPath.toString());
            response.put("directoryExists", exists);
            response.put("workingDirectory", System.getProperty("user.dir"));

            if (exists) {
                long fileCount = Files.list(uploadsPath).count();
                response.put("fileCount", fileCount);

                // Lister les fichiers
                List<String> files = Files.list(uploadsPath)
                        .limit(10)
                        .map(path -> path.getFileName().toString())
                        .collect(Collectors.toList());
                response.put("files", files);
            }

            logger.info("🔍 Test chemin images: {}", response);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("error", e.getMessage());
            logger.error("💥 Erreur test chemin: {}", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping("/boutique/{id}")
    public ResponseEntity<List<ProduitDTO>> getProduitsByBoutique(@PathVariable Integer id) {
        List<ProduitDTO> produits = produitService.getProduitsByBoutique(id);
        return ResponseEntity.ok(produits);
    }

}