package com.boutique_catalogue_produits.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import com.boutique_catalogue_produits.model.Reduction;
import com.boutique_catalogue_produits.repository.ReductionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReductionService {

    @Autowired
    private ReductionRepository reductionRepository;

    // Récupérer toutes les réductions
    public List<Reduction> getAllReductions() {
        return reductionRepository.findAll();
    }

    // Récupérer une réduction par son ID
    public Optional<Reduction> getReductionById(Integer id) {
        return reductionRepository.findById(id.longValue());
    }

    // Créer ou mettre à jour une réduction
    @Transactional
    public Reduction saveReduction(Reduction reduction) {
        return reductionRepository.save(reduction);
    }

    // Supprimer une réduction
    @Transactional
    public void deleteReduction(Integer id) {
        reductionRepository.deleteById(id.longValue());
    }

    // Obtenir les réductions pour un produit
    public List<Reduction> getReductionsByProduit(String idProduit) {
        return reductionRepository.findByProduitId(idProduit);
    }
}