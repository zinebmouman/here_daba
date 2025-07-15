package com.boutique_catalogue_produits.service;

import com.boutique_catalogue_produits.model.Produit;
import com.boutique_catalogue_produits.repository.ProduitImageRepository;
import com.boutique_catalogue_produits.dto.ProduitImageDTO;
import com.boutique_catalogue_produits.model.ProduitImage;
import com.boutique_catalogue_produits.repository.ProduitRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ProduitImageService {

    private static final Logger log = LoggerFactory.getLogger(ProduitImageService.class);

    @Autowired
    private ProduitImageRepository imageRepository;

    @Autowired
    private ProduitRepository produitRepository;

    @Autowired
    private FileStorageService fileStorageService;

    public List<ProduitImageDTO> getImagesByProduitId(Long produitId) {
        log.info("Récupération des images pour le produit ID: {}", produitId);
        return imageRepository.findByProduitId(produitId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public ProduitImageDTO ajouterImageAuProduit(Long produitId, String cheminFichier, String url, Boolean imagePrincipale) {
        log.info("Ajout d'une image au produit ID: {} (principale: {})", produitId, imagePrincipale);
        Produit produit = produitRepository.findById(produitId)
                .orElseThrow(() -> new RuntimeException("Produit non trouvé avec l'ID: " + produitId));

        // Si cette image est marquée comme principale, désactiver les autres
        if (Boolean.TRUE.equals(imagePrincipale)) {
            imageRepository.findByProduitIdAndImagePrincipale(produitId, true)
                    .ifPresent(ancienneImagePrincipale -> {
                        log.info("Désactivation de l'ancienne image principale ID: {} pour le produit ID: {}",
                                ancienneImagePrincipale.getId(), produitId);
                        ancienneImagePrincipale.setImagePrincipale(false);
                        imageRepository.save(ancienneImagePrincipale);
                    });
        }

        ProduitImage nouvelleImage = new ProduitImage();
        nouvelleImage.setCheminFichier(cheminFichier);
        nouvelleImage.setUrl(url);
        nouvelleImage.setImagePrincipale(imagePrincipale);
        nouvelleImage.setProduit(produit);
        nouvelleImage.setDateCreation(LocalDateTime.now());

        ProduitImage imageSauvegardee = imageRepository.save(nouvelleImage);
        log.info("Image ID: {} ajoutée avec succès au produit ID: {}", imageSauvegardee.getId(), produitId);
        return convertToDTO(imageSauvegardee);
    }

    @Transactional
    public Optional<ProduitImageDTO> updateImage(Long imageId, ProduitImageDTO imageDTO) {
        log.info("Mise à jour de l'image ID: {}", imageId);
        return imageRepository.findById(imageId)
                .map(image -> {
                    // Si l'image devient principale, désactiver les autres
                    if (Boolean.TRUE.equals(imageDTO.getImagePrincipale()) && !Boolean.TRUE.equals(image.getImagePrincipale())) {
                        imageRepository.findByProduitIdAndImagePrincipale(image.getProduit().getId(), true)
                                .ifPresent(ancienneImagePrincipale -> {
                                    log.info("Désactivation de l'ancienne image principale ID: {} pour le produit ID: {}",
                                            ancienneImagePrincipale.getId(), image.getProduit().getId());
                                    ancienneImagePrincipale.setImagePrincipale(false);
                                    imageRepository.save(ancienneImagePrincipale);
                                });
                    }

                    // Mise à jour des propriétés
                    image.setCheminFichier(imageDTO.getCheminFichier());
                    image.setUrl(imageDTO.getUrl());
                    image.setImagePrincipale(imageDTO.getImagePrincipale());

                    ProduitImage updatedImage = imageRepository.save(image);
                    log.info("Image ID: {} mise à jour avec succès", imageId);
                    return convertToDTO(updatedImage);
                });
    }

    @Transactional
    public boolean deleteImage(Long imageId) {
        log.info("Tentative de suppression de l'image ID: {}", imageId);
        try {
            supprimerImage(imageId);
            return true;
        } catch (Exception e) {
            log.error("Erreur lors de la suppression de l'image ID: {}", imageId, e);
            return false;
        }
    }

    @Transactional
    public void supprimerImage(Long imageId) throws IOException {
        log.info("Suppression de l'image ID: {}", imageId);
        ProduitImage image = imageRepository.findById(imageId)
                .orElseThrow(() -> new RuntimeException("Image non trouvée avec l'ID: " + imageId));

        Long produitId = image.getProduit().getId();
        boolean etaitPrincipale = Boolean.TRUE.equals(image.getImagePrincipale());

        // Supprimer le fichier physique
        fileStorageService.supprimerFichier(image.getCheminFichier());

        // Supprimer l'entrée dans la base de données
        imageRepository.delete(image);
        log.info("Image ID: {} supprimée avec succès", imageId);

        // Si c'était l'image principale et qu'il reste d'autres images, rendre une autre principale
        if (etaitPrincipale) {
            List<ProduitImage> imagesRestantes = imageRepository.findByProduitId(produitId);
            if (!imagesRestantes.isEmpty()) {
                ProduitImage nouvelleImagePrincipale = imagesRestantes.get(0);
                nouvelleImagePrincipale.setImagePrincipale(true);
                imageRepository.save(nouvelleImagePrincipale);
                log.info("Nouvelle image principale ID: {} définie pour le produit ID: {}",
                        nouvelleImagePrincipale.getId(), produitId);
            }
        }
    }

    public ProduitImageDTO getImageById(Long imageId) {
        log.info("Récupération de l'image ID: {}", imageId);
        return imageRepository.findById(imageId)
                .map(this::convertToDTO)
                .orElse(null);
    }

    private ProduitImageDTO convertToDTO(ProduitImage produitImage) {
        ProduitImageDTO dto = new ProduitImageDTO();
        dto.setId(produitImage.getId());
        dto.setCheminFichier(produitImage.getCheminFichier());
        dto.setUrl(produitImage.getUrl());
        dto.setImagePrincipale(produitImage.getImagePrincipale());
        dto.setDateCreation(produitImage.getDateCreation());
        if (produitImage.getProduit() != null) {
            dto.setProduitId(produitImage.getProduit().getId());
        }
        return dto;
    }
}