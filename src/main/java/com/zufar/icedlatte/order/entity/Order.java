package com.zufar.icedlatte.order.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.*;

import org.hibernate.annotations.BatchSize;

import com.zufar.icedlatte.common.audit.AuditableEntity;
import com.zufar.icedlatte.openapi.dto.OrderStatus;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "orders")
@SuppressWarnings("unused") // JPA reads and writes entity fields reflectively.
public class Order extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @ToString.Include
    private UUID id;

    @Column(name = "user_id", updatable = false, nullable = false)
    private UUID userId;

    @Column(name = "session_id", updatable = false, nullable = false)
    private String sessionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 55)
    private OrderStatus status;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version;

    @Column(name = "idempotency_key", length = 64)
    private String idempotencyKey;

    @OneToMany(mappedBy = "orderId", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 50)
    private List<OrderItem> items;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "address_id", referencedColumnName = "id", nullable = false)
    private OrderAddress deliveryAddress;

    @Column(name = "recipient_name", nullable = false, length = 128)
    private String recipientName;

    @Column(name = "recipient_surname", nullable = false, length = 128)
    private String recipientSurname;

    @Column(name = "recipient_phone", length = 32)
    private String recipientPhone;

    @Column(name = "items_quantity", nullable = false)
    private Integer itemsQuantity;

    @Column(name = "items_total_price", nullable = false)
    private BigDecimal itemsTotalPrice;

    @Column(name = "cancellation_deadline")
    private OffsetDateTime cancellationDeadline;

    @Column(name = "stripe_payment_intent_id")
    private String stripePaymentIntentId;

    @Column(name = "refund_reason", length = 500)
    private String refundReason;

    @Column(name = "refunded_at")
    private OffsetDateTime refundedAt;

    @PrePersist
    public void prePersist() {
        if (items == null) {
            return;
        }
        for (OrderItem orderItem : items) {
            orderItem.setOrderId(this.id);
        }
    }
}
