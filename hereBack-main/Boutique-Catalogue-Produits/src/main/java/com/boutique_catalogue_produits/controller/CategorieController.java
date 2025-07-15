package com.boutique_catalogue_produits.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.boutique_catalogue_produits.model.Categorie;
import com.boutique_catalogue_produits.model.Produit;
import com.boutique_catalogue_produits.repository.CategorieRepository;
import com.boutique_catalogue_produits.service.CategorieService;
import jakarta.validation.Valid;
import org.jboss.logging.BasicLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
@RestController
@RequestMapping("/api/categories")

public class CategorieController {

    @Autowired
    private CategorieService categorieService;
    @Autowired
    private CategorieRepository categorieRepository;

    private static final Logger log = LoggerFactory.getLogger(CategorieController.class);

    // Obtenir toutes les catégories
    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<?> getAllCategories() {
        try {
            log.info("Début de récupération des catégories");

            List<Categorie> categories = categorieService.getAllCategories();

            log.info("Nombre de catégories récupérées : {}", categories.size());

            if (categories.isEmpty()) {
                log.warn("Aucune catégorie trouvée");
                return ResponseEntity.noContent().build();
            }

            return ResponseEntity.ok(categories);
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des catégories", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "message", "Erreur lors de la récupération des catégories",
                            "error", e.getMessage()
                    ));
        }
    }
    @GetMapping("/debug")
    public ResponseEntity<?> debugCategories() {
        Map<String, Object> debug = new HashMap<>();
        debug.put("total_count", categorieRepository.count());
        debug.put("first_10_categories", categorieRepository.findAllForDebugging());
        return ResponseEntity.ok(debug);
    }

    // Obtenir une catégorie par son ID
    @Transactional(readOnly = true)
    @GetMapping("/{id}")
    public ResponseEntity<Categorie> getCategorieById(@PathVariable(value = "id") String categorieId) {
        return categorieService.getCategorieById(categorieId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Créer une nouvelle catégorie
    @PostMapping
    public ResponseEntity<Categorie> createCategorie(@Valid @RequestBody Categorie categorie) {
        Categorie savedCategorie = categorieService.saveCategorie(categorie);
        return new ResponseEntity<>(savedCategorie, HttpStatus.CREATED);
    }

    // Mettre à jour une catégorie
    @PutMapping("/{id}")
    public ResponseEntity<Categorie> updateCategorie(
            @PathVariable(value = "id") String categorieId,
            @Valid @RequestBody Categorie categorieDetails) {

        Optional<Categorie> optionalCategorie = categorieService.getCategorieById(categorieId);
        if (!optionalCategorie.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        // Conserver l'ID de la catégorie existante
        categorieDetails.setIdCategorie(categorieId);
        Categorie updatedCategorie = categorieService.saveCategorie(categorieDetails);
        return ResponseEntity.ok(updatedCategorie);
    }

    // Supprimer une catégorie
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Boolean>> deleteCategorie(@PathVariable(value = "id") String categorieId) {
        Optional<Categorie> optionalCategorie = categorieService.getCategorieById(categorieId);
        if (!optionalCategorie.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        categorieService.deleteCategorie(categorieId);

        Map<String, Boolean> response = new HashMap<>();
        response.put("deleted", Boolean.TRUE);
        return ResponseEntity.ok(response);
    }

    // Rechercher des catégories par nom
    @GetMapping("/search")
    public ResponseEntity<List<Categorie>> searchCategories(@RequestParam(value = "nom") String nom) {
        List<Categorie> categories = categorieService.searchCategoriesByName(nom);
        return ResponseEntity.ok(categories);
    }

    // Obtenir les produits d'une catégorie
    @Transactional(readOnly = true)
    @GetMapping("/{id}/produits")
    public ResponseEntity<Set<Produit>> getCategorieProduits(@PathVariable(value = "id") String categorieId) {
        try {
            Set<Produit> produits = categorieService.getCategorieProduits(categorieId);
            return ResponseEntity.ok(produits);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Ajouter un produit à une catégorie
    @PostMapping("/{categorieId}/produits/{produitId}")
    public ResponseEntity<Categorie> addProduitToCategorie(
            @PathVariable(value = "categorieId") String categorieId,
            @PathVariable(value = "produitId") Long produitId) {
        try {
            Categorie categorie = categorieService.addProduitToCategorie(categorieId, produitId);
            return ResponseEntity.ok(categorie);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Supprimer un produit d'une catégorie
    @DeleteMapping("/{categorieId}/produits/{produitId}")
    public ResponseEntity<Categorie> removeProduitFromCategorie(
            @PathVariable(value = "categorieId") String categorieId,
            @PathVariable(value = "produitId") Long produitId) {
        try {
            Categorie categorie = categorieService.removeProduitFromCategorie(categorieId, produitId);
            return ResponseEntity.ok(categorie);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}