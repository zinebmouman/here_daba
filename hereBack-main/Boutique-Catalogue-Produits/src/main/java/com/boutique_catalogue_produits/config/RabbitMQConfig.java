package com.boutique_catalogue_produits.config; // Ajustez le package selon votre structure

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    public static final String EXCHANGE_NAME = "stock-notifications";
    public static final String QUEUE_PRODUCT_EXPIRATION = "product-expiration-queue";
    public static final String ROUTING_KEY_PRODUCT_EXPIRATION = "product.expiration";
    public static final String QUEUE_CRITICAL_STOCK = "critical-stock-queue";
    public static final String ROUTING_KEY_CRITICAL_STOCK = "stock.critical";
    @Bean
    public TopicExchange stockExchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public Queue productExpirationQueue() {
        return new Queue(QUEUE_PRODUCT_EXPIRATION, true);
    }

    @Bean
    public Binding productExpirationBinding(Queue productExpirationQueue, TopicExchange stockExchange) {
        return BindingBuilder.bind(productExpirationQueue)
                .to(stockExchange)
                .with(ROUTING_KEY_PRODUCT_EXPIRATION);
    }
}