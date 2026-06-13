package com.zufar.icedlatte.supportchat.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "support_messages")
public class SupportMessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "conversation_id", nullable = false)
    private UUID conversationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "sender_type", nullable = false, length = 32)
    private SupportMessageSenderType senderType;

    @Column(name = "sender_user_id")
    private UUID senderUserId;

    @Column(name = "client_message_id")
    private UUID clientMessageId;

    @Column(name = "body", nullable = false, length = 4000)
    private String body;

    @Column(name = "normalized_body", nullable = false, length = 4000)
    private String normalizedBody;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_status", nullable = false, length = 32)
    private SupportMessageDeliveryStatus deliveryStatus = SupportMessageDeliveryStatus.PENDING;

    @Column(name = "operator_inspection_required", nullable = false)
    private boolean operatorInspectionRequired = false;

    @Column(name = "visible_to_customer", nullable = false)
    private boolean visibleToCustomer = true;

    @Column(name = "telegram_update_id")
    private Long telegramUpdateId;

    @Column(name = "telegram_message_id")
    private Long telegramMessageId;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void prePersist() {
        createdAt = OffsetDateTime.now();
    }
}
