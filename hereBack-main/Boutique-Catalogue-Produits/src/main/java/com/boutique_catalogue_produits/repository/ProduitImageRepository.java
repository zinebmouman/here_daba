package com.boutique_catalogue_produits.repository;

import com.boutique_catalogue_produits.model.ProduitImage;
import feign.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

// Dans ProduitImageRepository
@Repository
public interface ProduitImageRepository extends JpaRepository<ProduitImage, Long> {
    // Méthode corrigée pour correspondre à l'appel dans le service
    @Query("SELECT pi FROM ProduitImage pi WHERE pi.produit.id = :produitId AND pi.imagePrincipale = :estPrincipale")
    Optional<ProduitImage> findByProduitIdAndImagePrincipale(
            @Param("produitId") Long produitId,
            @Param("estPrincipale") boolean estPrincipale
    );

    // Les autres méthodes restent les mêmes
    List<ProduitImage> findByProduitId(Long produitId);
}