package com.servicepaiement.client;

import com.servicepaiement.dto.CartItemDTO;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;
@FeignClient(
        name = "CART-SERVICE",
        fallback = CartServiceClientFallback.class
)

public interface CartServiceClient {

    @GetMapping("/api/cart/items")
    List<CartItemDTO> getCartItemsByUserId(@RequestHeader("X-User-Id") String userId);
}