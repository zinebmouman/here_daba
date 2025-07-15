package com.example.authentification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;

@SpringBootApplication
@EnableJpaRepositories("com.example.authentification.repository")
public class AuthentificationApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuthentificationApplication.class, args);
    }
}
