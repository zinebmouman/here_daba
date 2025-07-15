package com.servicepaiement.repository;

import com.servicepaiement.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByPaymentId(String paymentId);

    List<Payment> findByUserId(String userId);

    Optional<Payment> findByOrderId(Long orderId);
}
