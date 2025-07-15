package com.servicepaiement.event;

import com.servicepaiement.model.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderEvent implements Serializable {
    private Long orderId;
    private String orderNumber;
    private String userId;
    private OrderStatus status;
    private Double amount;
    private LocalDateTime timestamp;
}