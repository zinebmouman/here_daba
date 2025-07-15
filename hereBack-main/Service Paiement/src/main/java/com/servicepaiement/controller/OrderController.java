package com.servicepaiement.controller;



import com.servicepaiement.dto.OrderResponseDTO;
import com.servicepaiement.model.OrderStatus;

import com.servicepaiement.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    // Récupérer une commande par son numéro
    @GetMapping("/{orderNumber}")
    public ResponseEntity<OrderResponseDTO> getOrderByNumber(
            @PathVariable String orderNumber) {
        return ResponseEntity.ok(orderService.getOrderByNumber(orderNumber));
    }

    // Récupérer les commandes d'un utilisateur
    @GetMapping
    public ResponseEntity<List<OrderResponseDTO>> getOrdersByUser(
            @RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(orderService.getOrdersByUserId(userId));
    }

    // Mettre à jour le statut d'une commande
    @PutMapping("/{orderNumber}/status")
    public ResponseEntity<OrderResponseDTO> updateOrderStatus(
            @PathVariable String orderNumber,
            @RequestParam OrderStatus status) {
        return ResponseEntity.ok(orderService.updateOrderStatus(orderNumber, status));
    }

    // Annuler une commande
    @PostMapping("/{orderNumber}/cancel")
    public ResponseEntity<OrderResponseDTO> cancelOrder(
            @PathVariable String orderNumber,
            @RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(orderService.cancelOrder(orderNumber, userId));
    }

    // Récupérer les commandes à livrer (pour les livreurs)
    @GetMapping("/to-deliver")
    public ResponseEntity<List<OrderResponseDTO>> getOrdersToDeliver() {
        return ResponseEntity.ok(orderService.getOrdersToDeliver());
    }

    // Mettre à jour une commande comme livrée
    @PutMapping("/{orderNumber}/delivered")
    public ResponseEntity<OrderResponseDTO> markOrderAsDelivered(
            @PathVariable String orderNumber,
            @RequestHeader("X-User-Id") String livreurId,
            @RequestParam(required = false) String deliveryNotes) {
        return ResponseEntity.ok(orderService.markOrderAsDelivered(orderNumber, livreurId, deliveryNotes));
    }
}