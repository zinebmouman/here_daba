package com.boutique_catalogue_produits.repository;

import com.boutique_catalogue_produits.model.Categorie;
import feign.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CategorieRepository extends JpaRepository<Categorie, String> {
    List<Categorie> findByNomContainingIgnoreCase(String nom);
    @Query("SELECT COUNT(c) FROM Categorie c")
    long countCategories();
    // Méthode pour vérifier si des données existent
    boolean existsByNomIsNotNull();


    @Query("SELECT c FROM Categorie c")
    List<Categorie> findAllForDebugging();
    @Query("SELECT c FROM Categorie c JOIN c.boutiques b WHERE b.id_boutique = :idBoutique")
    List<Categorie> findByBoutiqueId(@Param("idBoutique") Integer idBoutique);

}
