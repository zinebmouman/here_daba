package com.notificationsmessage.config;

import feign.Logger;
import feign.Retryer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignConfig {

    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }

    @Bean
    public Retryer feignRetryer() {
        // Retryer.Default(100L, TimeUnit.SECONDS.toMillis(1L), 3);
        // Le premier paramètre est le délai de base en millisecondes
        // Le deuxième paramètre est le délai maximum en millisecondes
        // Le troisième paramètre est le nombre maximum de tentatives
        return new Retryer.Default(1000, 5000, 3);
    }
}