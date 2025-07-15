package com.servicepaiement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableDiscoveryClient // Pour l'enregistrement auprès d'Eureka
@EnableFeignClients // Pour utiliser Feign
@SpringBootApplication
public class ServicePaiementApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServicePaiementApplication.class, args);
    }

}
