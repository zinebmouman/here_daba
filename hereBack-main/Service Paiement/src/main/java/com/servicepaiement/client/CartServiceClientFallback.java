package com.servicepaiement.client;

import com.servicepaiement.client.CartServiceClient;
import com.servicepaiement.dto.CartItemDTO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class CartServiceClientFallback implements CartServiceClient {

    @Override
    public List<CartItemDTO> getCartItemsByUserId(String userId) {
        // Retourner une liste vide ou des données par défaut en cas de panne
        System.err.println("⚠️ Fallback activé pour getCartItemsByUserId: " + userId);
        return new ArrayList<>();
    }
}