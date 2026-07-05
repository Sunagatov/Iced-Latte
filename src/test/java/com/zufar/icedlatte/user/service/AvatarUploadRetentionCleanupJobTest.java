package com.zufar.icedlatte.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.zufar.icedlatte.common.monitoring.SentryJobMonitor;

@DisplayName("AvatarUploadRetentionCleanupJob unit tests")
class AvatarUploadRetentionCleanupJobTest {

    private final AvatarUploadLifecycleService lifecycleService = mock(AvatarUploadLifecycleService.class);
    private final SentryJobMonitor sentryJobMonitor = mock(SentryJobMonitor.class);

    @Test
    @DisplayName("Expires stale uploads before now")
    void cleanupExpiredUploadsInternal_expiresStaleUploads() {
        when(lifecycleService.expireStaleUploads(any())).thenReturn(2L);
        AvatarUploadRetentionCleanupJob job = new AvatarUploadRetentionCleanupJob(lifecycleService, sentryJobMonitor);

        long expiredCount = job.cleanupExpiredUploadsInternal();

        assertThat(expiredCount).isEqualTo(2L);
        ArgumentCaptor<Instant> cutoffCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(lifecycleService).expireStaleUploads(cutoffCaptor.capture());
        assertThat(cutoffCaptor.getValue()).isBefore(Instant.now().plusSeconds(1));
        assertThat(cutoffCaptor.getValue()).isAfter(Instant.now().minusSeconds(5));
    }

    @Test
    @DisplayName("Scheduled cleanup runs through Sentry monitor with the configured interval")
    void cleanupExpiredUploads_runsThroughSentryMonitor() {
        when(lifecycleService.expireStaleUploads(any())).thenReturn(1L);
        doAnswer(invocation -> {
                    ((Runnable) invocation.getArgument(2)).run();
                    return null;
                })
                .when(sentryJobMonitor)
                .run(eq("avatar-upload-retention-cleanup"), any(), any(Runnable.class));
        AvatarUploadRetentionCleanupJob job = new AvatarUploadRetentionCleanupJob(lifecycleService, sentryJobMonitor);
        org.springframework.test.util.ReflectionTestUtils.setField(
                job, "retentionCleanupInterval", Duration.ofMinutes(30));

        job.cleanupExpiredUploads();

        verify(sentryJobMonitor).run(eq("avatar-upload-retention-cleanup"), any(), any(Runnable.class));
        verify(lifecycleService).expireStaleUploads(any());
    }
}
