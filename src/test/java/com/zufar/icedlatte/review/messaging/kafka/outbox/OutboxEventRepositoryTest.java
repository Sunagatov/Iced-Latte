package com.zufar.icedlatte.review.messaging.kafka.outbox;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
@DisplayName("OutboxEventRepository")
class OutboxEventRepositoryTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("claims only review created events from shared outbox table")
    void claimsOnlyReviewCreatedEventsFromSharedOutboxTable() {
        var repository = new OutboxEventRepository(jdbcTemplate);

        repository.claimPublishableEvents(25, "worker-1");

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate)
                .query(
                        sql.capture(),
                        any(org.springframework.jdbc.core.RowMapper.class),
                        eq("review.created"),
                        eq(25),
                        eq("worker-1"));
        Assertions.assertThat(sql.getValue()).contains("WHERE event_type = ?");
    }

    @Test
    @DisplayName("reclaims only review created stale locks")
    void reclaimsOnlyReviewCreatedStaleLocks() {
        var repository = new OutboxEventRepository(jdbcTemplate);
        Instant lockedBefore = Instant.parse("2026-05-29T10:00:00Z");

        repository.reclaimStaleLocks(lockedBefore);

        verify(jdbcTemplate).update(anyString(), eq("review.created"), eq(Timestamp.from(lockedBefore)));
    }

    @Test
    @DisplayName("marks failed publish attempts as retryable before max attempts")
    void marksFailedPublishAttemptsAsRetryableBeforeMaxAttempts() {
        UUID rowId = UUID.randomUUID();
        var repository = new OutboxEventRepository(jdbcTemplate);
        IllegalStateException failure = new IllegalStateException("boom");

        repository.markFailed(rowId, "worker-1", 2, 10, failure);

        verify(jdbcTemplate)
                .update(
                        anyString(),
                        eq("FAILED_RETRYABLE"),
                        eq(3),
                        any(Timestamp.class),
                        eq("IllegalStateException: boom"),
                        eq(rowId),
                        eq("review.created"),
                        eq("worker-1"));
    }

    @Test
    @DisplayName("marks failed publish attempts as permanent at max attempts")
    void marksFailedPublishAttemptsAsPermanentAtMaxAttempts() {
        UUID rowId = UUID.randomUUID();
        var repository = new OutboxEventRepository(jdbcTemplate);
        IllegalStateException failure = new IllegalStateException("boom");

        repository.markFailed(rowId, "worker-1", 9, 10, failure);

        verify(jdbcTemplate)
                .update(
                        anyString(),
                        eq("FAILED_PERMANENT"),
                        eq(10),
                        isNull(),
                        eq("IllegalStateException: boom"),
                        eq(rowId),
                        eq("review.created"),
                        eq("worker-1"));
    }
}
