package com.servicepaiement.dto;

import com.servicepaiement.model.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequestDTO {
    private String userId;
    private List<CartItemDTO> items;
    private ShippingInfoDTO shipping;
    private PaymentMethod paymentMethod;
    private Double subtotal;
    private Double shippingFee;
    private Double total;
}