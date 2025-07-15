package com.cartservice.dto;

import com.cartservice.dto.CartItemDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartResponseDTO {
    private String userId;
    private List<CartItemDTO> items;
    private Double subtotal;
}