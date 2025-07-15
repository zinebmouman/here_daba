package com.cartservice.service;

import com.cartservice.dto.CartItemDTO;
import com.cartservice.dto.CartResponseDTO;
import com.cartservice.event.CheckoutEvent;
import com.cartservice.model.CartItem;
import com.cartservice.repository.CartItemRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CartService {

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private RabbitMQSender rabbitMQSender;

    @Autowired
    private AuthServiceClient authServiceClient; // Client pour accéder au service d'authentification


    /**
     * Prépare le panier pour le checkout
     */
    @Transactional
    public CartResponseDTO prepareForCheckout(String userId) {
        List<CartItem> cartItems = cartItemRepository.findByUserId(userId);

        if (cartItems.isEmpty()) {
            throw new RuntimeException("Le panier est vide");
        }

        // Calculer le sous-total
        double subtotal = cartItems.stream()
                .mapToDouble(item -> item.getPrix() * item.getQuantite())
                .sum();

        // Récupérer les informations utilisateur depuis le service d'authentification
        String email = authServiceClient.getUserEmail(userId);

        // Créer et envoyer l'événement de checkout
        CheckoutEvent checkoutEvent = new CheckoutEvent();
        checkoutEvent.setCartId(cartItems.get(0).getId()); // ou un ID de panier si vous en avez un
        checkoutEvent.setUserId(userId);
        checkoutEvent.setUserEmail(email);
        checkoutEvent.setTotal(subtotal);
        checkoutEvent.setTimestamp(LocalDateTime.now());

        // Envoyer l'événement via RabbitMQ
        rabbitMQSender.sendCheckoutEvent(checkoutEvent);

        // Retourner la réponse
        List<CartItemDTO> cartItemDTOs = cartItems.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return new CartResponseDTO(userId, cartItemDTOs, subtotal);
    }
    /**
     * Récupère le panier d'un utilisateur
     */
    public CartResponseDTO getCartByUserId(String userId) {
        List<CartItem> cartItems = cartItemRepository.findByUserId(userId);

        // Calculer le sous-total
        double subtotal = cartItems.stream()
                .mapToDouble(item -> item.getPrix() * item.getQuantite())
                .sum();

        // Convertir les entités en DTOs
        List<CartItemDTO> cartItemDTOs = cartItems.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return new CartResponseDTO(userId, cartItemDTOs, subtotal);
    }

    /**
     * Ajoute un item au panier
     */
    @Transactional
    public CartItemDTO addItemToCart(String userId, CartItemDTO cartItemDTO) {
        // Vérifier si le produit existe déjà dans le panier
        Optional<CartItem> existingItemOpt = cartItemRepository.findByUserIdAndProductId(
                userId, cartItemDTO.getProductId());

        CartItem cartItem;

        if (existingItemOpt.isPresent()) {
            // Mettre à jour la quantité si le produit existe déjà
            cartItem = existingItemOpt.get();
            cartItem.setQuantite(cartItem.getQuantite() + cartItemDTO.getQuantite());
        } else {
            // Créer un nouvel item si le produit n'existe pas dans le panier
            cartItem = new CartItem();
            cartItem.setUserId(userId);

            cartItem.setNomProduit(cartItemDTO.getNomProduit());
            cartItem.setPrix(cartItemDTO.getPrix());
            cartItem.setImageUrl(cartItemDTO.getImageUrl());
            cartItem.setQuantite(cartItemDTO.getQuantite());
            cartItem.setCategorie(cartItemDTO.getCategorie());
            cartItem.setProductId(cartItemDTO.getProductId());
        }

        cartItem = cartItemRepository.save(cartItem);
        return convertToDTO(cartItem);
    }

    /**
     * Met à jour un item du panier
     */
    @Transactional
    public CartItemDTO updateCartItem(String userId, Long itemId, CartItemDTO cartItemDTO) {
        CartItem cartItem = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item not found"));

        // Vérifier que l'item appartient à l'utilisateur
        if (!cartItem.getUserId().equals(userId)) {
            throw new RuntimeException("Item does not belong to user");
        }

        // Mettre à jour les champs modifiables
        cartItem.setQuantite(cartItemDTO.getQuantite());

        cartItem = cartItemRepository.save(cartItem);
        return convertToDTO(cartItem);
    }

    /**
     * Supprime un item du panier
     */
    @Transactional
    public void removeItemFromCart(String userId, Long itemId) {
        cartItemRepository.deleteByUserIdAndId(userId, itemId);
    }

    /**
     * Vide le panier d'un utilisateur
     */
    @Transactional
    public void clearCart(String userId) {
        cartItemRepository.deleteAllByUserId(userId);
    }

    /**
     * Convertit une entité CartItem en DTO
     */
    private CartItemDTO convertToDTO(CartItem cartItem) {
        CartItemDTO dto = new CartItemDTO();
        dto.setId(cartItem.getId());
        dto.setNomProduit(cartItem.getNomProduit());
        dto.setPrix(cartItem.getPrix());
        dto.setImageUrl(cartItem.getImageUrl());
        dto.setQuantite(cartItem.getQuantite());
        dto.setCategorie(cartItem.getCategorie());
        dto.setProductId(cartItem.getProductId());
        return dto;
    }
}