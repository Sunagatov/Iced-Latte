package com.zufar.icedlatte.filestorage.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zufar.icedlatte.filestorage.api.dto.FileMetadataDto;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class FileDeletionOutboxRepository {

    public static final String EVENT_TYPE = "file.object.delete";
    private static final String AGGREGATE_TYPE = "FileObjectDeletion";
    private static final int EVENT_VERSION = 1;
    private static final String INTERNAL_TOPIC = "internal.file.object.delete";
    private static final String EMPTY_HEADERS = "{}";

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public void insertDeleteObjectEvent(FileMetadataDto metadata, int maxAttempts) {
        UUID deletionId = UUID.randomUUID();
        jdbcTemplate.update(
                """
                INSERT INTO outbox_events (
                    event_id, aggregate_type, aggregate_id, event_type, event_version,
                    topic, partition_key, payload, headers, status, max_attempts
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, CAST(? AS jsonb), CAST(? AS jsonb), 'PENDING', ?)
                ON CONFLICT (event_id) DO NOTHING
                """,
                deletionId,
                AGGREGATE_TYPE,
                deletionId,
                EVENT_TYPE,
                EVENT_VERSION,
                INTERNAL_TOPIC,
                metadata.relatedObjectId().toString(),
                toPayload(metadata),
                EMPTY_HEADERS,
                maxAttempts);
    }

    public List<FileDeletionOutboxRow> claimDeleteObjectEvents(int batchSize, String workerId) {
        return jdbcTemplate.query(
                """
                WITH candidate AS (
                    SELECT id
                    FROM outbox_events
                    WHERE event_type = ?
                      AND status IN ('PENDING', 'FAILED_RETRYABLE')
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
                RETURNING o.id, o.event_id, o.payload::text, o.attempt_count, o.max_attempts
                """,
                (rs, _) -> mapRow(rs),
                EVENT_TYPE,
                batchSize,
                workerId);
    }

    public int reclaimStaleLocks(Instant lockedBefore) {
        return jdbcTemplate.update(
                """
                UPDATE outbox_events
                SET status = 'FAILED_RETRYABLE',
                    locked_by = NULL,
                    locked_at = NULL,
                    next_attempt_at = now(),
                    updated_at = now()
                WHERE event_type = ?
                  AND status = 'IN_PROGRESS'
                  AND locked_at < ?
                """,
                EVENT_TYPE,
                Timestamp.from(lockedBefore));
    }

    public void markDeleted(UUID id, String workerId) {
        jdbcTemplate.update(
                """
                UPDATE outbox_events
                SET status = 'PUBLISHED',
                    locked_by = NULL,
                    locked_at = NULL,
                    published_at = now(),
                    last_error = NULL,
                    updated_at = now()
                WHERE id = ?
                  AND event_type = ?
                  AND status = 'IN_PROGRESS'
                  AND locked_by = ?
                """,
                id,
                EVENT_TYPE,
                workerId);
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
                  AND event_type = ?
                  AND status = 'IN_PROGRESS'
                  AND locked_by = ?
                """,
                permanent ? "FAILED_PERMANENT" : "FAILED_RETRYABLE",
                nextAttemptCount,
                nextAttemptAt == null ? null : Timestamp.from(nextAttemptAt),
                sanitizedError(failure),
                id,
                EVENT_TYPE,
                workerId);
    }

    private String toPayload(FileMetadataDto metadata) {
        try {
            return objectMapper.writeValueAsString(new FileObjectDeletionPayload(
                    metadata.relatedObjectId(), metadata.bucketName(), metadata.fileName()));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Cannot serialize file object deletion outbox payload", ex);
        }
    }

    private long backoffSeconds(int attemptCount) {
        return Math.min(1L << Math.min(attemptCount, 8), 300L);
    }

    private String sanitizedError(Throwable failure) {
        String message = failure.getClass().getSimpleName() + ": " + failure.getMessage();
        return message.length() <= 1000 ? message : message.substring(0, 1000);
    }

    private FileDeletionOutboxRow mapRow(ResultSet rs) throws SQLException {
        return new FileDeletionOutboxRow(
                rs.getObject("id", UUID.class),
                rs.getObject("event_id", UUID.class),
                rs.getString("payload"),
                rs.getInt("attempt_count"),
                rs.getInt("max_attempts"));
    }

    public record FileDeletionOutboxRow(UUID id, UUID eventId, String payload, int attemptCount, int maxAttempts) {}

    public record FileObjectDeletionPayload(UUID relatedObjectId, String bucketName, String fileName) {}
}
