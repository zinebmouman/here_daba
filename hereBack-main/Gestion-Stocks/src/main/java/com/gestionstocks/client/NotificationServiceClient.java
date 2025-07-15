package com.gestionstocks.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "NOTIFICATIONS-MESSAGE")
public interface NotificationServiceClient {

    @PostMapping("/api/notifications/stock")
    @CircuitBreaker(name = "notificationService", fallbackMethod = "sendStockNotificationFallback")
    void sendStockNotification(@RequestBody Map<String, Object> notificationData);

    // Méthode fallback
    default void sendStockNotificationFallback(Map<String, Object> notificationData, Exception e) {
        // Journaliser l'erreur mais pas d'action nécessaire car notification est non bloquante
    }
}