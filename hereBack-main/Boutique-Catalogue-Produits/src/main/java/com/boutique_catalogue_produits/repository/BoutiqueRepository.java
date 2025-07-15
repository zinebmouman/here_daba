// BoutiqueRepository.java
package com.boutique_catalogue_produits.repository;

import com.boutique_catalogue_produits.model.Boutique;
import feign.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface BoutiqueRepository extends JpaRepository<Boutique, Integer> {
    // Utiliser le nom qui correspond au getter dans votre classe Boutique
    List<Boutique> findByVendeurId(String vendeurId);

    List<Boutique> findByNomContainingIgnoreCase(String nom);

    List<Boutique> findByNomContainingIgnoreCaseOrVilleContainingIgnoreCaseOrAdressContainingIgnoreCase(
            String nom, String ville, String adresse);

    /**
     * 🌍 Recherche par ville uniquement
     */
    List<Boutique> findByVilleContainingIgnoreCase(String ville);

    /**
     * 🏪 Recherche par nom de boutique
     */

    /**
     * 📍 Recherche par adresse
     */
    List<Boutique> findByAdressContainingIgnoreCase(String adresse);

    /**
     * 🔍 Recherche combinée avec query personnalisée
     */

    /**
     * 🌟 Recherche les boutiques actives seulement
     */

    /**
     * 🎯 Recherche par ville et vendeur
     */
    List<Boutique> findByVilleAndVendeurId(String ville, String vendeurId);



    /**
     * 🗺️ Recherche par région/pays
     */
    List<Boutique> findByPaysContainingIgnoreCase(String pays);

    /**
     * 📞 Recherche avec contact disponible
     */
    @Query("SELECT b FROM Boutique b WHERE b.contact IS NOT NULL AND b.contact > 0")
    List<Boutique> findBoutiquesWithContact();

    /**
     * 🕐 Recherche avec horaires disponibles
     */
    @Query("SELECT b FROM Boutique b WHERE b.horaire IS NOT NULL AND b.horaire != ''")
    List<Boutique> findBoutiquesWithHoraire();

    /**
     * 🖼️ Recherche avec images disponibles
     */
    @Query("SELECT b FROM Boutique b WHERE b.boutique_img IS NOT NULL AND b.boutique_img != ''")
    List<Boutique> findBoutiquesWithImages();

    /**
     * 📊 Statistiques par ville
     */
    @Query("SELECT b.ville, COUNT(b) FROM Boutique b WHERE b.ville IS NOT NULL GROUP BY b.ville ORDER BY COUNT(b) DESC")
    List<Object[]> countBoutiquesByVille();

    /**
     * 📊 Statistiques par vendeur
     */
    @Query("SELECT b.vendeurId, COUNT(b) FROM Boutique b WHERE b.vendeurId IS NOT NULL GROUP BY b.vendeurId ORDER BY COUNT(b) DESC")
    List<Object[]> countBoutiquesByVendeur();

    /**
     * 🔍 Recherche fulltext avancée (si supportée par votre DB)
     */
    @Query(value = "SELECT * FROM boutique WHERE " +
            "MATCH(nom, ville, adress, description) AGAINST(?1 IN NATURAL LANGUAGE MODE)",
            nativeQuery = true)
    List<Boutique> fullTextSearch(String searchTerm);

    /**
     * 🎯 Recherche par distance (nécessite coordinates GPS dans le modèle)
     * TODO: À implémenter quand les coordonnées seront ajoutées
     */
    /*
    @Query(value = "SELECT *, " +
                   "(6371 * acos(cos(radians(?1)) * cos(radians(latitude)) * " +
                   "cos(radians(longitude) - radians(?2)) + sin(radians(?1)) * " +
                   "sin(radians(latitude)))) AS distance " +
                   "FROM boutique " +
                   "HAVING distance < ?3 " +
                   "ORDER BY distance",
           nativeQuery = true)
    List<Boutique> findByProximity(double latitude, double longitude, double radiusKm);
    */
}