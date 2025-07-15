package com.notificationsmessage.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

@Configuration
@EnableRetry
public class RetryConfig {
    // La configuration de retry est définie dans application.properties
    // ou avec les annotations @Retryable sur les méthodes
}