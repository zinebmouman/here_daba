package com.servicepaiement.service;

import com.servicepaiement.config.RabbitMQConfig;
import com.servicepaiement.event.OrderEvent;
import com.servicepaiement.model.Order;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class RabbitMQSender {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    public void sendOrderEvent(Order order) {
        OrderEvent event = new OrderEvent();
        event.setOrderId(order.getId());
        event.setOrderNumber(order.getOrderNumber());
        event.setUserId(order.getUserId());
        event.setStatus(order.getStatus());
        event.setAmount(order.getTotal());
        event.setTimestamp(LocalDateTime.now());

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.ORDER_EXCHANGE,
                RabbitMQConfig.ORDER_ROUTING_KEY,
                event
        );

        System.out.println("✅ Événement d'order envoyé: " + event);
    }
}