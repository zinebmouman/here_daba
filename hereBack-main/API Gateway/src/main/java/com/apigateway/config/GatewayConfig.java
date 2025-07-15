package com.apigateway.config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;

import java.util.Arrays;
import java.util.Collections;

@Configuration
public class GatewayConfig {
    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }
    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // Route pour l'authentification de base
                .route("auth-service", r -> r
                        .path("/api/auth/**")
                        .uri("lb://AUTHENTIFICATION"))
                // Route pour la gestion des utilisateurs généraux
                .route("users-service", r -> r
                        .path("/api/users/**")
                        .uri("lb://AUTHENTIFICATION"))
                // Route pour la gestion des vendeurs
                .route("vendeurs-service", r -> r
                        .path("/api/vendeurs/**")
                        .uri("lb://AUTHENTIFICATION"))
                // Route pour la gestion des livreurs
                .route("livreurs-service", r -> r
                        .path("/api/livreurs/**")
                        .uri("lb://AUTHENTIFICATION"))

                // Route pour la boutique
                .route("boutique-catalogue-routes", r -> r
                        .path("/api/boutiques/**", "/api/categories/**", "/api/produits/**","/api/favoris/**", "/api/reductions/**")
                        .uri("lb://BOUTIQUE-CATALOGUE-PRODUITS")
                )
                // Dans GatewayConfig.java, ajoutez cette route
                .route("placeholder-route", r -> r
                        .path("/api/placeholder/**")
                        .uri("lb://BOUTIQUE-CATALOGUE-PRODUITS")
                )
                // Route pour les images de la boutique
                .route("boutique-catalogue-images-routes", r -> r
                        .path("/api/fichiers/**")
                        .uri("lb://BOUTIQUE-CATALOGUE-PRODUITS")
                )
                // Route pour les stocks
                .route("stocks-routes", r -> r
                        .path("/api/stocks/**", "/api/stock-transactions/**", "/api/stock-notifications/**")  // Ajout du motif /**
                        .uri("lb://GESTION-STOCKS"))
                // Route pour les stocks
                .route("notification-routes", r -> r
                        .path("/api/notifications/**")
                        .uri("lb://NOTIFICATIONS-MESSAGE"))
                .route("cart-service", r -> r
                        .path("/api/cart/**")
                        .uri("http://localhost:8087"))
// Route pour health check
                .route("health-check", r -> r
                        .path("/health")
                        .uri("lb://API-GATEWAY"))

                .route("payment-service", r -> r.
                        path("/api/payments/**", "/api/orders/**")
                        .uri("lb://SERVICE-PAIEMENT"))


                .build();
    }
    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();

        corsConfig.setAllowedOrigins(Arrays.asList(
                "http://localhost:5173",
                "http://localhost:5174",
                "http://localhost:5175",
                "http://localhost:5176",
                "http://localhost:3000",
                "http://192.168.100.58:5173"
        ));

        corsConfig.setMaxAge(3600L);
        corsConfig.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"
        ));
        corsConfig.setAllowedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "Accept",
                "X-Requested-With",
                "Cache-Control",
                "X-Vendeur-ID",
                "X-User-Id"
        ));
        corsConfig.setExposedHeaders(Arrays.asList(
                "Authorization",
                "Content-Disposition"
        ));
        corsConfig.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);

        return new CorsWebFilter(source);
    }
}