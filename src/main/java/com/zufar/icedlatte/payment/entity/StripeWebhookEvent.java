package com.zufar.icedlatte.payment.entity;

import java.time.OffsetDateTime;

import jakarta.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Deduplicates Stripe webhook events using insert-first pattern. The stripe_event_id is the natural primary key
 * (assigned by Stripe).
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "stripe_webhook_events")
@SuppressWarnings("unused") // JPA reads and writes entity fields reflectively.
public class StripeWebhookEvent {

    @Id
    @Column(name = "stripe_event_id", nullable = false)
    private String stripeEventId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private WebhookEventStatus status;

    @Column(name = "received_at", nullable = false)
    private OffsetDateTime receivedAt;

    @Column(name = "processed_at")
    private OffsetDateTime processedAt;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;
}
