package com.boutique_catalogue_produits.repository;

import com.boutique_catalogue_produits.model.Reduction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository
public interface ReductionRepository extends JpaRepository<Reduction, Long> {
    // Trouver les réductions actives
    List<Reduction> findByActifTrue();
    // Trouver toutes les réductions pour un produit spécifique (requête SQL native)
    @Query(value = "SELECT r.* FROM reductions r JOIN produits p ON r.id_reduction = p.id_reduction WHERE p.id_produit = :produitId", nativeQuery = true)
    List<Reduction> findByProduitId(@Param("produitId") String produitId);
    @Query("SELECT r FROM Reduction r WHERE r.actif = true AND r.periode_debut <= CURRENT_DATE AND r.periode_fin >= CURRENT_DATE")
    List<Reduction> findAllActiveReductions();

    // Si vous voulez chercher des réductions par nom
    List<Reduction> findByNomContainingIgnoreCase(String nom);
}