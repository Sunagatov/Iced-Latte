package com.zufar.icedlatte.review.messaging.kafka.inbox;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

@ExtendWith(MockitoExtension.class)
@DisplayName("InboxEventRepository")
class InboxEventRepositoryTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("claims only rows for the requested consumer")
    void claimsOnlyRowsForRequestedConsumer() {
        when(jdbcTemplate.query(anyString(), anyInboxRowMapper(), any(), any(), any()))
                .thenReturn(List.of());
        var repository = new InboxEventRepository(jdbcTemplate);

        repository.claimProcessableEvents(25, "iced-latte-review-ai", "worker-1");

        verify(jdbcTemplate)
                .query(
                        contains("AND consumer_name = ?"),
                        anyInboxRowMapper(),
                        eq("iced-latte-review-ai"),
                        eq(25),
                        eq("worker-1"));
    }

    private RowMapper<InboxEventRepository.InboxEventRow> anyInboxRowMapper() {
        return any();
    }

    @Test
    @DisplayName("marks failed processing attempts as retryable before max attempts")
    void marksFailedProcessingAttemptsAsRetryableBeforeMaxAttempts() {
        UUID rowId = UUID.randomUUID();
        var repository = new InboxEventRepository(jdbcTemplate);
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
                        eq("worker-1"));
    }

    @Test
    @DisplayName("marks failed processing attempts as permanent at max attempts")
    void marksFailedProcessingAttemptsAsPermanentAtMaxAttempts() {
        UUID rowId = UUID.randomUUID();
        var repository = new InboxEventRepository(jdbcTemplate);
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
                        eq("worker-1"));
    }
}
