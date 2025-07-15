package com.boutique_catalogue_produits.dto;

import org.springframework.web.multipart.MultipartFile;
import java.util.Date;
import java.util.List;
import java.util.Map;
/**
 * 🔧 Configuration IA dynamique
 */
public class AIDynamicConfig {
    private boolean aiEnabled;
    private Map<String, Boolean> serviceStates;
    private Map<String, Double> thresholds;
    private Map<String, Integer> limits;
    private Date lastModified;

    // Constructeurs
    public AIDynamicConfig() {
        this.lastModified = new Date();
    }

    // Getters et Setters
    public boolean isAiEnabled() { return aiEnabled; }
    public void setAiEnabled(boolean aiEnabled) { this.aiEnabled = aiEnabled; }

    public Map<String, Boolean> getServiceStates() { return serviceStates; }
    public void setServiceStates(Map<String, Boolean> serviceStates) { this.serviceStates = serviceStates; }

    public Map<String, Double> getThresholds() { return thresholds; }
    public void setThresholds(Map<String, Double> thresholds) { this.thresholds = thresholds; }

    public Map<String, Integer> getLimits() { return limits; }
    public void setLimits(Map<String, Integer> limits) { this.limits = limits; }

    public Date getLastModified() { return lastModified; }
    public void setLastModified(Date lastModified) { this.lastModified = lastModified; }
}
