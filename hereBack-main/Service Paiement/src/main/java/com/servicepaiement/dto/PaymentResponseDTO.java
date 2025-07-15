package com.servicepaiement.dto;

import com.servicepaiement.model.PaymentMethod;
import com.servicepaiement.model.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponseDTO {
    private String paymentId;
    private String orderNumber;
    private PaymentStatus status;
    private PaymentMethod method;
    private Double amount;
    private String currency;
    private LocalDateTime createdAt;
    private String redirectUrl; // Pour les redirections PayPal
    private String message;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CodPaymentRequestDTO {
        private PaymentRequestDTO paymentRequest;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaypalPaymentRequestDTO {
        private PaymentRequestDTO paymentRequest;
    }
}
