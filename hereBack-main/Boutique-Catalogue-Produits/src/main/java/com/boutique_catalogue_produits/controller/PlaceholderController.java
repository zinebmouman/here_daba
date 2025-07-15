package com.boutique_catalogue_produits.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@RestController
@RequestMapping("/api/placeholder")

public class PlaceholderController {

    @GetMapping("/{width}/{height}")
    public ResponseEntity<byte[]> getPlaceholder(
            @PathVariable int width,
            @PathVariable int height) {

        try {
            // Créer une image placeholder dynamique
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();

            // Fond gris clair
            g.setColor(new Color(240, 240, 240));
            g.fillRect(0, 0, width, height);

            // Bordure grise
            g.setColor(new Color(200, 200, 200));
            g.drawRect(0, 0, width - 1, height - 1);

            // Texte pour les dimensions
            g.setColor(new Color(150, 150, 150));
            g.setFont(new Font("Arial", Font.BOLD, 12));
            String dimensionText = width + "x" + height;
            FontMetrics fm = g.getFontMetrics();
            int textX = (width - fm.stringWidth(dimensionText)) / 2;
            int textY = (height / 2) + (fm.getAscent() / 2);
            g.drawString(dimensionText, textX, textY);

            g.dispose();

            // Convertir l'image en tableau d'octets
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            byte[] imageData = baos.toByteArray();

            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                    .body(imageData);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}