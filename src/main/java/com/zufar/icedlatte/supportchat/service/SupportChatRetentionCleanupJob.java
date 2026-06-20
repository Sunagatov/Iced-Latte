package com.zufar.icedlatte.supportchat.service;

import java.time.Duration;
import java.time.OffsetDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.zufar.icedlatte.common.monitoring.SentryJobMonitor;
import com.zufar.icedlatte.supportchat.config.SupportChatProperties;
import com.zufar.icedlatte.supportchat.repository.SupportMessageRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "support-chat.retention-cleanup-enabled", havingValue = "true", matchIfMissing = true)
class SupportChatRetentionCleanupJob {

    private static final String MONITOR_SLUG = "support-chat-retention-cleanup";

    private final SupportChatProperties properties;
    private final SupportMessageRepository messageRepository;
    private final SentryJobMonitor sentryJobMonitor;

    @Value("${support-chat.retention-cleanup-interval:PT24H}")
    private Duration retentionCleanupInterval;

    @Scheduled(fixedDelayString = "${support-chat.retention-cleanup-interval:PT24H}")
    @Transactional
    public void cleanupExpiredMessages() {
        sentryJobMonitor.run(
                MONITOR_SLUG,
                sentryJobMonitor.fixedDelayConfig(retentionCleanupInterval.toMillis()),
                this::cleanupExpiredMessagesInternal);
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
