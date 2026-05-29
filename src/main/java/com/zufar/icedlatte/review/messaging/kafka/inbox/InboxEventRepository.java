package com.zufar.icedlatte.review.messaging.kafka.inbox;

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
public class InboxEventRepository {

    private final JdbcTemplate jdbcTemplate;

    public boolean insertReceivedEvent(
            ReviewCreatedKafkaEvent event,
            String topic,
            String partitionKey,
            int kafkaPartition,
            long kafkaOffset,
            String consumerName,
            String payload,
            String headers,
            int maxAttempts) {
        int inserted = jdbcTemplate.update(
                """
                        INSERT INTO inbox_events (
                            event_id, event_type, event_version, topic, partition_key,
                            kafka_partition, kafka_offset, consumer_name, payload, headers,
                            status, max_attempts
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS jsonb), CAST(? AS jsonb), 'RECEIVED', ?)
                        ON CONFLICT DO NOTHING
                        """,
                event.eventId(),
                event.eventType(),
                event.eventVersion(),
                topic,
                partitionKey,
                kafkaPartition,
                kafkaOffset,
                consumerName,
                payload,
                headers,
                maxAttempts);
        return inserted == 1;
    }

    public List<InboxEventRow> claimProcessableEvents(
            int batchSize, String consumerName, String eventType, String workerId) {
        return jdbcTemplate.query("""
                        WITH candidate AS (
                            SELECT id
                            FROM inbox_events
                            WHERE status IN ('RECEIVED', 'FAILED_RETRYABLE')
                              AND (next_attempt_at IS NULL OR next_attempt_at <= now())
                              AND consumer_name = ?
                              AND event_type = ?
                            ORDER BY created_at
                            LIMIT ?
                            FOR UPDATE SKIP LOCKED
                        )
                        UPDATE inbox_events i
                        SET status = 'IN_PROGRESS',
                            locked_by = ?,
                            locked_at = now(),
                            updated_at = now()
                        FROM candidate
                        WHERE i.id = candidate.id
                        RETURNING i.id, i.event_id, i.payload::text, i.attempt_count, i.max_attempts
                        """, (rs, _) -> mapRow(rs), consumerName, eventType, batchSize, workerId);
    }

    public int reclaimStaleLocks(Instant lockedBefore, String consumerName, String eventType) {
        return jdbcTemplate.update("""
                        UPDATE inbox_events
                        SET status = 'FAILED_RETRYABLE',
                            locked_by = NULL,
                            locked_at = NULL,
                            next_attempt_at = now(),
                            updated_at = now()
                        WHERE status = 'IN_PROGRESS'
                          AND consumer_name = ?
                          AND event_type = ?
                          AND locked_at < ?
                        """, consumerName, eventType, Timestamp.from(lockedBefore));
    }

    public void markProcessed(UUID id, String workerId) {
        markTerminal(id, workerId, "PROCESSED");
    }

    public void markIgnored(UUID id, String workerId) {
        markTerminal(id, workerId, "IGNORED");
    }

    public void markFailed(UUID id, String workerId, int attemptCount, int maxAttempts, Throwable failure) {
        int nextAttemptCount = attemptCount + 1;
        boolean permanent = nextAttemptCount >= maxAttempts;
        String status = permanent ? "FAILED_PERMANENT" : "FAILED_RETRYABLE";
        Timestamp nextAttemptAt;
        if (permanent) {
            nextAttemptAt = null;
        } else {
            nextAttemptAt = Timestamp.from(Instant.now().plus(backoffSeconds(nextAttemptCount), ChronoUnit.SECONDS));
        }
        jdbcTemplate.update("""
                        UPDATE inbox_events
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
                        """, status, nextAttemptCount, nextAttemptAt, sanitizedError(failure), id, workerId);
    }

    private void markTerminal(UUID id, String workerId, String status) {
        jdbcTemplate.update("""
                        UPDATE inbox_events
                        SET status = ?,
                            locked_by = NULL,
                            locked_at = NULL,
                            processed_at = now(),
                            last_error = NULL,
                            updated_at = now()
                        WHERE id = ?
                          AND status = 'IN_PROGRESS'
                          AND locked_by = ?
                        """, status, id, workerId);
    }

    private long backoffSeconds(int attemptCount) {
        return Math.min(1L << Math.min(attemptCount, 8), 300L);
    }

    private String sanitizedError(Throwable failure) {
        String message = failure.getClass().getSimpleName() + ": " + failure.getMessage();
        return message.length() <= 1000 ? message : message.substring(0, 1000);
    }

    private InboxEventRow mapRow(ResultSet rs) throws SQLException {
        return new InboxEventRow(
                rs.getObject("id", UUID.class),
                rs.getObject("event_id", UUID.class),
                rs.getString("payload"),
                rs.getInt("attempt_count"),
                rs.getInt("max_attempts"));
    }

    public record InboxEventRow(UUID id, UUID eventId, String payload, int attemptCount, int maxAttempts) {}
}
