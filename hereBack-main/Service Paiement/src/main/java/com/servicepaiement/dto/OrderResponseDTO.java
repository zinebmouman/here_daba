package com.servicepaiement.dto;

import com.servicepaiement.model.OrderStatus;
import com.servicepaiement.model.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponseDTO {
    private String orderNumber;
    private OrderStatus status;
    private LocalDateTime createdAt;
    private ShippingInfoDTO shipping;
    private PaymentMethod paymentMethod;
    private String paymentStatus;
    private List<CartItemDTO> items; // Utilisez la classe CartItemDTO standard
    private Double subtotal;
    private Double shippingFee;
    private Double total;
}