package com.boutique_catalogue_produits.service;

import com.boutique_catalogue_produits.model.Categorie;
import com.boutique_catalogue_produits.model.Produit;
import com.boutique_catalogue_produits.repository.CategorieRepository;
import com.boutique_catalogue_produits.repository.ProduitRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class CategorieService {

    private static final Logger logger = LoggerFactory.getLogger(CategorieService.class);
    private static final Logger log = LoggerFactory.getLogger(CategorieService.class);


    @Autowired
    private CategorieRepository categorieRepository;

    @Autowired
    private ProduitRepository produitRepository;

    @Transactional(readOnly = true)
    public List<Categorie> getAllCategories() {
        try {
            // Vérification du nombre total de catégories
            long totalCount = categorieRepository.count();
            log.info("Nombre total de catégories en base de données : {}", totalCount);

            List<Categorie> categories = categorieRepository.findAll();

            // Log détaillé des catégories
            categories.forEach(categorie ->
                    log.debug("Catégorie - ID: {}, Nom: {}",
                            categorie.getIdCategorie(),
                            categorie.getNom())
            );

            return categories;
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des catégories", e);
            throw new RuntimeException("Impossible de récupérer les catégories", e);
        }
    }
    @Transactional(readOnly = true)
    public Optional<Categorie> getCategorieById(String id) {
        logger.info("Recherche de la catégorie avec l'ID: {}", id);
        return categorieRepository.findById(id);
    }

    public boolean categorieExists(String id) {
        logger.info("Vérification de l'existence de la catégorie avec l'ID: {}", id);
        return categorieRepository.existsById(id);
    }

    @Transactional
    public Categorie saveCategorie(Categorie categorie) {
        // Générer un ID si nouveau
        if (categorie.getIdCategorie() == null || categorie.getIdCategorie().isEmpty()) {
            categorie.setIdCategorie(UUID.randomUUID().toString());
            logger.info("Nouvel ID généré pour la catégorie: {}", categorie.getIdCategorie());
        }

        logger.info("Sauvegarde de la catégorie avec ID: {}", categorie.getIdCategorie());
        return categorieRepository.save(categorie);
    }

    @Transactional
    public void deleteCategorie(String id) {
        logger.info("Suppression de la catégorie avec l'ID: {}", id);
        categorieRepository.deleteById(id);
    }

    public List<Categorie> searchCategoriesByName(String nom) {
        return categorieRepository.findByNomContainingIgnoreCase(nom);
    }

    @Transactional
    public Categorie addProduitToCategorie(String idCategorie, Long idProduit) {
        Categorie categorie = categorieRepository.findById(idCategorie)
                .orElseThrow(() -> new RuntimeException("Catégorie non trouvée avec l'ID: " + idCategorie));

        Produit produit = produitRepository.findById(idProduit)
                .orElseThrow(() -> new RuntimeException("Produit non trouvé avec l'ID: " + idProduit));

        categorie.addProduit(produit);
        return categorieRepository.save(categorie);
    }

    @Transactional
    public Categorie removeProduitFromCategorie(String idCategorie, Long idProduit) {
        Categorie categorie = categorieRepository.findById(idCategorie)
                .orElseThrow(() -> new RuntimeException("Catégorie non trouvée avec l'ID: " + idCategorie));

        Produit produit = produitRepository.findById(idProduit)
                .orElseThrow(() -> new RuntimeException("Produit non trouvé avec l'ID: " + idProduit));

        categorie.removeProduit(produit);
        return categorieRepository.save(categorie);
    }
    @Transactional(readOnly = true)
    public Set<Produit> getCategorieProduits(String idCategorie) {
        Categorie categorie = categorieRepository.findById(idCategorie)
                .orElseThrow(() -> new RuntimeException("Catégorie non trouvée avec l'ID: " + idCategorie));

        return categorie.getProduits();
    }
}