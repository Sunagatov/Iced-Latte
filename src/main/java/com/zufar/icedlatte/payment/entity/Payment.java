package com.zufar.icedlatte.payment.entity;

import com.zufar.icedlatte.common.audit.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Tracks Stripe payment details, separate from the Order entity.
 * Iced Latte uses Stripe test mode only — no real money is charged.
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "payments")
@SuppressWarnings("unused") // JPA reads and writes entity fields reflectively.
public class Payment extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "order_id", nullable = false, unique = true)
    private UUID orderId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false)
    private PaymentProvider provider;

    @Column(name = "provider_session_id", unique = true)
    private String providerSessionId;

    @Column(name = "provider_payment_intent_id")
    private String providerPaymentIntentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PaymentStatus status;

    @Column(name = "amount_minor", nullable = false)
    private Long amountMinor;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "raw_event_id")
    private String rawEventId;

    @Column(name = "latest_event_type")
    private String latestEventType;

    @Column(name = "checkout_idempotency_key", length = 100)
    private String checkoutIdempotencyKey;

    @Column(name = "checkout_request_hash", length = 64)
    private String checkoutRequestHash;

    @Override
    public String toString() {
        return "Payment{id=" + id + ", orderId=" + orderId + ", status=" + status + '}';
    }
}
