package com.boutique_catalogue_produits.dto;

public class PriceRange {
    private double min;
    private double max;
    private double average;

    // Constructeurs
    public PriceRange() {}

    public PriceRange(double min, double max, double average) {
        this.min = min;
        this.max = max;
        this.average = average;
    }

    // Getters et Setters
    public double getMin() { return min; }
    public void setMin(double min) { this.min = min; }

    public double getMax() { return max; }
    public void setMax(double max) { this.max = max; }

    public double getAverage() { return average; }
    public void setAverage(double average) { this.average = average; }
}
