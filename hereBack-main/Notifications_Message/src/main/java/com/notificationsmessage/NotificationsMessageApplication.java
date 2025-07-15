package com.notificationsmessage;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class NotificationsMessageApplication {
    public static void main(String[] args) {
        SpringApplication.run(NotificationsMessageApplication.class, args);
    }
}