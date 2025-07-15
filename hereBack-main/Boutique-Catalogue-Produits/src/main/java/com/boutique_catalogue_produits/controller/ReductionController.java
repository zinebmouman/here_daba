package com.boutique_catalogue_produits.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.boutique_catalogue_produits.model.Reduction;
import com.boutique_catalogue_produits.repository.ReductionRepository;
import com.boutique_catalogue_produits.service.ReductionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.bind.annotation.*;

import java.util.*;


@RestController
@RequestMapping("/api/reductions")
public class ReductionController {
    private static final Logger log = LoggerFactory.getLogger(ReductionController.class);

    @Autowired
    private ReductionService reductionService;

    @Autowired
    private ReductionRepository reductionRepository;

    // Obtenir toutes les réductions


    @GetMapping
    public ResponseEntity<?> getAllReductions() {
        try {
            log.info("Tentative de récupération de toutes les réductions");
            List<Reduction> reductions = reductionService.getAllReductions();

            if (reductions == null) {
                log.warn("La liste des réductions est null");
                return ResponseEntity.ok(new ArrayList<Reduction>());
            }

            log.info("Nombre de réductions récupérées : {}", reductions.size());

            // Log détaillé de chaque réduction
            reductions.forEach(reduction -> {
                log.debug("Réduction - ID: {}, Nom: {}, Pourcentage: {}, Actif: {}, Début: {}, Fin: {}",
                        reduction.getId(),
                        reduction.getNom(),
                        reduction.getPourcentage_reduction(),
                        reduction.getActif(),
                        reduction.getPeriode_debut(),
                        reduction.getPeriode_fin()
                );
            });

            return ResponseEntity.ok(reductions);
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des réductions", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "message", "Impossible de récupérer les réductions",
                            "error", e.getMessage()
                    ));
        }
    }
    @GetMapping("/active")
    public ResponseEntity<List<Reduction>> getActiveReductions() {
        return ResponseEntity.ok(reductionRepository.findByActifTrue());
    }
    // Obtenir une réduction par son ID
    @GetMapping("/{id}")
    public ResponseEntity<Reduction> getReductionById(@PathVariable Integer id) {
        return reductionService.getReductionById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Créer une nouvelle réduction
    @PostMapping
    public ResponseEntity<Reduction> createReduction(@RequestBody Reduction reduction) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reductionService.saveReduction(reduction));
    }

    // Mettre à jour une réduction existante
    @PutMapping("/{id}")
    public ResponseEntity<Reduction> updateReduction(@PathVariable Integer id, @RequestBody Reduction reduction) {
        return reductionService.getReductionById(id)
                .map(existingReduction -> {
                    reduction.setId(id.longValue());
                    return ResponseEntity.ok(reductionService.saveReduction(reduction));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // Supprimer une réduction
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Boolean>> deleteReduction(@PathVariable Integer id) {
        return reductionService.getReductionById(id)
                .map(reduction -> {
                    reductionService.deleteReduction(id);
                    Map<String, Boolean> response = new HashMap<>();
                    response.put("deleted", Boolean.TRUE);
                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.notFound().build());
    }
}