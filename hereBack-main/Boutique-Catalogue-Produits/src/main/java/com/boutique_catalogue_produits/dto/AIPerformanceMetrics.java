package com.boutique_catalogue_produits.dto;
import org.springframework.web.multipart.MultipartFile;
import java.util.Date;
import java.util.List;
import java.util.Map;
public class AIPerformanceMetrics {
    private Map<String, Long> averageResponseTimes;
    private Map<String, Integer> requestCounts;
    private Map<String, Double> successRates;
    private Map<String, Object> cacheStatistics;
    private Date lastUpdated;

    // Constructeurs
    public AIPerformanceMetrics() {
        this.lastUpdated = new Date();
    }

    // Getters et Setters
    public Map<String, Long> getAverageResponseTimes() { return averageResponseTimes; }
    public void setAverageResponseTimes(Map<String, Long> averageResponseTimes) {
        this.averageResponseTimes = averageResponseTimes;
    }

    public Map<String, Integer> getRequestCounts() { return requestCounts; }
    public void setRequestCounts(Map<String, Integer> requestCounts) { this.requestCounts = requestCounts; }

    public Map<String, Double> getSuccessRates() { return successRates; }
    public void setSuccessRates(Map<String, Double> successRates) { this.successRates = successRates; }

    public Map<String, Object> getCacheStatistics() { return cacheStatistics; }
    public void setCacheStatistics(Map<String, Object> cacheStatistics) {
        this.cacheStatistics = cacheStatistics;
    }

    public Date getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(Date lastUpdated) { this.lastUpdated = lastUpdated; }
}

