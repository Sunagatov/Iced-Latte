package com.zufar.icedlatte.review.messaging.kafka.outbox;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.zufar.icedlatte.review.messaging.kafka.event.ReviewCreatedKafkaEvent;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class OutboxEventRepository {

    private final JdbcTemplate jdbcTemplate;

    public void insertReviewCreatedEvent(
            ReviewCreatedKafkaEvent event,
            String topic,
            String partitionKey,
            String payload,
            String headers,
            int maxAttempts) {
        jdbcTemplate.update(
                """
                INSERT INTO outbox_events (
                    event_id, aggregate_type, aggregate_id, event_type, event_version,
                    topic, partition_key, payload, headers, status, max_attempts
                )
                VALUES (?, 'ProductReview', ?, ?, ?, ?, ?, CAST(? AS jsonb), CAST(? AS jsonb), 'PENDING', ?)
                ON CONFLICT (event_id) DO NOTHING
                """,
                event.eventId(),
                event.payload().reviewId(),
                event.eventType(),
                event.eventVersion(),
                topic,
                partitionKey,
                payload,
                headers,
                maxAttempts);
    }

    public List<OutboxEventRow> claimPublishableEvents(int batchSize, String workerId) {
        return jdbcTemplate.query("""
                WITH candidate AS (
                    SELECT id
                    FROM outbox_events
                    WHERE status IN ('PENDING', 'FAILED_RETRYABLE')
                      AND (next_attempt_at IS NULL OR next_attempt_at <= now())
                    ORDER BY created_at
                    LIMIT ?
                    FOR UPDATE SKIP LOCKED
                )
                UPDATE outbox_events o
                SET status = 'IN_PROGRESS',
                    locked_by = ?,
                    locked_at = now(),
                    updated_at = now()
                FROM candidate
                WHERE o.id = candidate.id
                RETURNING o.id, o.event_id, o.topic, o.partition_key, o.payload::text, o.headers::text,
                          o.attempt_count, o.max_attempts
                """, (rs, _) -> mapRow(rs), batchSize, workerId);
    }

    public int reclaimStaleLocks(Instant lockedBefore) {
        return jdbcTemplate.update("""
                UPDATE outbox_events
                SET status = 'FAILED_RETRYABLE',
                    locked_by = NULL,
                    locked_at = NULL,
                    next_attempt_at = now(),
                    updated_at = now()
                WHERE status = 'IN_PROGRESS'
                  AND locked_at < ?
                """, Timestamp.from(lockedBefore));
    }

    public void markPublished(UUID id, String workerId, int partition, long offset) {
        jdbcTemplate.update("""
                UPDATE outbox_events
                SET status = 'PUBLISHED',
                    locked_by = NULL,
                    locked_at = NULL,
                    published_at = now(),
                    kafka_partition = ?,
                    kafka_offset = ?,
                    last_error = NULL,
                    updated_at = now()
                WHERE id = ?
                  AND status = 'IN_PROGRESS'
                  AND locked_by = ?
                """, partition, offset, id, workerId);
    }

    public void markFailed(UUID id, String workerId, int attemptCount, int maxAttempts, Throwable failure) {
        int nextAttemptCount = attemptCount + 1;
        boolean permanent = nextAttemptCount >= maxAttempts;
        Instant nextAttemptAt =
                permanent ? null : Instant.now().plus(backoffSeconds(nextAttemptCount), ChronoUnit.SECONDS);
        jdbcTemplate.update(
                """
                UPDATE outbox_events
                SET status = ?,
                    attempt_count = ?,
                    next_attempt_at = ?,
                    locked_by = NULL,
                    locked_at = NULL,
                    last_error = ?,
                    updated_at = now()
                WHERE id = ?
                  AND status = 'IN_PROGRESS'
                  AND locked_by = ?
                """,
                permanent ? "FAILED_PERMANENT" : "FAILED_RETRYABLE",
                nextAttemptCount,
                nextAttemptAt == null ? null : Timestamp.from(nextAttemptAt),
                sanitizedError(failure),
                id,
                workerId);
    }

    private long backoffSeconds(int attemptCount) {
        return Math.min(1L << Math.min(attemptCount, 8), 300L);
    }

    private String sanitizedError(Throwable failure) {
        String message = failure.getClass().getSimpleName() + ": " + failure.getMessage();
        return message.length() <= 1000 ? message : message.substring(0, 1000);
    }

    private OutboxEventRow mapRow(ResultSet rs) throws SQLException {
        return new OutboxEventRow(
                rs.getObject("id", UUID.class),
                rs.getObject("event_id", UUID.class),
                rs.getString("topic"),
                rs.getString("partition_key"),
                rs.getString("payload"),
                rs.getString("headers"),
                rs.getInt("attempt_count"),
                rs.getInt("max_attempts"));
    }

    public record OutboxEventRow(
            UUID id,
            UUID eventId,
            String topic,
            String partitionKey,
            String payload,
            String headers,
            int attemptCount,
            int maxAttempts) {}
}
