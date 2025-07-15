package com.boutique_catalogue_produits.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {
    private static final Logger logger = LoggerFactory.getLogger(FileStorageService.class);

    // Valeurs codées en dur comme fallback

    private final String baseUrl = "http://localhost:8080";
    @Value("${file.upload-dir:./uploads/images}")
    private String uploadDir;
    public FileStorageService() {
        initializeStorage();
    }
    /**
     * Stocke un fichier avec un nom généré par UUID (pour les produits)
     */
    // Ajouter cette méthode à FileStorageService.java
    public String stockerFichier(MultipartFile fichier) throws IOException {
        if (fichier == null) {
            throw new IllegalArgumentException("Le fichier ne peut pas être null");
        }

        return stockerFichierInterne(fichier, null);
    }
    private void initializeStorage() {
        try {
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            logger.info("Initialisation du stockage de fichiers dans: {}", uploadPath);

            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                logger.info("Dossier d'upload créé avec succès: {}", uploadPath);

                // Créer un placeholder
                createPlaceholder(uploadPath);
            } else {
                logger.info("Dossier d'upload existant: {}", uploadPath);

                // Vérifier si le placeholder existe
                Path placeholderPath = uploadPath.resolve("placeholder.png");
                if (!Files.exists(placeholderPath)) {
                    createPlaceholder(uploadPath);
                }
            }
            logger.info("Permissions du dossier d'upload: readable={}, writable={}, executable={}",
                    Files.isReadable(uploadPath), Files.isWritable(uploadPath), Files.isExecutable(uploadPath));
        } catch (Exception e) {
            logger.error("Erreur lors de l'initialisation du stockage: {}", e.getMessage(), e);
        }
    }

    private void createPlaceholder(Path uploadPath) {
        try {
            Path placeholderPath = uploadPath.resolve("placeholder.png");

            // Créer un fichier placeholder simple
            BufferedImage img = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            g.setColor(new Color(200, 200, 200));
            g.fillRect(0, 0, 200, 200);
            g.dispose();

            ImageIO.write(img, "png", placeholderPath.toFile());
            logger.info("Image placeholder créée: {}", placeholderPath);
        } catch (Exception e) {
            logger.error("Erreur lors de la création du placeholder: {}", e.getMessage());
        }
    }

    /**
     * Stocke un fichier avec un nom généré par UUID (pour les produits)
     */
    private String stockerFichierInterne(MultipartFile fichier, String prefixe) throws IOException {
        // Utiliser chemin absolu
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();

        // Log pour debug
        logger.debug("Chemin d'upload absolu: {}", uploadPath);

        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
            logger.info("Dossier d'upload créé: {}", uploadPath);
        }

        // Extraire l'extension
        String nomFichierOriginal = fichier.getOriginalFilename();
        String extension = "";
        if (nomFichierOriginal != null && nomFichierOriginal.contains(".")) {
            extension = nomFichierOriginal.substring(nomFichierOriginal.lastIndexOf("."));
        }

        // Générer le nom du fichier
        String nomFichier;
        if (prefixe != null && prefixe.equals("boutique")) {
            nomFichier = "boutique_" + System.currentTimeMillis() + extension;
        } else {
            nomFichier = UUID.randomUUID().toString() + extension;
        }

        // Enregistrer le fichier sur le disque
        Path destination = uploadPath.resolve(nomFichier);
        logger.info("Stockage de fichier: {} -> {}", nomFichierOriginal, destination);

        // Vérifier que le dossier parent existe
        if (!Files.exists(destination.getParent())) {
            Files.createDirectories(destination.getParent());
        }

        // Copier le fichier
        Files.copy(fichier.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);

        // Vérifier que le fichier a bien été créé
        boolean fileExists = Files.exists(destination);
        logger.info("Fichier créé avec succès: {} - existe: {} - taille: {}",
                nomFichier, fileExists, fileExists ? Files.size(destination) : 0);

        if (!fileExists) {
            throw new IOException("Échec de la création du fichier: " + destination);
        }

        return nomFichier;
    }

    /**
     * Stocke une image de boutique avec un préfixe "boutique_" et un timestamp
     */
    public String stockerImageBoutique(MultipartFile fichier) throws IOException {
        return stockerFichierInterne(fichier, "boutique");
    }

    /**
     * Méthode interne pour stocker un fichier avec un préfixe optionnel
     */


    public String genererUrl(String nomFichier) {
        return baseUrl + "/api/fichiers/" + nomFichier;
    }

    public void supprimerFichier(String nomFichier) throws IOException {
        if (nomFichier == null || nomFichier.trim().isEmpty()) {
            logger.warn("Tentative de suppression d'un fichier avec un nom vide");
            return;
        }

        Path cheminFichier = Paths.get(uploadDir).toAbsolutePath().normalize().resolve(nomFichier);
        if (Files.exists(cheminFichier)) {
            Files.delete(cheminFichier);
            logger.info("Fichier supprimé: {}", cheminFichier);
        } else {
            logger.warn("Tentative de suppression d'un fichier inexistant: {}", cheminFichier);
        }
    }

    // Getter pour être utilisé dans les contrôleurs
    public String getUploadDir() {
        return uploadDir;
    }
}