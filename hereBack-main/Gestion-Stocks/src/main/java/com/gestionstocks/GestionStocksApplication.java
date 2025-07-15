package com.gestionstocks;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication
@EnableFeignClients(basePackages = "com.gestionstocks.client")
@EnableDiscoveryClient
@EntityScan(basePackages = "com.gestionstocks.model")
@EnableJpaRepositories(basePackages = "com.gestionstocks.repository")
public class GestionStocksApplication {

	public static void main(String[] args) {
		SpringApplication.run(GestionStocksApplication.class, args);
	}
}

