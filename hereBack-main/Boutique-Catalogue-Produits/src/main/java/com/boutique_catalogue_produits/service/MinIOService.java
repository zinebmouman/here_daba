
// src/main/java/com/boutique_catalogue_produits/service/MinIOService.java
package com.boutique_catalogue_produits.service;

import io.minio.*;
import io.minio.errors.*;
import io.minio.http.Method;
import io.minio.messages.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class MinIOService {

    private static final Logger logger = LoggerFactory.getLogger(MinIOService.class);

    @Autowired
    private MinioClient minioClient;

    @Value("${minio.bucket-name}")
    private String bucketName;

    @Value("${minio.endpoint}")
    private String minioEndpoint;

    @Value("${app.minio-public-url:${minio.endpoint}}")
    private String minioPublicUrl;

    // Types de fichiers autorisés
    private static final String[] ALLOWED_IMAGE_TYPES = {
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"
    };

    // Taille maximale : 10MB
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    @PostConstruct
    public void initializeBucket() {
        try {
            logger.info("🚀 Initialisation du service MinIO...");

            // Vérifier si le bucket existe
            boolean bucketExists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucketName).build()
            );

            if (!bucketExists) {
                logger.info("📦 Création du bucket: {}", bucketName);

                // Créer le bucket
                minioClient.makeBucket(
                        MakeBucketArgs.builder().bucket(bucketName).build()
                );

                // Définir la politique publique pour les images
                String policy = createPublicReadPolicy(bucketName);
                minioClient.setBucketPolicy(
                        SetBucketPolicyArgs.builder()
                                .bucket(bucketName)
                                .config(policy)
                                .build()
                );

                logger.info("✅ Bucket créé avec succès avec politique publique");
            } else {
                logger.info("✅ Bucket {} existe déjà", bucketName);
            }

            logger.info("🔗 MinIO endpoint: {}", minioEndpoint);
            logger.info("🔗 MinIO public URL: {}", minioPublicUrl);

        } catch (Exception e) {
            logger.error("❌ Erreur lors de l'initialisation du bucket MinIO", e);
            throw new RuntimeException("Impossible d'initialiser MinIO", e);
        }
    }

    /**
     * Upload d'un fichier vers MinIO
     */
    public String uploadFile(MultipartFile file) throws Exception {
        logger.info("📤 Début upload fichier: {}", file.getOriginalFilename());

        // Validations
        validateFile(file);

        // Générer un nom de fichier unique
        String fileName = generateUniqueFileName(file.getOriginalFilename());

        // Déterminer le content type
        String contentType = file.getContentType();
        if (contentType == null || contentType.isEmpty()) {
            contentType = "application/octet-stream";
        }

        try (InputStream inputStream = file.getInputStream()) {

            // Upload vers MinIO
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType(contentType)
                            .build()
            );

            logger.info("✅ Fichier uploadé avec succès: {}", fileName);

            // Retourner l'URL publique
            String publicUrl = generatePublicUrl(fileName);
            logger.info("🔗 URL publique générée: {}", publicUrl);

            return publicUrl;

        } catch (Exception e) {
            logger.error("❌ Erreur lors de l'upload du fichier: {}", fileName, e);
            throw new Exception("Erreur lors de l'upload: " + e.getMessage(), e);
        }
    }

    /**
     * Upload de plusieurs fichiers
     */
    public java.util.List<String> uploadMultipleFiles(MultipartFile[] files) throws Exception {
        logger.info("📤 Upload de {} fichiers", files.length);

        java.util.List<String> uploadedUrls = new java.util.ArrayList<>();

        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                try {
                    String url = uploadFile(file);
                    uploadedUrls.add(url);
                } catch (Exception e) {
                    logger.error("❌ Erreur upload fichier {}: {}", file.getOriginalFilename(), e.getMessage());
                    // Continuer avec les autres fichiers
                }
            }
        }

        logger.info("✅ {} fichiers uploadés avec succès", uploadedUrls.size());
        return uploadedUrls;
    }

    /**
     * Supprimer un fichier de MinIO
     */
    public void deleteFile(String fileName) throws Exception {
        try {
            logger.info("🗑️ Suppression du fichier: {}", fileName);

            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .build()
            );

            logger.info("✅ Fichier supprimé avec succès: {}", fileName);

        } catch (Exception e) {
            logger.error("❌ Erreur lors de la suppression du fichier: {}", fileName, e);
            throw new Exception("Erreur lors de la suppression: " + e.getMessage(), e);
        }
    }

    /**
     * Supprimer un fichier à partir de son URL
     */
    public void deleteFileFromUrl(String fileUrl) throws Exception {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return;
        }

        // Extraire le nom du fichier de l'URL
        String fileName = extractFileNameFromUrl(fileUrl);
        if (fileName != null) {
            deleteFile(fileName);
        }
    }

    /**
     * Générer une URL présignée pour un accès temporaire
     */
    public String generatePresignedUrl(String fileName, int expiryInHours) throws Exception {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucketName)
                            .object(fileName)
                            .expiry(expiryInHours, TimeUnit.HOURS)
                            .build()
            );
        } catch (Exception e) {
            logger.error("❌ Erreur génération URL présignée pour: {}", fileName, e);
            throw new Exception("Erreur génération URL présignée: " + e.getMessage(), e);
        }
    }

    /**
     * Vérifier si un fichier existe dans MinIO
     */
    public boolean fileExists(String fileName) {
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .build()
            );
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Obtenir les informations d'un fichier
     */
    public StatObjectResponse getFileInfo(String fileName) throws Exception {
        try {
            return minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .build()
            );
        } catch (Exception e) {
            logger.error("❌ Erreur récupération info fichier: {}", fileName, e);
            throw new Exception("Fichier non trouvé: " + fileName, e);
        }
    }

    // =============== MÉTHODES PRIVÉES ===============

    private void validateFile(MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new Exception("Le fichier est vide ou null");
        }

        // Vérifier la taille
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new Exception("Fichier trop volumineux. Taille maximum: " + (MAX_FILE_SIZE / 1024 / 1024) + "MB");
        }

        // Vérifier le type
        String contentType = file.getContentType();
        if (contentType == null || !isAllowedImageType(contentType)) {
            throw new Exception("Type de fichier non supporté: " + contentType +
                    ". Types autorisés: " + String.join(", ", ALLOWED_IMAGE_TYPES));
        }

        logger.info("✅ Validation fichier réussie: {} ({})", file.getOriginalFilename(), contentType);
    }

    private boolean isAllowedImageType(String contentType) {
        for (String allowedType : ALLOWED_IMAGE_TYPES) {
            if (allowedType.equalsIgnoreCase(contentType)) {
                return true;
            }
        }
        return false;
    }

    private String generateUniqueFileName(String originalFileName) {
        // Extraire l'extension
        String extension = "";
        if (originalFileName != null && originalFileName.contains(".")) {
            extension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }

        // Générer un nom unique avec timestamp et UUID
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String uuid = UUID.randomUUID().toString().substring(0, 8);

        return String.format("products/%s_%s%s", timestamp, uuid, extension);
    }

    private String generatePublicUrl(String fileName) {
        // Utiliser l'URL publique configurée
        String baseUrl = minioPublicUrl.endsWith("/") ? minioPublicUrl.substring(0, minioPublicUrl.length() - 1) : minioPublicUrl;
        return String.format("%s/%s/%s", baseUrl, bucketName, fileName);
    }

    private String extractFileNameFromUrl(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return null;
        }

        try {
            // Extraire le nom du fichier de l'URL
            // Format attendu: http://localhost:9000/bucket-name/products/filename.jpg
            String[] parts = fileUrl.split("/");
            if (parts.length >= 2) {
                // Récupérer les 2 dernières parties (dossier/fichier)
                return parts[parts.length - 2] + "/" + parts[parts.length - 1];
            }
            return parts[parts.length - 1];
        } catch (Exception e) {
            logger.warn("⚠️ Impossible d'extraire le nom de fichier de l'URL: {}", fileUrl);
            return null;
        }
    }

    private String createPublicReadPolicy(String bucketName) {
        return String.format("""
            {
                "Version": "2012-10-17",
                "Statement": [
                    {
                        "Effect": "Allow",
                        "Principal": {
                            "AWS": "*"
                        },
                        "Action": [
                            "s3:GetObject"
                        ],
                        "Resource": [
                            "arn:aws:s3:::%s/*"
                        ]
                    }
                ]
            }
            """, bucketName);
    }

    // =============== MÉTHODES UTILITAIRES PUBLIQUES ===============

    /**
     * Obtenir l'URL publique d'un fichier existant
     */
    public String getPublicUrl(String fileName) {
        return generatePublicUrl(fileName);
    }

    /**
     * Obtenir des statistiques du bucket
     */
    public java.util.Map<String, Object> getBucketStats() {
        java.util.Map<String, Object> stats = new java.util.HashMap<>();

        try {
            // Compter les objets (approximatif)
            Iterable<Result<Item>> objects = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(bucketName)
                            .recursive(true)
                            .build()
            );

            long count = 0;
            long totalSize = 0;

            for (Result<Item> result : objects) {
                Item item = result.get();
                count++;
                totalSize += item.size();
            }

            stats.put("bucketName", bucketName);
            stats.put("objectCount", count);
            stats.put("totalSize", totalSize);
            stats.put("totalSizeMB", totalSize / 1024.0 / 1024.0);
            stats.put("endpoint", minioEndpoint);
            stats.put("publicUrl", minioPublicUrl);

        } catch (Exception e) {
            logger.error("❌ Erreur récupération stats bucket", e);
            stats.put("error", e.getMessage());
        }

        return stats;
    }
}