package com.cartservice.service;

import com.cartservice.config.RabbitMQConfig;
import com.cartservice.event.CheckoutEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class RabbitMQSender {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    public void sendCheckoutEvent(CheckoutEvent event) {
        // Si l'horodatage n'est pas défini, le définir maintenant
        if (event.getTimestamp() == null) {
            event.setTimestamp(LocalDateTime.now());
        }

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.CHECKOUT_EXCHANGE,
                RabbitMQConfig.CHECKOUT_ROUTING_KEY,
                event
        );

        System.out.println("✅ Événement de checkout envoyé: " + event);
    }
}