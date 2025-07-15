package com.servicepaiement.service;

import com.servicepaiement.dto.CartItemDTO;
import com.servicepaiement.dto.OrderResponseDTO;
import com.servicepaiement.dto.ShippingInfoDTO;
import com.servicepaiement.exception.ResourceNotFoundException;
import com.servicepaiement.model.*;
import com.servicepaiement.repository.OrderItemRepository;
import com.servicepaiement.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private RabbitMQSender rabbitMQSender;

    /**
     * Génère un numéro de commande unique
     */
    public String generateOrderNumber() {
        return "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    /**
     * Crée une commande payée
     */
    @Transactional
    public Order createPaidOrder(String userId, String orderNumber, List<CartItemDTO> items,
                                 ShippingInfoDTO shippingInfo, PaymentMethod paymentMethod, Double total) {
        Order order = new Order();
        order.setUserId(userId);
        order.setOrderNumber(orderNumber);
        order.setSubtotal(total - (total * 0.05)); // Approximation du sous-total
        order.setShippingFee(total * 0.05); // Approximation des frais de livraison
        order.setTotal(total);
        order.setStatus(OrderStatus.PROCESSING);
        order.setPaymentMethod(paymentMethod);

        // Configurer les informations de livraison s'il y en a
        if (shippingInfo != null) {
            ShippingInfo info = new ShippingInfo();
            info.setFullName(shippingInfo.getFullName());
            info.setAddress(shippingInfo.getAddress());
            info.setCity(shippingInfo.getCity());
            info.setPostalCode(shippingInfo.getPostalCode());
            info.setCountry(shippingInfo.getCountry());
            info.setPhone(shippingInfo.getPhone());
            info.setEmail(shippingInfo.getEmail());

            order.setShippingInfo(info);
        }

        order.setCreatedAt(LocalDateTime.now());

        Order savedOrder = orderRepository.save(order);

        // Ajouter les articles
        for (CartItemDTO item : items) {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(savedOrder);
            orderItem.setProductId(item.getId());
            orderItem.setNomProduit(item.getNomProduit());
            orderItem.setPrix(item.getPrix());
            orderItem.setQuantite(item.getQuantite());
            orderItem.setImageUrl(item.getImageUrl());
            orderItem.setCategorie(item.getCategorie());

            orderItemRepository.save(orderItem);
        }

        // Envoyer un événement pour notifier de la commande
        rabbitMQSender.sendOrderEvent(savedOrder);

        return savedOrder;
    }

    /**
     * Crée une commande initiale (avant paiement)
     */
    @Transactional
    public Order createInitialOrder(String userId, String orderNumber, List<CartItemDTO> items, Double total) {
        Order order = new Order();
        order.setUserId(userId);
        order.setOrderNumber(orderNumber);
        order.setSubtotal(total);
        order.setShippingFee(0.0); // À calculer si nécessaire
        order.setTotal(total);
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedAt(LocalDateTime.now());

        Order savedOrder = orderRepository.save(order);

        // Ajouter les articles
        for (CartItemDTO item : items) {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(savedOrder);
            orderItem.setProductId(item.getId());
            orderItem.setNomProduit(item.getNomProduit());
            orderItem.setPrix(item.getPrix());
            orderItem.setQuantite(item.getQuantite());
            orderItem.setImageUrl(item.getImageUrl());
            orderItem.setCategorie(item.getCategorie());

            orderItemRepository.save(orderItem);
        }

        return savedOrder;
    }

    /**
     * Récupère une commande par son numéro
     */
    public OrderResponseDTO getOrderByNumber(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Commande non trouvée avec le numéro: " + orderNumber));

        return convertToDTO(order);
    }

    /**
     * Récupère les commandes d'un utilisateur
     */
    public List<OrderResponseDTO> getOrdersByUserId(String userId) {
        List<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return orders.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Met à jour le statut d'une commande
     */
    @Transactional
    public OrderResponseDTO updateOrderStatus(String orderNumber, OrderStatus status) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Commande non trouvée avec le numéro: " + orderNumber));

        order.setStatus(status);
        Order updatedOrder = orderRepository.save(order);

        // Envoyer un événement pour notifier du changement de statut
        rabbitMQSender.sendOrderEvent(updatedOrder);

        return convertToDTO(updatedOrder);
    }

    /**
     * Annule une commande
     */
    @Transactional
    public OrderResponseDTO cancelOrder(String orderNumber, String userId) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Commande non trouvée avec le numéro: " + orderNumber));

        // Vérifier que l'utilisateur est le propriétaire de la commande
        if (!order.getUserId().equals(userId)) {
            throw new SecurityException("Vous n'êtes pas autorisé à annuler cette commande");
        }

        // Vérifier que la commande peut être annulée
        if (order.getStatus() == OrderStatus.DELIVERED || order.getStatus() == OrderStatus.SHIPPED) {
            throw new IllegalStateException("Impossible d'annuler une commande déjà expédiée ou livrée");
        }

        order.setStatus(OrderStatus.CANCELLED);
        Order cancelledOrder = orderRepository.save(order);

        // Envoyer un événement pour notifier de l'annulation
        rabbitMQSender.sendOrderEvent(cancelledOrder);

        return convertToDTO(cancelledOrder);
    }

    /**
     * Récupère les commandes à livrer
     */
    public List<OrderResponseDTO> getOrdersToDeliver() {
        // Trouver toutes les commandes avec le statut SHIPPED
        List<Order> orders = orderRepository.findAll().stream()
                .filter(order -> order.getStatus() == OrderStatus.SHIPPED)
                .collect(Collectors.toList());

        return orders.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Marque une commande comme livrée
     */
    @Transactional
    public OrderResponseDTO markOrderAsDelivered(String orderNumber, String livreurId, String deliveryNotes) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Commande non trouvée avec le numéro: " + orderNumber));

        order.setStatus(OrderStatus.DELIVERED);
        order.setLivreurId(livreurId);
        order.setDeliveryNotes(deliveryNotes);
        order.setDeliveryDate(LocalDateTime.now());

        Order deliveredOrder = orderRepository.save(order);

        // Envoyer un événement pour notifier de la livraison
        rabbitMQSender.sendOrderEvent(deliveredOrder);

        return convertToDTO(deliveredOrder);
    }

    /**
     * Convertit une entité Order en DTO
     */
    private OrderResponseDTO convertToDTO(Order order) {
        OrderResponseDTO dto = new OrderResponseDTO();
        dto.setOrderNumber(order.getOrderNumber());
        dto.setStatus(order.getStatus());
        dto.setCreatedAt(order.getCreatedAt());

        // Convertir les informations de livraison
        if (order.getShippingInfo() != null) {
            ShippingInfoDTO shippingDTO = new ShippingInfoDTO();
            shippingDTO.setFullName(order.getShippingInfo().getFullName());
            shippingDTO.setAddress(order.getShippingInfo().getAddress());
            shippingDTO.setCity(order.getShippingInfo().getCity());
            shippingDTO.setPostalCode(order.getShippingInfo().getPostalCode());
            shippingDTO.setCountry(order.getShippingInfo().getCountry());
            shippingDTO.setPhone(order.getShippingInfo().getPhone());
            shippingDTO.setEmail(order.getShippingInfo().getEmail());

            dto.setShipping(shippingDTO);
        }

        dto.setPaymentMethod(order.getPaymentMethod());

        if (order.getPayment() != null) {
            dto.setPaymentStatus(order.getPayment().getStatus().toString());
        } else {
            dto.setPaymentStatus("PENDING");
        }

        // Récupérer les articles de la commande
        List<OrderItem> orderItems = orderItemRepository.findByOrderId(order.getId());
        List<CartItemDTO> itemDTOs = new ArrayList<>();

        for (OrderItem item : orderItems) {
            CartItemDTO itemDTO = new CartItemDTO();
            itemDTO.setId(item.getProductId());
            itemDTO.setNomProduit(item.getNomProduit());
            itemDTO.setPrix(item.getPrix());
            itemDTO.setQuantite(item.getQuantite());
            itemDTO.setImageUrl(item.getImageUrl());
            itemDTO.setCategorie(item.getCategorie());

            itemDTOs.add(itemDTO);
        }

        dto.setItems(itemDTOs);
        dto.setSubtotal(order.getSubtotal());
        dto.setShippingFee(order.getShippingFee());
        dto.setTotal(order.getTotal());

        return dto;
    }
}