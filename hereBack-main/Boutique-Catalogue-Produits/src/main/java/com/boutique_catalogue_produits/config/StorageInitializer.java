package com.boutique_catalogue_produits.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class StorageInitializer {

    private static final Logger logger = LoggerFactory.getLogger(StorageInitializer.class);

    @Value("${file.upload-dir}")
    private String uploadDir;

    @PostConstruct
    public void init() {
        try {
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                logger.info("Création du répertoire de stockage: {}", uploadPath);
                Files.createDirectories(uploadPath);
                logger.info("Répertoire de stockage créé avec succès!");
            } else {
                logger.info("Le répertoire de stockage existe déjà: {}", uploadPath);
            }

            // Créer un placeholder pour le développement front-end
            Path placeholderPath = uploadPath.resolve("placeholder.png");
            if (!Files.exists(placeholderPath)) {
                logger.info("Création d'une image placeholder par défaut");
                // Si vous avez une image par défaut dans vos ressources, vous pouvez la copier ici
                // Pour l'exemple, nous allons juste créer un fichier vide
// Créer une vraie image placeholder
                try {
                    BufferedImage img = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
                    Graphics2D g = img.createGraphics();
                    g.setColor(new Color(200, 200, 200));
                    g.fillRect(0, 0, 200, 200);
                    g.dispose();

                    ImageIO.write(img, "png", placeholderPath.toFile());
                    logger.info("Image placeholder créée: {}", placeholderPath);
                } catch (Exception e) {
                    logger.error("Erreur lors de la création du placeholder: {}", e.getMessage());
                    // Créer au moins un fichier vide comme fallback
                    Files.createFile(placeholderPath);
                }
            }
        } catch (IOException e) {
            logger.error("Erreur lors de l'initialisation du stockage", e);
            throw new RuntimeException("Impossible d'initialiser le stockage", e);
        }
    }
}