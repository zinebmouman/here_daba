package com.servicepaiement.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Noms des queues
    public static final String CHECKOUT_QUEUE = "checkout-queue";
    public static final String ORDER_QUEUE = "order-queue";

    // Noms des exchanges
    public static final String CHECKOUT_EXCHANGE = "checkout-exchange";
    public static final String ORDER_EXCHANGE = "order-exchange";

    // Routing keys
    public static final String CHECKOUT_ROUTING_KEY = "checkout.event";
    public static final String ORDER_ROUTING_KEY = "order.event";

    // Configuration de la queue de checkout
    @Bean
    public Queue checkoutQueue() {
        return new Queue(CHECKOUT_QUEUE, true);
    }

    // Configuration de la queue d'order
    @Bean
    public Queue orderQueue() {
        return new Queue(ORDER_QUEUE, true);
    }

    // Configuration de l'exchange pour les événements de checkout
    @Bean
    public DirectExchange checkoutExchange() {
        return new DirectExchange(CHECKOUT_EXCHANGE);
    }

    // Configuration de l'exchange pour les événements d'order
    @Bean
    public DirectExchange orderExchange() {
        return new DirectExchange(ORDER_EXCHANGE);
    }

    // Liaison entre la queue et l'exchange de checkout
    @Bean
    public Binding checkoutBinding(Queue checkoutQueue, DirectExchange checkoutExchange) {
        return BindingBuilder.bind(checkoutQueue).to(checkoutExchange).with(CHECKOUT_ROUTING_KEY);
    }

    // Liaison entre la queue et l'exchange d'order
    @Bean
    public Binding orderBinding(Queue orderQueue, DirectExchange orderExchange) {
        return BindingBuilder.bind(orderQueue).to(orderExchange).with(ORDER_ROUTING_KEY);
    }

    // Configuration pour la conversion JSON des messages
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // Template RabbitMQ pour l'envoi de messages
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }
}