package com.servicepaiement.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CodPaymentRequestDTO {
    private PaymentRequestDTO paymentRequest;
}