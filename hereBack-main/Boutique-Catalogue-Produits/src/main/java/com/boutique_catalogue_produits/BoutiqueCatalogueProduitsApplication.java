package com.boutique_catalogue_produits;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@EnableCaching
@EnableFeignClients
@SpringBootApplication
@EnableDiscoveryClient
@EnableJpaRepositories(basePackages = "com.boutique_catalogue_produits.repository")
@EntityScan(basePackages = "com.boutique_catalogue_produits.model")
public class BoutiqueCatalogueProduitsApplication {

	public static void main(String[] args) {
		SpringApplication.run(BoutiqueCatalogueProduitsApplication.class, args);
	}

}
