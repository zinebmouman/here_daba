package com.boutique_catalogue_produits.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "rag")
public class RAGConfiguration {

    // Configuration principale
    private boolean enabled = true;
    private String version = "1.0";
    private String environment = "development";

    // Configuration cache
    private Cache cache = new Cache();

    // Configuration recherche
    private Search search = new Search();

    // Configuration scoring
    private Scoring scoring = new Scoring();

    // Configuration similarité
    private Similarity similarity = new Similarity();

    // Classes internes pour les sous-configurations
    public static class Cache {
        private boolean enabled = true;
        private int durationMinutes = 10;
        private int maxSize = 100;
        private int cleanupIntervalMinutes = 5;

        // Getters et Setters
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public int getDurationMinutes() { return durationMinutes; }
        public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }

        public int getMaxSize() { return maxSize; }
        public void setMaxSize(int maxSize) { this.maxSize = maxSize; }

        public int getCleanupIntervalMinutes() { return cleanupIntervalMinutes; }
        public void setCleanupIntervalMinutes(int cleanupIntervalMinutes) { this.cleanupIntervalMinutes = cleanupIntervalMinutes; }
    }

    public static class Search {
        private int maxResults = 50;
        private int maxConcurrent = 5;
        private int timeoutSeconds = 30;

        // Getters et Setters
        public int getMaxResults() { return maxResults; }
        public void setMaxResults(int maxResults) { this.maxResults = maxResults; }

        public int getMaxConcurrent() { return maxConcurrent; }
        public void setMaxConcurrent(int maxConcurrent) { this.maxConcurrent = maxConcurrent; }

        public int getTimeoutSeconds() { return timeoutSeconds; }
        public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
    }

    public static class Scoring {
        private double keywordWeight = 0.4;
        private double textSimilarityWeight = 0.3;
        private double categoryWeight = 0.2;
        private double priceWeight = 0.1;
        private double exactMatchBonus = 0.2;
        private double partialMatchBonus = 0.1;

        // Getters et Setters
        public double getKeywordWeight() { return keywordWeight; }
        public void setKeywordWeight(double keywordWeight) { this.keywordWeight = keywordWeight; }

        public double getTextSimilarityWeight() { return textSimilarityWeight; }
        public void setTextSimilarityWeight(double textSimilarityWeight) { this.textSimilarityWeight = textSimilarityWeight; }

        public double getCategoryWeight() { return categoryWeight; }
        public void setCategoryWeight(double categoryWeight) { this.categoryWeight = categoryWeight; }

        public double getPriceWeight() { return priceWeight; }
        public void setPriceWeight(double priceWeight) { this.priceWeight = priceWeight; }

        public double getExactMatchBonus() { return exactMatchBonus; }
        public void setExactMatchBonus(double exactMatchBonus) { this.exactMatchBonus = exactMatchBonus; }

        public double getPartialMatchBonus() { return partialMatchBonus; }
        public void setPartialMatchBonus(double partialMatchBonus) { this.partialMatchBonus = partialMatchBonus; }
    }

    public static class Similarity {
        private double thresholdMinimum = 0.3;
        private double thresholdGood = 0.6;
        private double thresholdExcellent = 0.8;

        // Getters et Setters
        public double getThresholdMinimum() { return thresholdMinimum; }
        public void setThresholdMinimum(double thresholdMinimum) { this.thresholdMinimum = thresholdMinimum; }

        public double getThresholdGood() { return thresholdGood; }
        public void setThresholdGood(double thresholdGood) { this.thresholdGood = thresholdGood; }

        public double getThresholdExcellent() { return thresholdExcellent; }
        public void setThresholdExcellent(double thresholdExcellent) { this.thresholdExcellent = thresholdExcellent; }
    }

    // Getters et Setters principaux
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public String getEnvironment() { return environment; }
    public void setEnvironment(String environment) { this.environment = environment; }

    public Cache getCache() { return cache; }
    public void setCache(Cache cache) { this.cache = cache; }

    public Search getSearch() { return search; }
    public void setSearch(Search search) { this.search = search; }

    public Scoring getScoring() { return scoring; }
    public void setScoring(Scoring scoring) { this.scoring = scoring; }

    public Similarity getSimilarity() { return similarity; }
    public void setSimilarity(Similarity similarity) { this.similarity = similarity; }
}