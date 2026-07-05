package com.zufar.icedlatte.user.service;

import java.time.Duration;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.zufar.icedlatte.common.monitoring.SentryJobMonitor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "avatar.retention-cleanup-enabled", havingValue = "true", matchIfMissing = true)
class AvatarUploadRetentionCleanupJob {

    private static final String MONITOR_SLUG = "avatar-upload-retention-cleanup";

    private final AvatarUploadLifecycleService lifecycleService;
    private final SentryJobMonitor sentryJobMonitor;

    @Value("${avatar.retention-cleanup-interval:PT30M}")
    private Duration retentionCleanupInterval;

    @Scheduled(fixedDelayString = "${avatar.retention-cleanup-interval:PT30M}")
    @Transactional
    public void cleanupExpiredUploads() {
        sentryJobMonitor.run(
                MONITOR_SLUG,
                sentryJobMonitor.fixedDelayConfig(retentionCleanupInterval.toMillis()),
                this::cleanupExpiredUploadsInternal);
    }

    long cleanupExpiredUploadsInternal() {
        long expiredCount = lifecycleService.expireStaleUploads(Instant.now());
        if (expiredCount > 0) {
            log.info("avatar.upload_retention.cleanup_completed: expiredCount={}", expiredCount);
        }
        return expiredCount;
    }
}
