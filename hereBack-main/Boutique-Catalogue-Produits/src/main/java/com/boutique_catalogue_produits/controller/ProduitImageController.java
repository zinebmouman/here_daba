
// src/main/java/com/boutique_catalogue_produits/controller/ProduitImageController.java - VERSION MISE À JOUR
package com.boutique_catalogue_produits.controller;

import com.boutique_catalogue_produits.dto.ProduitImageDTO;
import com.boutique_catalogue_produits.service.MinIOService;
import com.boutique_catalogue_produits.service.ProduitImageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/produits/{produitId}/images")
@CrossOrigin(origins = {"http://localhost:3000", "http://10.0.2.2:8081", "http://localhost:8081"})
public class ProduitImageController {

    private static final Logger log = LoggerFactory.getLogger(ProduitImageController.class);

    @Autowired
    private ProduitImageService imageService;

    @Autowired
    private MinIOService minioService;

    @GetMapping
    public ResponseEntity<List<ProduitImageDTO>> getImagesByProduitId(@PathVariable Long produitId) {
        log.info("📷 Récupération des images pour le produit ID: {}", produitId);
        return ResponseEntity.ok(imageService.getImagesByProduitId(produitId));
    }

    /**
     * ✅ NOUVEAU: Upload d'une image directement vers MinIO avec association au produit
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> uploadImageToProduit(
            @PathVariable Long produitId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "imagePrincipale", defaultValue = "false") Boolean imagePrincipale,
            @RequestParam(value = "vendeurId", required = false) String vendeurId) {

        log.info("📤 Upload image pour produit {} - Vendeur: {}, Fichier: {}",
                produitId, vendeurId, file.getOriginalFilename());

        Map<String, Object> response = new HashMap<>();

        try {
            // Validation du fichier
            if (file.isEmpty()) {
                response.put("success", false);
                response.put("message", "Fichier vide");
                return ResponseEntity.badRequest().body(response);
            }

            if (!file.getContentType().startsWith("image/")) {
                log.warn("Type de fichier non supporté: {}", file.getContentType());
                response.put("success", false);
                response.put("message", "Type de fichier non supporté: " + file.getContentType());
                return ResponseEntity.badRequest().body(response);
            }

            // 1. Upload vers MinIO
            String imageUrl = minioService.uploadFile(file);
            log.info("✅ Image uploadée vers MinIO: {}", imageUrl);

            // 2. Extraire le nom du fichier de l'URL pour le chemin
            String fileName = extractFileNameFromUrl(imageUrl);

            // 3. Sauvegarder en base de données
            ProduitImageDTO imageDTO = imageService.ajouterImageAuProduit(
                    produitId, fileName, imageUrl, imagePrincipale);

            // 4. Réponse de succès
            response.put("success", true);
            response.put("message", "Image uploadée et associée au produit avec succès");
            response.put("image", imageDTO);
            response.put("url", imageUrl);

            log.info("✅ Image associée au produit {} avec succès", produitId);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            log.error("❌ Erreur upload image pour produit {}: {}", produitId, e.getMessage(), e);

            response.put("success", false);
            response.put("message", "Erreur lors de l'upload: " + e.getMessage());
            response.put("error", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * ✅ NOUVEAU: Upload de plusieurs images simultanément
     */
    @PostMapping(value = "/multiple", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> uploadMultipleImagesToProduit(
            @PathVariable Long produitId,
            @RequestParam("files") MultipartFile[] files,
            @RequestParam(value = "indexImagePrincipale", defaultValue = "0") Integer indexImagePrincipale,
            @RequestParam(value = "vendeurId", required = false) String vendeurId) {

        log.info("📤 Upload de {} images pour produit {} - Vendeur: {}",
                files.length, produitId, vendeurId);

        Map<String, Object> response = new HashMap<>();

        try {
            if (files.length == 0) {
                response.put("success", false);
                response.put("message", "Aucun fichier fourni");
                return ResponseEntity.badRequest().body(response);
            }

            if (indexImagePrincipale < 0 || indexImagePrincipale >= files.length) {
                log.warn("Index d'image principale invalide: {}", indexImagePrincipale);
                indexImagePrincipale = 0;
            }

            List<ProduitImageDTO> uploadedImages = new ArrayList<>();
            List<String> errorMessages = new ArrayList<>();

            // Upload chaque fichier
            for (int i = 0; i < files.length; i++) {
                MultipartFile file = files[i];

                if (file.isEmpty()) {
                    errorMessages.add("Fichier " + i + " est vide");
                    continue;
                }

                if (!file.getContentType().startsWith("image/")) {
                    errorMessages.add("Fichier " + i + " n'est pas une image: " + file.getContentType());
                    continue;
                }

                try {
                    // Upload vers MinIO
                    String imageUrl = minioService.uploadFile(file);
                    String fileName = extractFileNameFromUrl(imageUrl);

                    // Marquer comme image principale si c'est l'index choisi
                    boolean isImagePrincipale = (i == indexImagePrincipale);

                    // Sauvegarder en base
                    ProduitImageDTO imageDTO = imageService.ajouterImageAuProduit(
                            produitId, fileName, imageUrl, isImagePrincipale);

                    uploadedImages.add(imageDTO);

                    log.info("✅ Image {} uploadée: {}", i, imageUrl);

                } catch (Exception e) {
                    log.error("❌ Erreur upload image {}: {}", i, e.getMessage());
                    errorMessages.add("Erreur fichier " + i + ": " + e.getMessage());
                }
            }

            // Préparer la réponse
            response.put("success", uploadedImages.size() > 0);
            response.put("message", uploadedImages.size() + " image(s) uploadée(s) avec succès");
            response.put("images", uploadedImages);
            response.put("uploadedCount", uploadedImages.size());
            response.put("totalFiles", files.length);

            if (!errorMessages.isEmpty()) {
                response.put("errors", errorMessages);
                response.put("partialSuccess", true);
            }

            HttpStatus status = uploadedImages.size() > 0 ? HttpStatus.CREATED : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status).body(response);

        } catch (Exception e) {
            log.error("❌ Erreur upload multiple images pour produit {}: {}", produitId, e.getMessage(), e);

            response.put("success", false);
            response.put("message", "Erreur lors de l'upload: " + e.getMessage());
            response.put("error", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PutMapping("/{imageId}")
    public ResponseEntity<ProduitImageDTO> updateImage(
            @PathVariable Long produitId,
            @PathVariable Long imageId,
            @RequestBody ProduitImageDTO imageDTO) {
        log.info("📝 Mise à jour de l'image ID: {} pour le produit ID: {}", imageId, produitId);
        return imageService.updateImage(imageId, imageDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * ✅ MISE À JOUR: Suppression avec nettoyage MinIO
     */
    @DeleteMapping("/{imageId}")
    public ResponseEntity<Map<String, Object>> deleteImage(
            @PathVariable Long produitId,
            @PathVariable Long imageId) {

        log.info("🗑️ Suppression de l'image ID: {} pour le produit ID: {}", imageId, produitId);

        Map<String, Object> response = new HashMap<>();

        try {
            // 1. Récupérer l'image pour obtenir l'URL
            ProduitImageDTO imageDTO = imageService.getImageById(imageId);

            if (imageDTO == null) {
                response.put("success", false);
                response.put("message", "Image non trouvée");
                return ResponseEntity.notFound().build();
            }

            // 2. Supprimer de MinIO
            if (imageDTO.getUrl() != null) {
                try {
                    minioService.deleteFileFromUrl(imageDTO.getUrl());
                    log.info("✅ Image supprimée de MinIO: {}", imageDTO.getUrl());
                } catch (Exception e) {
                    log.warn("⚠️ Erreur suppression MinIO (continuant): {}", e.getMessage());
                }
            }

            // 3. Supprimer de la base de données
            imageService.supprimerImage(imageId);

            response.put("success", true);
            response.put("message", "Image supprimée avec succès");
            response.put("deletedId", imageId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Erreur suppression image ID: {} pour produit ID: {}", imageId, produitId, e);

            response.put("success", false);
            response.put("message", "Erreur lors de la suppression: " + e.getMessage());
            response.put("error", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // =============== MÉTHODES UTILITAIRES ===============

    private String extractFileNameFromUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return null;
        }

        try {
            String[] parts = imageUrl.split("/");
            if (parts.length >= 2) {
                return parts[parts.length - 2] + "/" + parts[parts.length - 1];
            }
            return parts[parts.length - 1];
        } catch (Exception e) {
            log.warn("⚠️ Impossible d'extraire le nom de fichier de l'URL: {}", imageUrl);
            return imageUrl; // Fallback
        }
    }


    @PostMapping(value = "/url", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> associateImageUrlToProduit(
            @PathVariable Long produitId,
            @RequestBody Map<String, Object> imageData) {

        log.info("🔗 Association URL image pour produit {} - Data: {}", produitId, imageData);

        Map<String, Object> response = new HashMap<>();

        try {
            String url = (String) imageData.get("url");
            String cheminFichier = (String) imageData.get("cheminFichier");
            Boolean imagePrincipale = (Boolean) imageData.getOrDefault("imagePrincipale", false);

            if (url == null || url.isEmpty()) {
                response.put("success", false);
                response.put("message", "URL manquante");
                return ResponseEntity.badRequest().body(response);
            }

            // Sauvegarder en base de données
            ProduitImageDTO imageDTO = imageService.ajouterImageAuProduit(
                    produitId, cheminFichier, url, imagePrincipale);

            response.put("success", true);
            response.put("message", "URL d'image associée au produit avec succès");
            response.put("image", imageDTO);
            response.put("url", url);

            log.info("✅ URL image associée au produit {} avec succès", produitId);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            log.error("❌ Erreur association URL image pour produit {}: {}", produitId, e.getMessage(), e);

            response.put("success", false);
            response.put("message", "Erreur lors de l'association: " + e.getMessage());
            response.put("error", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}