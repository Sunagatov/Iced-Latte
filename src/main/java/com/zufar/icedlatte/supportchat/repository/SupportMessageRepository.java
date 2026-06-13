package com.zufar.icedlatte.supportchat.repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.zufar.icedlatte.supportchat.entity.SupportMessageDeliveryStatus;
import com.zufar.icedlatte.supportchat.entity.SupportMessageEntity;
import com.zufar.icedlatte.supportchat.entity.SupportMessageSenderType;

public interface SupportMessageRepository extends JpaRepository<SupportMessageEntity, UUID> {

    Page<SupportMessageEntity> findByConversationIdAndVisibleToCustomerTrueAndCreatedAtAfter(
            UUID conversationId, OffsetDateTime createdAfter, Pageable pageable);

    Optional<SupportMessageEntity> findByConversationIdAndClientMessageId(UUID conversationId, UUID clientMessageId);

    boolean existsByTelegramUpdateId(Long telegramUpdateId);

    long deleteByCreatedAtBefore(OffsetDateTime createdAt);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE SupportMessageEntity message
            SET message.deliveryStatus = :deliveryStatus,
                message.operatorInspectionRequired = :operatorInspectionRequired
            WHERE message.id = :messageId
            """)
    int updateDeliveryStatus(
            @Param("messageId") UUID messageId,
            @Param("deliveryStatus") SupportMessageDeliveryStatus deliveryStatus,
            @Param("operatorInspectionRequired") boolean operatorInspectionRequired);

    Optional<SupportMessageEntity> findFirstByConversationIdAndSenderTypeOrderByCreatedAtDesc(
            UUID conversationId, SupportMessageSenderType senderType);
}
