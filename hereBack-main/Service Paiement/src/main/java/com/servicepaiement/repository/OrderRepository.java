package com.servicepaiement.repository;

import com.servicepaiement.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByOrderNumber(String orderNumber);

    List<Order> findByUserId(String userId);

    List<Order> findByUserIdOrderByCreatedAtDesc(String userId);
}