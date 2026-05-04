package com.zufar.icedlatte.payment.repository;

import com.zufar.icedlatte.payment.entity.Payment;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByOrderId(UUID orderId);

    Optional<Payment> findByCheckoutIdempotencyKeyAndUserId(String checkoutIdempotencyKey, UUID userId);

    /**
     * Row-level lock to prevent concurrent webhook processing for the same order.
     * Different Stripe events (e.g., completed + async_payment_succeeded) can target
     * the same order with different event IDs — event dedup alone is not enough.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payment p WHERE p.orderId = :orderId")
    Optional<Payment> findByOrderIdForUpdate(UUID orderId);
}
