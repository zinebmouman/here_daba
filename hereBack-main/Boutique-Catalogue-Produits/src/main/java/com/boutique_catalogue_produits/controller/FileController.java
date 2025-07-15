package com.boutique_catalogue_produits.controller;

import com.boutique_catalogue_produits.model.Boutique;
import com.boutique_catalogue_produits.service.BoutiqueService;
import com.boutique_catalogue_produits.service.FileStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/fichiers")

public class FileController {
    private static final Logger logger = LoggerFactory.getLogger(FileController.class);

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private BoutiqueService boutiqueService;

    /**
     * Méthode principale pour servir tous les fichiers
     */
    @GetMapping("/{fileName:.+}")
    public ResponseEntity<Resource> serveFile(@PathVariable String fileName) {
        try {
            Path filePath = Paths.get(fileStorageService.getUploadDir()).resolve(fileName).normalize();
            logger.info("Recherche du fichier: {}", filePath.toString());

            if (Files.exists(filePath)) {
                logger.info("Fichier trouvé: {}", filePath);
                Resource resource = new UrlResource(filePath.toUri());
                String contentType = determineContentType(fileName);

                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                // Afficher un message d'erreur au lieu du placeholder pour déboguer
                logger.error("Fichier non trouvé: {}", filePath);
                return ResponseEntity.status(404)
                        .body(null);
            }
        } catch (MalformedURLException e) {
            logger.error("Erreur URL: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    @PostMapping("/repair-image/{boutiqueId}")
    public ResponseEntity<Map<String, Object>> repairImage(
            @PathVariable Integer boutiqueId,
            @RequestBody Map<String, String> requestBody) {
        Map<String, Object> result = new HashMap<>();

        try {
            // Récupérer le nom du fichier depuis le corps de la requête
            String imageFileName = requestBody.get("imageFileName");
            if (imageFileName == null || imageFileName.isEmpty()) {
                result.put("success", false);
                result.put("message", "Nom de fichier manquant");
                return ResponseEntity.badRequest().body(result);
            }

            // Récupérer la boutique
            Boutique boutique = boutiqueService.getBoutiqueById(boutiqueId);
            if (boutique == null) {
                result.put("success", false);
                result.put("message", "Boutique non trouvée");
                return ResponseEntity.notFound().build();
            }

            // Vérifier si le fichier existe
            Path uploadPath = Paths.get(fileStorageService.getUploadDir()).toAbsolutePath().normalize();
            Path imagePath = uploadPath.resolve(imageFileName);
            boolean fileExists = Files.exists(imagePath);

            if (fileExists) {
                result.put("success", true);
                result.put("message", "Le fichier existe déjà");
                return ResponseEntity.ok(result);
            }

            // Copier le placeholder en tant que fichier d'image
            Path placeholderPath = uploadPath.resolve("placeholder.png");
            if (!Files.exists(placeholderPath)) {
                result.put("success", false);
                result.put("message", "Placeholder non trouvé");
                return ResponseEntity.status(500).body(result);
            }

            // Créer une copie du placeholder avec le nom du fichier attendu
            Files.copy(placeholderPath, imagePath);

            // Vérifier si la copie a réussi
            boolean repairSuccessful = Files.exists(imagePath);
            result.put("success", repairSuccessful);
            result.put("message", repairSuccessful ?
                    "Image réparée avec succès" : "Échec de la réparation");

            if (repairSuccessful) {
                // Mettre à jour l'URL de l'image dans la boutique
                String url = fileStorageService.genererUrl(imageFileName);

                Boutique boutiqueModifiee = new Boutique();
                boutiqueModifiee.setId_boutique(boutique.getId_boutique());
                boutiqueModifiee.setVendeurId(boutique.getVendeurId());
                boutiqueModifiee.setNom(boutique.getNom());
                boutiqueModifiee.setAdress(boutique.getAdress());
                boutiqueModifiee.setVille(boutique.getVille());
                boutiqueModifiee.setPays(boutique.getPays());
                boutiqueModifiee.setContact(boutique.getContact());
                boutiqueModifiee.setHoraire(boutique.getHoraire());
                boutiqueModifiee.setLocalisation(boutique.getLocalisation());
                boutiqueModifiee.setCodePostal(boutique.getCodePostal());
                boutiqueModifiee.setNumero_patente(boutique.getNumero_patente());
                boutiqueModifiee.setAutorisation_image(boutique.getAutorisation_image());
                boutiqueModifiee.setCategories(boutique.getCategories());
                boutiqueModifiee.setBoutique_img(imageFileName);
                boutiqueModifiee.setBoutiqueImgUrl(url);

                boutiqueService.mettreAJourBoutique(boutiqueId, boutiqueModifiee, boutique.getVendeurId());

                result.put("imageUrl", url);
            }

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Erreur lors de la réparation de l'image: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "Erreur: " + e.getMessage());
            return ResponseEntity.status(500).body(result);
        }
    }
    // Dans FileController.java
    @GetMapping("/test-upload")
    public ResponseEntity<Map<String, Object>> testUpload() {
        Map<String, Object> result = new HashMap<>();

        try {
            // Tester l'accès au dossier
            Path uploadPath = Paths.get(fileStorageService.getUploadDir()).toAbsolutePath().normalize();
            result.put("uploadDir", uploadPath.toString());
            result.put("exists", Files.exists(uploadPath));
            result.put("isDirectory", Files.isDirectory(uploadPath));
            result.put("isWritable", Files.isWritable(uploadPath));

            // Tester la création d'un fichier de test
            String testContent = "Test file " + System.currentTimeMillis();
            Path testFile = uploadPath.resolve("test_" + System.currentTimeMillis() + ".txt");
            Files.write(testFile, testContent.getBytes());

            result.put("testFileCreated", Files.exists(testFile));
            result.put("testFileContent", new String(Files.readAllBytes(testFile)));

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            result.put("error", e.getMessage());
            return ResponseEntity.status(500).body(result);
        }
    }
    /**
     * Méthode utilitaire pour déterminer le type de contenu
     */
    private String determineContentType(String fileName) {
        if (fileName.toLowerCase().endsWith(".jpg") || fileName.toLowerCase().endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (fileName.toLowerCase().endsWith(".png")) {
            return "image/png";
        } else if (fileName.toLowerCase().endsWith(".gif")) {
            return "image/gif";
        } else if (fileName.toLowerCase().endsWith(".webp")) {
            return "image/webp";
        } else {
            return "application/octet-stream";
        }
    }

    /**
     * Endpoint pour servir des images placeholder avec dimensions
     */
    @GetMapping("/placeholder/{width}/{height}")
    public ResponseEntity<Resource> servePlaceholder(
            @PathVariable int width,
            @PathVariable int height) {
        try {
            Path filePath = Paths.get(fileStorageService.getUploadDir()).resolve("placeholder.png").normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_PNG)
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                        .body(resource);
            } else {
                logger.warn("Placeholder non trouvé");
                return ResponseEntity.notFound().build();
            }
        } catch (MalformedURLException e) {
            logger.error("Erreur lors du traitement du placeholder: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Upload général d'image
     */
    @PostMapping("/upload")
    public ResponseEntity<String> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("Fichier vide");
            }

            String filename = fileStorageService.stockerFichier(file);
            logger.info("Fichier '{}' téléchargé avec succès", filename);

            return ResponseEntity.ok(filename);
        } catch (IOException e) {
            logger.error("Erreur lors du téléchargement du fichier: {}", e.getMessage());
            return ResponseEntity.status(500).body("Erreur lors du téléchargement: " + e.getMessage());
        }
    }
    @GetMapping("/diagnostic/file/{fileName:.+}")
    public ResponseEntity<Map<String, Object>> checkFile(@PathVariable String fileName) {
        Map<String, Object> info = new HashMap<>();
        info.put("fileName", fileName);

        try {
            Path filePath = Paths.get(fileStorageService.getUploadDir()).resolve(fileName).normalize();
            info.put("absolutePath", filePath.toString());
            info.put("exists", Files.exists(filePath));

            if (Files.exists(filePath)) {
                info.put("size", Files.size(filePath));
                info.put("isReadable", Files.isReadable(filePath));
                info.put("lastModified", Files.getLastModifiedTime(filePath).toString());
            }

            return ResponseEntity.ok(info);
        } catch (Exception e) {
            info.put("error", e.getMessage());
            return ResponseEntity.status(500).body(info);
        }
    }
    /**
     * Upload d'image pour une boutique spécifique
     */
    /**
     * Upload d'image pour une boutique spécifique
     */
    @PostMapping("/upload/boutique/{boutiqueId}")
    public ResponseEntity<String> uploadBoutiqueImage(
            @PathVariable Integer boutiqueId,
            @RequestParam("file") MultipartFile file,
            @RequestHeader(value = "X-Vendeur-ID", required = false) String idVendeur) {

        try {
            logger.info("Début de l'upload d'image pour la boutique ID={}", boutiqueId);
            logger.info("Informations du fichier: nom={}, taille={}, type={}",
                    file.getOriginalFilename(), file.getSize(), file.getContentType());

            // Vérifier que le fichier n'est pas vide
            if (file.isEmpty()) {
                logger.error("Fichier vide reçu");
                return ResponseEntity.badRequest().body("Fichier vide");
            }

            // Récupérer la boutique
            Boutique boutique = boutiqueService.getBoutiqueById(boutiqueId);
            if (boutique == null) {
                logger.error("Boutique {} non trouvée", boutiqueId);
                return ResponseEntity.notFound().build();
            }

            logger.info("Boutique trouvée: ID={}, Nom={}, Image actuelle={}",
                    boutique.getId_boutique(), boutique.getNom(), boutique.getBoutique_img());

            // Vérifier que le vendeur est bien le propriétaire (pour sécurité)
            if (idVendeur != null && !idVendeur.isEmpty() && !boutique.getVendeurId().equals(idVendeur)) {
                return ResponseEntity.status(403).body("Non autorisé à modifier cette boutique");
            }

            // Pour le débogage, utiliser une valeur par défaut si nécessaire
            if (idVendeur == null || idVendeur.isEmpty()) {
                idVendeur = boutique.getVendeurId();
                logger.info("Utilisation de l'ID vendeur de la boutique: {}", idVendeur);
            }

            // Stocker le fichier
            String filename = fileStorageService.stockerImageBoutique(file);

            // Vérifier que le fichier a été créé physiquement
            Path uploadPath = Paths.get(fileStorageService.getUploadDir()).toAbsolutePath().normalize();
            Path savedFilePath = uploadPath.resolve(filename);
            boolean fileExists = Files.exists(savedFilePath);
            long fileSize = fileExists ? Files.size(savedFilePath) : 0;

            logger.info("Vérification du fichier sauvegardé: {} - existe: {} - taille: {}",
                    savedFilePath, fileExists, fileSize);

            if (!fileExists || fileSize == 0) {
                logger.error("ERREUR CRITIQUE: Le fichier n'a pas été créé physiquement");
                return ResponseEntity.status(500).body("Erreur lors de la sauvegarde physique du fichier");
            }

            // Générer l'URL complète
            String url = fileStorageService.genererUrl(filename);
            logger.info("Image '{}' sauvegardée pour la boutique {}, URL: {}", filename, boutiqueId, url);

            // Supprimer l'ancienne image si elle existe
            String ancienneImage = boutique.getBoutique_img();
            if (ancienneImage != null && !ancienneImage.isEmpty() && !ancienneImage.equals(filename)) {
                try {
                    fileStorageService.supprimerFichier(ancienneImage);
                    logger.info("Ancienne image supprimée: {}", ancienneImage);
                } catch (IOException e) {
                    logger.warn("Impossible de supprimer l'ancienne image: {}", e.getMessage());
                }
            }

            // IMPORTANT: Créer une nouvelle instance boutique pour la mise à jour
            Boutique boutiqueModifiee = new Boutique();
            boutiqueModifiee.setId_boutique(boutique.getId_boutique());
            boutiqueModifiee.setVendeurId(boutique.getVendeurId());
            boutiqueModifiee.setNom(boutique.getNom());
            boutiqueModifiee.setAdress(boutique.getAdress());
            boutiqueModifiee.setVille(boutique.getVille());
            boutiqueModifiee.setPays(boutique.getPays());
            boutiqueModifiee.setContact(boutique.getContact());
            boutiqueModifiee.setHoraire(boutique.getHoraire());
            boutiqueModifiee.setLocalisation(boutique.getLocalisation());
            boutiqueModifiee.setCodePostal(boutique.getCodePostal());
            boutiqueModifiee.setNumero_patente(boutique.getNumero_patente());
            boutiqueModifiee.setAutorisation_image(boutique.getAutorisation_image());
            boutiqueModifiee.setCategories(boutique.getCategories());

            // Définir la nouvelle image
            boutiqueModifiee.setBoutique_img(filename);
            boutiqueModifiee.setBoutiqueImgUrl(url);

            logger.info("URL d'image définie dans boutiqueModifiee: {}", boutiqueModifiee.getBoutiqueImgUrl());
            logger.info("Nom de fichier défini dans boutiqueModifiee: {}", boutiqueModifiee.getBoutique_img());

            // Mettre à jour la boutique
            logger.info("Mise à jour de la boutique avec la nouvelle image: {}", filename);
            Boutique updated = boutiqueService.mettreAJourBoutique(boutiqueId, boutiqueModifiee, idVendeur);

            logger.info("Boutique mise à jour: ID={}, Image={}", updated.getId_boutique(), updated.getBoutique_img());

            // Vérifier si la mise à jour a bien pris effet
            Boutique verification = boutiqueService.getBoutiqueById(boutiqueId);

            logger.info("Vérification après mise à jour: ID={}, Image={}, URL={}",
                    verification.getId_boutique(),
                    verification.getBoutique_img(),
                    verification.getBoutiqueImgUrl());

            // Retourner le nom du fichier pour référence future
            return ResponseEntity.ok(filename);
        } catch (Exception e) {
            logger.error("Erreur lors du téléchargement du fichier: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("Erreur lors du téléchargement: " + e.getMessage());
        }
    }

    @GetMapping("/diagnostic/images-boutiques-details")
    public ResponseEntity<Map<String, Object>> verifierImagesBoutiques() {
        Map<String, Object> result = new HashMap<>();
        try {
            // Récupérer toutes les boutiques
            List<Boutique> boutiques = boutiqueService.getAllBoutiques();
            List<Map<String, Object>> boutiquesInfo = new ArrayList<>();

            // Parcourir chaque boutique
            for (Boutique b : boutiques) {
                Map<String, Object> info = new HashMap<>();
                info.put("id", b.getId_boutique());
                info.put("nom", b.getNom());
                info.put("image", b.getBoutique_img());
                info.put("imageUrl", b.getBoutiqueImgUrl());

                // Vérifier si le fichier image existe
                if (b.getBoutique_img() != null && !b.getBoutique_img().isEmpty()) {
                    Path filePath = Paths.get(fileStorageService.getUploadDir())
                            .resolve(b.getBoutique_img()).normalize();
                    info.put("fichierExiste", Files.exists(filePath));
                    if (Files.exists(filePath)) {
                        info.put("tailleFichier", Files.size(filePath));
                    }
                } else {
                    info.put("fichierExiste", false);
                }

                boutiquesInfo.add(info);
            }

            result.put("boutiques", boutiquesInfo);

            // Vérifier le dossier d'upload
            Path uploadPath = Paths.get(fileStorageService.getUploadDir()).normalize();
            result.put("cheminUpload", uploadPath.toString());
            result.put("dossierExiste", Files.exists(uploadPath));
            result.put("estDossier", Files.isDirectory(uploadPath));
            result.put("permissions", Map.of(
                    "readable", Files.isReadable(uploadPath),
                    "writable", Files.isWritable(uploadPath),
                    "executable", Files.isExecutable(uploadPath)
            ));

            // Lister tous les fichiers dans le dossier
            if (Files.exists(uploadPath)) {
                List<String> fichiers = Files.list(uploadPath)
                        .map(p -> p.getFileName().toString())
                        .collect(Collectors.toList());
                result.put("fichiersDansDossier", fichiers);
            }

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            result.put("erreur", e.getMessage());
            result.put("stacktrace", Arrays.toString(e.getStackTrace()));
            return ResponseEntity.status(500).body(result);
        }
    }

    @GetMapping("/diagnostic")
    public ResponseEntity<Map<String, Object>> checkFileSystem() {
        Map<String, Object> info = new HashMap<>();

        try {
            // Informations sur le système
            info.put("workingDirectory", System.getProperty("user.dir"));
            info.put("uploadDir", fileStorageService.getUploadDir());

            // Vérification du chemin d'upload
            Path uploadPath = Paths.get(fileStorageService.getUploadDir()).toAbsolutePath().normalize();
            info.put("absolutePath", uploadPath.toString());
            info.put("exists", Files.exists(uploadPath));
            info.put("isDirectory", Files.isDirectory(uploadPath));
            info.put("isWritable", Files.isWritable(uploadPath));

            // Lister les fichiers existants
            if (Files.exists(uploadPath)) {
                List<String> files = Files.list(uploadPath)
                        .map(p -> p.getFileName().toString())
                        .collect(Collectors.toList());
                info.put("files", files);

                // Vérifier les boutiques avec leurs images
                List<Boutique> boutiques = boutiqueService.getAllBoutiques();
                List<Map<String, String>> boutiquesInfo = boutiques.stream()
                        .map(b -> {
                            Map<String, String> bInfo = new HashMap<>();
                            bInfo.put("id", String.valueOf(b.getId_boutique()));
                            bInfo.put("nom", b.getNom());
                            bInfo.put("image", b.getBoutique_img());
                            return bInfo;
                        })
                        .collect(Collectors.toList());
                info.put("boutiques", boutiquesInfo);
            }

            return ResponseEntity.ok(info);
        } catch (Exception e) {
            info.put("error", e.getMessage());
            return ResponseEntity.status(500).body(info);
        }
    }
    /**
     * Redirection des anciens endpoints vers le nouveau (compatibilité)
     */
    @GetMapping("/api/placeholder/{width}/{height}")
    public ResponseEntity<Resource> redirectPlaceholder(
            @PathVariable int width,
            @PathVariable int height) {
        return servePlaceholder(width, height);
    }
}