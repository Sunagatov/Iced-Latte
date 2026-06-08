package com.zufar.icedlatte.supportchat.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.zufar.icedlatte.supportchat.entity.SupportConversationEntity;

public interface SupportConversationRepository extends JpaRepository<SupportConversationEntity, UUID> {

    Optional<SupportConversationEntity> findByUserId(UUID userId);

    Optional<SupportConversationEntity> findByTelegramMessageThreadId(Long telegramMessageThreadId);

    Optional<SupportConversationEntity> findByTelegramFallbackMessageId(Long telegramFallbackMessageId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
                    INSERT INTO public.support_conversations (id, user_id, created_at, updated_at)
                    VALUES (:id, :userId, current_timestamp, current_timestamp)
                    ON CONFLICT (user_id) DO NOTHING
                    """, nativeQuery = true)
    void insertOpenConversationIfAbsent(@Param("id") UUID id, @Param("userId") UUID userId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
                    UPDATE public.support_conversations
                    SET last_message_at = current_timestamp, updated_at = current_timestamp
                    WHERE id = :conversationId
                    """, nativeQuery = true)
    void touchLastMessageAt(@Param("conversationId") UUID conversationId);
}
