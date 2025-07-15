package com.cartservice.controller;

import com.cartservice.dto.CartItemDTO;
import com.cartservice.dto.CartResponseDTO;
import com.cartservice.model.CartItem;
import com.cartservice.service.CartService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cart")
public class CartController {
    // Déclaration du logger
    private static final Logger logger = LoggerFactory.getLogger(CartController.class);

    @Autowired
    private CartService cartService;

    @PostMapping("/{userId}/items")
    public ResponseEntity<CartItemDTO> addItemToCart(
            @PathVariable String userId,
            @RequestBody CartItemDTO cartItemDTO) {
        logger.info("POST /api/cart/{}/items - Ajout d'un article au panier - {}", userId, cartItemDTO);
        try {
            CartItemDTO addedItem = cartService.addItemToCart(userId, cartItemDTO);
            logger.info("Article ajouté avec succès - id: {}", addedItem.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(addedItem);
        } catch (Exception e) {
            logger.error("Erreur lors de l'ajout d'un article au panier - userId: {}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/{userId}")
    public ResponseEntity<CartResponseDTO> getCartByUserId(@PathVariable String userId) {
        logger.info("GET /api/cart/{} - Récupération du panier", userId);
        try {
            CartResponseDTO cart = cartService.getCartByUserId(userId);
            logger.info("Panier récupéré avec succès - {} articles trouvés", cart.getItems().size());
            return ResponseEntity.ok(cart);
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération du panier - userId: {}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PutMapping("/{userId}/items/{itemId}")
    public ResponseEntity<CartItemDTO> updateCartItem(
            @PathVariable String userId,
            @PathVariable Long itemId,
            @RequestBody CartItemDTO cartItemDTO) {
        logger.info("PUT /api/cart/{}/items/{} - Mise à jour d'un article du panier", userId, itemId);
        try {
            CartItemDTO updatedItem = cartService.updateCartItem(userId, itemId, cartItemDTO);
            logger.info("Article mis à jour avec succès - id: {}", updatedItem.getId());
            return ResponseEntity.ok(updatedItem);
        } catch (Exception e) {
            logger.error("Erreur lors de la mise à jour d'un article du panier - userId: {}, itemId: {}", userId, itemId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @DeleteMapping("/{userId}/items/{itemId}")
    public ResponseEntity<Void> removeItemFromCart(
            @PathVariable String userId,
            @PathVariable Long itemId) {
        logger.info("DELETE /api/cart/{}/items/{} - Suppression d'un article du panier", userId, itemId);
        try {
            cartService.removeItemFromCart(userId, itemId);
            logger.info("Article supprimé avec succès");
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            logger.error("Erreur lors de la suppression d'un article du panier - userId: {}, itemId: {}", userId, itemId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> clearCart(@PathVariable String userId) {
        logger.info("DELETE /api/cart/{} - Vidage du panier", userId);
        try {
            cartService.clearCart(userId);
            logger.info("Panier vidé avec succès");
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            logger.error("Erreur lors du vidage du panier - userId: {}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/{userId}/checkout")
    public ResponseEntity<CartResponseDTO> checkout(@PathVariable String userId) {
        logger.info("POST /api/cart/{}/checkout - Préparation de la commande", userId);
        try {
            CartResponseDTO checkoutResult = cartService.prepareForCheckout(userId);
            logger.info("Commande préparée avec succès");
            return ResponseEntity.ok(checkoutResult);
        } catch (RuntimeException e) {
            logger.warn("Erreur lors de la préparation de la commande - userId: {}, message: {}", userId, e.getMessage());
            return ResponseEntity.badRequest().body(null);
        } catch (Exception e) {
            logger.error("Erreur inattendue lors de la préparation de la commande - userId: {}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    // Endpoint public pour vérifier l'état du service
    @GetMapping("/public/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Cart Service is running");
    }
}