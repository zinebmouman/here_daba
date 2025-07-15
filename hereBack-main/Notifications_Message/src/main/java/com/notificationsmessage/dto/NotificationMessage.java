package com.notificationsmessage.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;

public class NotificationMessage implements Serializable {
    private String type;
    private String vendeurId;
    private Long productId;
    private String productName;
    private String currentStock;  // Changez en String pour plus de flexibilité
    private String seuilCritique; // Changez en String
    private String dateExpiration;
    // Nouvelles propriétés
    private Long joursRestants;
    private String messageTemps;

    // Constructeur existant - conserver tel quel
    @JsonCreator
    public NotificationMessage(
            @JsonProperty("type") String type,
            @JsonProperty("vendeurId") String vendeurId,
            @JsonProperty("productId") Long productId,
            @JsonProperty("productName") String productName,
            @JsonProperty("currentStock") String currentStock,
            @JsonProperty("seuilCritique") String seuilCritique,
            @JsonProperty("dateExpiration") String dateExpiration
    ) {
        this.type = type;
        this.vendeurId = vendeurId;
        this.productId = productId;
        this.productName = productName;
        this.currentStock = currentStock;
        this.seuilCritique = seuilCritique;
        this.dateExpiration = dateExpiration;
    }

    // Ajouter ces getters et setters
    public Long getJoursRestants() {
        return joursRestants;
    }

    public void setJoursRestants(Long joursRestants) {
        this.joursRestants = joursRestants;
    }

    public String getMessageTemps() {
        return messageTemps;
    }

    public void setMessageTemps(String messageTemps) {
        this.messageTemps = messageTemps;
    }


    // Constructeur par défaut
    public NotificationMessage() {}

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getVendeurId() {
        return vendeurId;
    }

    public void setVendeurId(String vendeurId) {
        this.vendeurId = vendeurId;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getCurrentStock() {
        return currentStock;
    }

    public void setCurrentStock(String currentStock) {
        this.currentStock = currentStock;
    }

    public String getSeuilCritique() {
        return seuilCritique;
    }

    public void setSeuilCritique(String seuilCritique) {
        this.seuilCritique = seuilCritique;
    }

    public String getDateExpiration() {
        return dateExpiration;
    }

    public void setDateExpiration(String dateExpiration) {
        this.dateExpiration = dateExpiration;
    }
}