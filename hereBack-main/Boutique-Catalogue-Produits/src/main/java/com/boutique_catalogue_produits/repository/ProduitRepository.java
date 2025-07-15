package com.boutique_catalogue_produits.repository;

import com.boutique_catalogue_produits.model.Produit;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ProduitRepository extends JpaRepository<Produit, Long> {
    @Query("SELECT DISTINCT p FROM Produit p LEFT JOIN FETCH p.images WHERE p.quantite <= p.seuilCritique")
    List<Produit> findBelowThreshold();

    /**
     * Trouve les IDs des stocks contenant un produit spécifique
     */
    @Query("SELECT p.idStock FROM Produit p WHERE p.id = :produitId")
    List<Long> findStockIdsForProduit(@Param("produitId") Long produitId);

    @Query("SELECT DISTINCT p FROM Produit p LEFT JOIN FETCH p.images")
    List<Produit> findAllWithImages();

    @Query("SELECT DISTINCT p FROM Produit p LEFT JOIN FETCH p.images WHERE p.idStock = :idStock")
    List<Produit> findByIdStock(@Param("idStock") Long idStock);

    @Query("SELECT DISTINCT p FROM Produit p LEFT JOIN FETCH p.images " +
            "WHERE LOWER(p.nomProduit) LIKE LOWER(CONCAT('%', :q, '%')) " +
            "OR LOWER(p.description) LIKE LOWER(CONCAT('%', :q, '%')) " +
            "ORDER BY " +
            "CASE " +
            "   WHEN LOWER(p.nomProduit) = LOWER(:q) THEN 1 " +
            "   WHEN LOWER(p.nomProduit) LIKE LOWER(CONCAT(:q, '%')) THEN 2 " +
            "   WHEN LOWER(p.nomProduit) LIKE LOWER(CONCAT('%', :q, '%')) THEN 3 " +
            "   ELSE 4 " +
            "END")
    List<Produit> searchFuzzy(@Param("q") String q);

    // Méthode pour trouver des produits par idCategorie avec chargement des images
    @Query("SELECT DISTINCT p FROM Produit p LEFT JOIN FETCH p.images WHERE p.idCategorie = :idCategorie")
    List<Produit> findByIdCategorie(@Param("idCategorie") String idCategorie);

    // Méthodes supplémentaires si nécessaire
    List<Produit> findByQuantiteLessThanEqual(Integer seuilCritique);

    @Query("SELECT DISTINCT p FROM Produit p LEFT JOIN FETCH p.images WHERE p.quantite <= p.seuilCritique")
    List<Produit> findProduitsEnAlerte();

    // Méthode pour trouver les produits proches de l'expiration (1 semaine)
    @Query("SELECT DISTINCT p FROM Produit p LEFT JOIN FETCH p.images " +
            "WHERE p.dateExpiration BETWEEN :now AND :oneWeekLater")
    List<Produit> findProduitsProcheExpiration(
            @Param("now") LocalDate now,
            @Param("oneWeekLater") LocalDate oneWeekLater
    );

    // méthode native pour plus de détails
    @Query(value = "SELECT p.* FROM produits p WHERE p.quantite <= p.seuil_critique", nativeQuery = true)
    List<Produit> findProduitsEnAlerteNative();


    // AJOUTE cette méthode dans ton ProduitRepository
    @Query("SELECT DISTINCT p FROM Produit p LEFT JOIN FETCH p.images " +
            "WHERE LOWER(p.nomProduit) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
            "OR LOWER(p.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<Produit> searchByNom(@Param("searchTerm") String searchTerm);


    @Query("SELECT DISTINCT p FROM Produit p LEFT JOIN FETCH p.images " +
            "WHERE (:keywords IS NULL OR " +
            "       (LOWER(p.nomProduit) LIKE LOWER(CONCAT('%', :keywords, '%')) OR " +
            "        LOWER(p.description) LIKE LOWER(CONCAT('%', :keywords, '%')))) " +
            "AND (:prixMin IS NULL OR p.prix >= :prixMin) " +
            "AND (:prixMax IS NULL OR p.prix <= :prixMax) " +
            "AND (:categorie IS NULL OR LOWER(p.idCategorie) LIKE LOWER(CONCAT('%', :categorie, '%'))) " +
            "AND (:quantiteMin IS NULL OR p.quantite >= :quantiteMin) " +
            "AND (:marque IS NULL OR LOWER(p.nomProduit) LIKE LOWER(CONCAT('%', :marque, '%')))")
    List<Produit> searchWithAdvancedCriteria(
            @Param("keywords") String keywords,
            @Param("prixMin") Double prixMin,
            @Param("prixMax") Double prixMax,
            @Param("categorie") String categorie,
            @Param("quantiteMin") Integer quantiteMin,
            @Param("marque") String marque
    );

    /**
     * NOUVELLE MÉTHODE - Recherche par mots-clés multiples
     */
    @Query("SELECT DISTINCT p FROM Produit p LEFT JOIN FETCH p.images " +
            "WHERE LOWER(p.nomProduit) LIKE LOWER(CONCAT('%', :keyword1, '%')) " +
            "OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword1, '%')) " +
            "OR (:keyword2 IS NOT NULL AND (LOWER(p.nomProduit) LIKE LOWER(CONCAT('%', :keyword2, '%')) " +
            "    OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword2, '%')))) " +
            "OR (:keyword3 IS NOT NULL AND (LOWER(p.nomProduit) LIKE LOWER(CONCAT('%', :keyword3, '%')) " +
            "    OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword3, '%'))))")
    List<Produit> searchByMultipleKeywords(
            @Param("keyword1") String keyword1,
            @Param("keyword2") String keyword2,
            @Param("keyword3") String keyword3
    );

    List<Produit> findByIdCategorieIn(List<String> idCategories);
}
