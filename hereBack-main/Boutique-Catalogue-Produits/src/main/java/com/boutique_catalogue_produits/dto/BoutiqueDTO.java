package com.boutique_catalogue_produits.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

public class BoutiqueDTO implements Serializable {
    private String id_boutique;
    private String nom;
    private String adress;
    private String contact;
    private String horaire;
    private String ville;
    private String localisation;
    private String codePostal;

    // Ajout de @JsonProperty sur le getter et le setter (optionnel, mais recommandé)
    private String vendeurId;

    private String pays;
    private String boutique_img;
    private String autorisation_image;
    private String numero_patente;

    public BoutiqueDTO() {
    }

    public BoutiqueDTO(String id_boutique, String nom, String adress, String contact,
                       String horaire, String ville, String localisation,
                       String codePostal, String pays, String boutique_img,
                       String autorisation_image, String numero_patente) {
        this.id_boutique = id_boutique;
        this.nom = nom;
        this.adress = adress;
        this.contact = contact;
        this.horaire = horaire;
        this.ville = ville;
        this.localisation = localisation;
        this.codePostal = codePostal;
        this.pays = pays;
        this.boutique_img = boutique_img;
        this.autorisation_image = autorisation_image;
        this.numero_patente = numero_patente;
    }

    // Getters / setters (les autres inchangés)

    public String getId_boutique() {
        return id_boutique;
    }

    public void setId_boutique(String id_boutique) {
        this.id_boutique = id_boutique;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getAdress() {
        return adress;
    }

    public void setAdress(String adress) {
        this.adress = adress;
    }

    public String getContact() {
        return contact;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }

    public String getHoraire() {
        return horaire;
    }

    public void setHoraire(String horaire) {
        this.horaire = horaire;
    }

    public String getVille() {
        return ville;
    }

    public void setVille(String ville) {
        this.ville = ville;
    }

    public String getLocalisation() {
        return localisation;
    }

    public void setLocalisation(String localisation) {
        this.localisation = localisation;
    }

    public String getCodePostal() {
        return codePostal;
    }

    public void setCodePostal(String codePostal) {
        this.codePostal = codePostal;
    }

    public String getPays() {
        return pays;
    }

    public void setPays(String pays) {
        this.pays = pays;
    }

    public String getBoutique_img() {
        return boutique_img;
    }

    public void setBoutique_img(String boutique_img) {
        this.boutique_img = boutique_img;
    }

    public String getAutorisation_image() {
        return autorisation_image;
    }

    public void setAutorisation_image(String autorisation_image) {
        this.autorisation_image = autorisation_image;
    }

    public String getNumero_patente() {
        return numero_patente;
    }

    public void setNumero_patente(String numero_patente) {
        this.numero_patente = numero_patente;
    }

    @JsonProperty("vendeur_id")
    public String getVendeurId() {
        return vendeurId;
    }

    @JsonProperty("vendeur_id")
    public void setVendeurId(String vendeurId) {
        this.vendeurId = vendeurId;
    }

    public void nettoyerContact() {
        if (this.contact != null) {
            this.contact = this.contact.replaceAll("[^0-9]", "");
        }
    }

    @Override
    public String toString() {
        return "BoutiqueDTO{" +
                "id_boutique='" + id_boutique + '\'' +
                ", nom='" + nom + '\'' +
                ", adress='" + adress + '\'' +
                ", contact='" + contact + '\'' +
                ", horaire='" + horaire + '\'' +
                ", ville='" + ville + '\'' +
                ", localisation='" + localisation + '\'' +
                ", codePostal='" + codePostal + '\'' +
                ", vendeurId='" + vendeurId + '\'' +
                ", pays='" + pays + '\'' +
                ", boutique_img='" + boutique_img + '\'' +
                ", autorisation_image='" + autorisation_image + '\'' +
                ", numero_patente='" + numero_patente + '\'' +
                '}';
    }

    public boolean isValid() {
        return nom != null && !nom.isEmpty()
                && adress != null && !adress.isEmpty()
                && ville != null && !ville.isEmpty()
                && pays != null && !pays.isEmpty()
                && numero_patente != null && !numero_patente.isEmpty();
    }
}
