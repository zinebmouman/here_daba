package com.notificationsmessage.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "type")
    private String type;

    @Column(name = "message")
    private String message;

    @Column(name = "date_envoi")
    private LocalDateTime dateEnvoi;

    @Column(name = "vendeur_id", length = 100)
    private String vendeurId;  // ID Firebase du vendeur, donc String

    @Column(name = "produit_id")
    private Long produitId;


    @Column(name = "status")
    private String status = "NON_LU";

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getDateEnvoi() {
        return dateEnvoi;
    }

    public void setDateEnvoi(LocalDateTime dateEnvoi) {
        this.dateEnvoi = dateEnvoi;
    }

    public String getVendeurId() {
        return vendeurId;
    }

    public void setVendeurId(String vendeurId) {
        this.vendeurId = vendeurId;
    }

    public Long getProduitId() {
        return produitId;
    }

    public void setProduitId(Long produitId) {
        this.produitId = produitId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}