// src/main/java/com/boutique_catalogue_produits/controller/MinIOUploadController.java
package com.boutique_catalogue_produits.controller;

import com.boutique_catalogue_produits.service.MinIOService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/upload")
@CrossOrigin(origins = {"http://localhost:3000", "http://10.0.2.2:8081", "http://localhost:8081"})
public class MinIOUploadController {

    private static final Logger logger = LoggerFactory.getLogger(MinIOUploadController.class);

    @Autowired
    private MinIOService minioService;

    /**
     * Upload d'une seule image
     */
    @PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> uploadSingleImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "vendeurId", required = false) String vendeurId) {

        logger.info("📤 Upload d'une image - Vendeur: {}, Fichier: {}", vendeurId, file.getOriginalFilename());

        Map<String, Object> response = new HashMap<>();

        try {
            // Validation du fichier
            if (file.isEmpty()) {
                response.put("success", false);
                response.put("message", "Fichier vide");
                return ResponseEntity.badRequest().body(response);
            }

            // Upload vers MinIO
            String imageUrl = minioService.uploadFile(file);

            // Réponse de succès
            response.put("success", true);
            response.put("message", "Image uploadée avec succès");
            response.put("url", imageUrl);
            response.put("fileName", file.getOriginalFilename());
            response.put("size", file.getSize());
            response.put("contentType", file.getContentType());

            logger.info("✅ Image uploadée avec succès: {}", imageUrl);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("❌ Erreur upload image: {}", e.getMessage(), e);

            response.put("success", false);
            response.put("message", "Erreur lors de l'upload: " + e.getMessage());
            response.put("error", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Upload de plusieurs images
     */
    @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> uploadMultipleImages(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam(value = "vendeurId", required = false) String vendeurId) {

        logger.info("📤 Upload de {} images - Vendeur: {}", files.length, vendeurId);

        Map<String, Object> response = new HashMap<>();

        try {
            // Validation
            if (files.length == 0) {
                response.put("success", false);
                response.put("message", "Aucun fichier fourni");
                return ResponseEntity.badRequest().body(response);
            }

            // Upload vers MinIO
            List<String> imageUrls = minioService.uploadMultipleFiles(files);

            // Réponse de succès
            response.put("success", true);
            response.put("message", "Images uploadées avec succès");
            response.put("urls", imageUrls);
            response.put("count", imageUrls.size());
            response.put("totalFiles", files.length);

            logger.info("✅ {} images uploadées avec succès", imageUrls.size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("❌ Erreur upload multiple images: {}", e.getMessage(), e);

            response.put("success", false);
            response.put("message", "Erreur lors de l'upload: " + e.getMessage());
            response.put("error", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Supprimer une image
     */
    @DeleteMapping("/delete")
    public ResponseEntity<Map<String, Object>> deleteImage(
            @RequestParam("url") String imageUrl,
            @RequestParam(value = "vendeurId", required = false) String vendeurId) {

        logger.info("🗑️ Suppression image - Vendeur: {}, URL: {}", vendeurId, imageUrl);

        Map<String, Object> response = new HashMap<>();

        try {
            // Supprimer de MinIO
            minioService.deleteFileFromUrl(imageUrl);

            response.put("success", true);
            response.put("message", "Image supprimée avec succès");
            response.put("deletedUrl", imageUrl);

            logger.info("✅ Image supprimée avec succès: {}", imageUrl);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("❌ Erreur suppression image: {}", e.getMessage(), e);

            response.put("success", false);
            response.put("message", "Erreur lors de la suppression: " + e.getMessage());
            response.put("error", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Obtenir les informations d'une image
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getImageInfo(
            @RequestParam("url") String imageUrl) {

        logger.info("ℹ️ Récupération info image: {}", imageUrl);

        Map<String, Object> response = new HashMap<>();

        try {
            // Extraire le nom du fichier de l'URL
            String fileName = extractFileNameFromUrl(imageUrl);

            if (fileName == null) {
                response.put("success", false);
                response.put("message", "URL invalide");
                return ResponseEntity.badRequest().body(response);
            }

            // Obtenir les informations du fichier
            var fileInfo = minioService.getFileInfo(fileName);

            response.put("success", true);
            response.put("fileName", fileName);
            response.put("size", fileInfo.size());
            response.put("contentType", fileInfo.contentType());
            response.put("lastModified", fileInfo.lastModified());
            response.put("etag", fileInfo.etag());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("❌ Erreur récupération info image: {}", e.getMessage(), e);

            response.put("success", false);
            response.put("message", "Erreur: " + e.getMessage());
            response.put("error", e.getMessage());

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    /**
     * Obtenir les statistiques du stockage
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStorageStats(
            @RequestParam(value = "vendeurId", required = false) String vendeurId) {

        logger.info("📊 Récupération stats stockage - Vendeur: {}", vendeurId);

        try {
            Map<String, Object> stats = minioService.getBucketStats();
            stats.put("success", true);

            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            logger.error("❌ Erreur récupération stats: {}", e.getMessage(), e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Erreur: " + e.getMessage());
            response.put("error", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Test de connectivité MinIO
     */
    @GetMapping("/test")
    public ResponseEntity<Map<String, Object>> testMinIOConnection() {
        logger.info("🧪 Test de connectivité MinIO");

        Map<String, Object> response = new HashMap<>();

        try {
            // Tester la connectivité en récupérant les stats
            Map<String, Object> stats = minioService.getBucketStats();

            response.put("success", true);
            response.put("message", "Connexion MinIO OK");
            response.put("stats", stats);
            response.put("timestamp", java.time.LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("❌ Erreur test connectivité MinIO: {}", e.getMessage(), e);

            response.put("success", false);
            response.put("message", "Erreur connexion MinIO: " + e.getMessage());
            response.put("error", e.getMessage());
            response.put("timestamp", java.time.LocalDateTime.now());

            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
        }
    }

    // =============== MÉTHODES UTILITAIRES ===============

    private String extractFileNameFromUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return null;
        }

        try {
            // Format attendu: http://localhost:9000/bucket-name/products/filename.jpg
            String[] parts = imageUrl.split("/");
            if (parts.length >= 2) {
                // Récupérer les 2 dernières parties (dossier/fichier)
                return parts[parts.length - 2] + "/" + parts[parts.length - 1];
            }
            return parts[parts.length - 1];
        } catch (Exception e) {
            logger.warn("⚠️ Impossible d'extraire le nom de fichier de l'URL: {}", imageUrl);
            return null;
        }
    }
}
