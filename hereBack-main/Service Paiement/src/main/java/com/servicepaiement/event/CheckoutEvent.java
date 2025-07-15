package com.servicepaiement.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutEvent implements Serializable {
    private Long cartId;
    private String userId;
    private String userEmail;
    private Double total;
    private LocalDateTime timestamp;
}