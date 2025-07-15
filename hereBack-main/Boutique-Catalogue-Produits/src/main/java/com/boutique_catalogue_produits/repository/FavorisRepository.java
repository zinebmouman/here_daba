package com.boutique_catalogue_produits.repository;

import com.boutique_catalogue_produits.model.FavorisProduit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavorisRepository extends JpaRepository<FavorisProduit, Long> {

    List<FavorisProduit> findByIdClient(String idClient);

    List<FavorisProduit> findByIdProduit(Long idProduit);

    Optional<FavorisProduit> findByIdClientAndIdProduit(String idClient, Long idProduit);

    void deleteByIdClientAndIdProduit(String idClient, Long idProduit);

    long countByIdProduit(Long idProduit);

    @Query(value = "SELECT id_produit, COUNT(*) as count FROM favoris_produit GROUP BY id_produit ORDER BY count DESC LIMIT :limit", nativeQuery = true)
    List<Object[]> findTopFavoritedProductIds(@Param("limit") int limit);
}