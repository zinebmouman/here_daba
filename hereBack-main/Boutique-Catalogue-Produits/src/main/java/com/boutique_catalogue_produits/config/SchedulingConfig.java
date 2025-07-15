package com.boutique_catalogue_produits.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class SchedulingConfig {
    // La configuration @EnableScheduling active les tâches planifiées dans l'application
    // Aucune autre configuration n'est nécessaire ici
}