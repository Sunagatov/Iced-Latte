package com.zufar.icedlatte.order.repository;

import com.zufar.icedlatte.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID>, JpaSpecificationExecutor<Order> {

    Optional<Order> findByUserIdAndSessionId(UUID userId, String sessionId);

    Optional<Order> findByIdempotencyKeyAndUserId(String idempotencyKey, UUID userId);

    Optional<Order> findByStripePaymentIntentId(String stripePaymentIntentId);
}
