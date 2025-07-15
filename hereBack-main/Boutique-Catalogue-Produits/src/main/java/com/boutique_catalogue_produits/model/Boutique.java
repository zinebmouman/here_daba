package com.boutique_catalogue_produits.model;

import com.boutique_catalogue_produits.dto.BoutiqueDTO;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.micrometer.common.util.StringUtils;
import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;


@Entity
@Table(name = "boutique")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Boutique {

    @Id
    @Column(name = "id_boutique")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id_boutique;

    @Column(name = "vendeur_id", nullable = false)
    private String vendeurId;

    @Column(name = "nom", nullable = false, length = 100)
    private String nom;

    @Column(name = "adress", columnDefinition = "text")
    private String adress;

    @Column(name = "horaire", columnDefinition = "text")
    private String horaire;

    @Column(name = "localisation")
    private String localisation;

    @Column(name = "autorisation_image")
    private String autorisation_image;

    @Column(name = "numero_patente", length = 50)
    private String numero_patente;

    @Column(name = "boutique_img")
    private String boutique_img;
    @Column(name = "boutique_img_url")
    private String boutiqueImgUrl;
    @Column(name = "ville")
    private String ville;

    @Column(name = "codePostal")
    private Integer codePostal;

    @Column(name = "pays")
    private String pays;



    @Column(name = "contact", nullable = false)
    private Long contact = 0L;
    @JsonIgnore
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "boutique_categorie",
            joinColumns = @JoinColumn(name = "id_boutique"),
            inverseJoinColumns = @JoinColumn(name = "id_categorie")
    )
    private Set<Categorie> categories = new HashSet<>();


    // Méthodes de validation et de conversion
    @Transient
    public boolean isValid() {
        return StringUtils.isNotBlank(nom)
                && StringUtils.isNotBlank(adress)
                && StringUtils.isNotBlank(ville)
                && StringUtils.isNotBlank(pays)
                && contact != null;
    }

    // Méthode de conversion depuis DTO
    public static Boutique fromFrontendForm(BoutiqueDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Les données de la boutique sont nulles");
        }

        Boutique boutique = new Boutique();

        // Validation et attribution des champs
        boutique.setNom(sanitizeString(dto.getNom(), "Nom"));
        boutique.setAdress(sanitizeString(dto.getAdress(), "Adresse"));
        boutique.setVille(sanitizeString(dto.getVille(), "Ville"));
        boutique.setPays(sanitizeString(dto.getPays(), "Pays"));

        // Gestion des champs optionnels
        boutique.setHoraire(dto.getHoraire());
        boutique.setNumero_patente(dto.getNumero_patente());
        boutique.setAutorisation_image(dto.getAutorisation_image());
        boutique.setBoutique_img(dto.getBoutique_img());

        // Conversion de l'ID si présent
        if (StringUtils.isNotBlank(dto.getId_boutique())) {
            try {
                boutique.setId_boutique(Integer.parseInt(dto.getId_boutique()));
            } catch (NumberFormatException ignored) {}
        }
        if (dto.getBoutique_img() != null && !dto.getBoutique_img().isEmpty()) {
            boutique.setBoutique_img(dto.getBoutique_img());
            System.out.println("Image définie depuis DTO: " + dto.getBoutique_img());
        }
        // Conversion du code postal
        if (StringUtils.isNotBlank(dto.getCodePostal())) {
            try {
                boutique.setCodePostal(Integer.parseInt(dto.getCodePostal()));
            } catch (NumberFormatException ignored) {}
        }

        // Gestion du contact
        boutique.setContactFromString(dto.getContact());

        // Gestion de la localisation
        boutique.setLocalisationFromFrontend(dto.getLocalisation());

        return boutique;
    }
    public String getBoutiqueImgUrl() {
        return boutiqueImgUrl;
    }

    public void setBoutiqueImgUrl(String boutiqueImgUrl) {
        this.boutiqueImgUrl = boutiqueImgUrl;
    }
    // Méthode de nettoyage des chaînes
    private static String sanitizeString(String input, String fieldName) {
        if (StringUtils.isBlank(input)) {
            throw new IllegalArgumentException(fieldName + " est requis");
        }
        return input.trim();
    }

    // Méthode de gestion du contact
    public void setContactFromString(String contactStr) {
        // Log de débogage
        System.out.println("Conversion du contact : " + contactStr);

        // Valeur par défaut si le contact est null ou vide
        if (contactStr == null || contactStr.trim().isEmpty()) {
            this.contact = 0L;
            System.out.println("Contact vide ou null, valeur par défaut : 0");
            return;
        }

        try {
            // Nettoyer le numéro en ne gardant que les chiffres
            String digitsOnly = contactStr.replaceAll("[^0-9]", "");

            System.out.println("Chiffres extraits : " + digitsOnly);

            // Si aucun chiffre n'est trouvé
            if (digitsOnly.isEmpty()) {
                this.contact = 0L;
                System.out.println("Aucun chiffre trouvé, valeur par défaut : 0");
                return;
            }

            // Limiter à 15 chiffres maximum
            digitsOnly = digitsOnly.substring(0, Math.min(digitsOnly.length(), 15));

            // Convertir en Long
            this.contact = Long.parseLong(digitsOnly);

            System.out.println("Contact converti : " + this.contact);

        } catch (NumberFormatException e) {
            // En cas d'erreur de conversion
            this.contact = 0L;
            System.err.println("Erreur de conversion du contact : " + e.getMessage());
        }
    }

    // Méthode de gestion de la localisation
    public void setLocalisationFromFrontend(String frontendLocalisation) {
        if (StringUtils.isBlank(frontendLocalisation)) {
            return;
        }

        try {
            String[] parts = frontendLocalisation.split(",");
            if (parts.length == 2) {
                double latitude = Double.parseDouble(parts[0].trim());
                double longitude = Double.parseDouble(parts[1].trim());
                this.localisation = String.format("(%f,%f)", longitude, latitude);
            }
        } catch (Exception ignored) {}
    }

    public Integer getId_boutique() {
        return id_boutique;
    }

    public void setId_boutique(Integer id_boutique) {
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

    public String getHoraire() {
        return horaire;
    }

    public void setHoraire(String horaire) {
        this.horaire = horaire;
    }

    public String getLocalisation() {
        return localisation;
    }

    public void setLocalisation(String localisation) {
        this.localisation = localisation;
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

    public String getBoutique_img() {
        return boutique_img;
    }

    public void setBoutique_img(String boutique_img) {
        this.boutique_img = boutique_img;
    }

    public String getVille() {
        return ville;
    }

    public void setVille(String ville) {
        this.ville = ville;
    }

    public Integer getCodePostal() {
        return codePostal;
    }

    public void setCodePostal(Integer codePostal) {
        this.codePostal = codePostal;
    }

    public String getPays() {
        return pays;
    }

    public void setPays(String pays) {
        this.pays = pays;
    }

    public Long getContact() {
        return contact;
    }

    public void setContact(Long contact) {
        this.contact = contact;
    }

    public Set<Categorie> getCategories() {
        return categories;
    }

    public void setCategories(Set<Categorie> categories) {
        this.categories = categories;
    }

    public String getVendeurId() {
        return vendeurId;
    }

    public void setVendeurId(String vendeurId) {
        this.vendeurId = vendeurId;
    }
}