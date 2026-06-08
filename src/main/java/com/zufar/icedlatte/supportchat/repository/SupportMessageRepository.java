package com.zufar.icedlatte.supportchat.repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.zufar.icedlatte.supportchat.entity.SupportMessageEntity;
import com.zufar.icedlatte.supportchat.entity.SupportMessageSenderType;

public interface SupportMessageRepository extends JpaRepository<SupportMessageEntity, UUID> {

    Page<SupportMessageEntity> findByConversationIdAndVisibleToCustomerTrueAndCreatedAtAfter(
            UUID conversationId, OffsetDateTime createdAfter, Pageable pageable);

    Optional<SupportMessageEntity> findByConversationIdAndClientMessageId(UUID conversationId, UUID clientMessageId);

    boolean existsByTelegramUpdateId(Long telegramUpdateId);

    Optional<SupportMessageEntity> findFirstByConversationIdAndSenderTypeOrderByCreatedAtDesc(
            UUID conversationId, SupportMessageSenderType senderType);
}
