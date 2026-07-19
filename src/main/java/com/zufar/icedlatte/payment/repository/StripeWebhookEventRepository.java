package com.zufar.icedlatte.payment.repository;

import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.zufar.icedlatte.payment.entity.StripeWebhookEvent;

@SuppressWarnings("unused") // Spring Data generates implementations for repository methods.
public interface StripeWebhookEventRepository extends JpaRepository<StripeWebhookEvent, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select event from StripeWebhookEvent event where event.stripeEventId = :eventId")
    Optional<StripeWebhookEvent> findByIdForUpdate(@Param("eventId") String eventId);
}
