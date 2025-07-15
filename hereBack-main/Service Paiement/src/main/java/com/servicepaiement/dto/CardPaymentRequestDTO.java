package com.servicepaiement.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CardPaymentRequestDTO {
    private PaymentRequestDTO paymentRequest;
    private String cardNumber;
    private String cardHolder;
    private String expiryDate;
    private String cvv;
    private String token; // Token Stripe
}