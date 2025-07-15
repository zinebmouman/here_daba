package com.notificationsmessage.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    public static final String EXCHANGE_NAME = "stock-notifications";
    public static final String QUEUE_CRITICAL_STOCK = "critical-stock-queue";
    public static final String QUEUE_PRODUCT_EXPIRATION = "product-expiration-queue";

    @Value("${spring.rabbitmq.host}")
    private String rabbitmqHost;

    @Value("${spring.rabbitmq.port}")
    private int rabbitmqPort;

    @Value("${spring.rabbitmq.username}")
    private String rabbitmqUsername;

    @Value("${spring.rabbitmq.password}")
    private String rabbitmqPassword;

    @Bean
    public TopicExchange stockExchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public Queue criticalStockQueue() {
        return new Queue(QUEUE_CRITICAL_STOCK, true);
    }

    @Bean
    public Queue productExpirationQueue() {
        return new Queue(QUEUE_PRODUCT_EXPIRATION, true);
    }

    @Bean
    public Binding criticalStockBinding(Queue criticalStockQueue, TopicExchange stockExchange) {
        return BindingBuilder.bind(criticalStockQueue)
                .to(stockExchange)
                .with("stock.critical");
    }

    @Bean
    public Binding productExpirationBinding(Queue productExpirationQueue, TopicExchange stockExchange) {
        return BindingBuilder.bind(productExpirationQueue)
                .to(stockExchange)
                .with("product.expiration");
    }


    @Bean
    public ConnectionFactory connectionFactory() {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory(rabbitmqHost);
        connectionFactory.setPort(rabbitmqPort);
        connectionFactory.setUsername(rabbitmqUsername);
        connectionFactory.setPassword(rabbitmqPassword);
        connectionFactory.setRequestedHeartBeat(30);
        connectionFactory.setConnectionTimeout(60000);
        return connectionFactory;
    }

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return objectMapper;
    }

    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         Jackson2JsonMessageConverter jsonMessageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter);
        return rabbitTemplate;
    }
}