package com.servicepaiement.service;

import com.servicepaiement.dto.CartItemDTO;
import com.servicepaiement.event.CheckoutEvent;
import com.servicepaiement.model.Order;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.servicepaiement.config.RabbitMQConfig;

import java.util.List;

@Service
public class CheckoutListener {

    @Autowired
    private OrderService orderService;

    @Autowired
    private com.servicepaiement.client.CartServiceClient cartServiceClient;

    @RabbitListener(queues = RabbitMQConfig.CHECKOUT_QUEUE)
    @Transactional
    public void processCheckoutEvent(CheckoutEvent event) {
        System.out.println("✅ Événement de checkout reçu: " + event);

        try {
            // Récupérer les articles du panier depuis le Cart Service via Feign
            List<CartItemDTO> cartItems = cartServiceClient.getCartItemsByUserId(event.getUserId());

            // Créer la commande
            String orderNumber = orderService.generateOrderNumber();
            Order order = orderService.createInitialOrder(
                    event.getUserId(),
                    orderNumber,
                    cartItems,
                    event.getTotal()
            );

            System.out.println("✅ Commande créée avec succès: " + order.getOrderNumber());

        } catch (Exception e) {
            System.err.println("❌ Erreur lors du traitement de l'événement de checkout: " + e.getMessage());
            e.printStackTrace();
        }
    }
}