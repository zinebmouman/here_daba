package com.boutique_catalogue_produits.service;

import com.boutique_catalogue_produits.model.FavorisProduit;
import com.boutique_catalogue_produits.repository.FavorisRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class FavorisService {

    @Autowired
    private FavorisRepository favorisRepository;

    /**
     * Ajouter un produit aux favoris d'un utilisateur
     */
    @Transactional
    public Map<String, Object> ajouterFavori(String idUser, Long idProduit) {
        // Vérifier si le favori existe déjà
        Optional<FavorisProduit> existingFavori = favorisRepository.findByIdClientAndIdProduit(idUser, idProduit);

        FavorisProduit favori;
        if (existingFavori.isPresent()) {
            System.out.println("Le produit est déjà dans les favoris");
            favori = existingFavori.get();
        } else {
            // Créer un nouveau favori
            favori = new FavorisProduit(idUser, idProduit);
            favori = favorisRepository.save(favori);
            System.out.println("Nouveau favori ajouté: " + favori.getId());
        }

        // Créer la réponse
        Map<String, Object> response = new HashMap<>();
        response.put("id", favori.getId());
        response.put("idClient", favori.getIdClient());
        response.put("idProduit", favori.getIdProduit());
        response.put("dateAjout", favori.getDateAjout());
        response.put("success", true);

        return response;
    }

    /**
     * Supprimer un produit des favoris d'un utilisateur
     */
    @Transactional
    public void supprimerFavori(String idUser, Long idProduit) {
        Optional<FavorisProduit> favori = favorisRepository.findByIdClientAndIdProduit(idUser, idProduit);

        if (favori.isPresent()) {
            favorisRepository.delete(favori.get());
            System.out.println("Favori supprimé avec succès");
        } else {
            System.out.println("Favori non trouvé pour l'utilisateur " + idUser + " et le produit " + idProduit);
            throw new RuntimeException("Favori non trouvé");
        }
    }

    /**
     * Vérifier si un produit est dans les favoris d'un utilisateur
     */
    public boolean estFavori(String idUser, Long idProduit) {
        return favorisRepository.findByIdClientAndIdProduit(idUser, idProduit).isPresent();
    }

    /**
     * Récupérer tous les favoris d'un utilisateur
     */
    public List<Map<String, Object>> getFavorisByUser(String idUser) {
        List<FavorisProduit> favoris = favorisRepository.findByIdClient(idUser);
        List<Map<String, Object>> result = new ArrayList<>();

        for (FavorisProduit favori : favoris) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", favori.getId());
            item.put("idProduit", favori.getIdProduit());
            item.put("dateAjout", favori.getDateAjout());

            // Vous pourriez enrichir cette réponse avec des détails du produit si nécessaire
            // Pour cela, vous auriez besoin d'injecter le ProduitRepository

            result.add(item);
        }

        return result;
    }

    /**
     * Récupérer les statistiques de favoris pour un produit
     */
    public Map<String, Object> getStatsProduit(Long idProduit) {
        long count = favorisRepository.countByIdProduit(idProduit);
        List<String> users = favorisRepository.findByIdProduit(idProduit)
                .stream()
                .map(FavorisProduit::getIdClient)
                .toList();

        Map<String, Object> stats = new HashMap<>();
        stats.put("favorisCount", count);
        stats.put("users", users);

        return stats;
    }
}