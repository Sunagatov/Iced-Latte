package com.zufar.icedlatte.supportchat.service;

import java.time.OffsetDateTime;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.zufar.icedlatte.supportchat.config.SupportChatProperties;
import com.zufar.icedlatte.supportchat.repository.SupportMessageRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "support-chat.retention-cleanup-enabled", havingValue = "true", matchIfMissing = true)
class SupportChatRetentionCleanupJob {

    private final SupportChatProperties properties;
    private final SupportMessageRepository messageRepository;

    @Scheduled(fixedDelayString = "${support-chat.retention-cleanup-interval:PT24H}")
    @Transactional
    public void cleanupExpiredMessages() {
        cleanupExpiredMessagesInternal();
    }

    long cleanupExpiredMessagesInternal() {
        OffsetDateTime cutoff = OffsetDateTime.now().minusDays(properties.retentionDays());
        long deletedCount = messageRepository.deleteByCreatedAtBefore(cutoff);
        if (deletedCount > 0) {
            log.info("support_chat.retention.cleanup_completed: deletedCount={}", deletedCount);
        }
        return deletedCount;
    }
}
