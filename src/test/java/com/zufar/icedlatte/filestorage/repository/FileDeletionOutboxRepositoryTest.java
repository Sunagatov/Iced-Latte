package com.zufar.icedlatte.filestorage.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;

import java.sql.Timestamp;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zufar.icedlatte.filestorage.api.dto.FileMetadataDto;

@ExtendWith(MockitoExtension.class)
@DisplayName("FileDeletionOutboxRepository")
class FileDeletionOutboxRepositoryTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private FileDeletionOutboxRepository repository;

    @BeforeEach
    void setUp() {
        repository = new FileDeletionOutboxRepository(jdbcTemplate, new ObjectMapper());
    }

    @Test
    @DisplayName("inserts delete event into shared outbox table")
    void insertsDeleteEventIntoSharedOutboxTable() {
        UUID relatedObjectId = UUID.randomUUID();
        FileMetadataDto metadata = new FileMetadataDto(relatedObjectId, "bucket", "key");

        repository.insertDeleteObjectEvent(metadata, 10);

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate)
                .update(
                        sql.capture(),
                        any(UUID.class),
                        eq("FileObjectDeletion"),
                        any(UUID.class),
                        eq(FileDeletionOutboxRepository.EVENT_TYPE),
                        eq(1),
                        eq("internal.file.object.delete"),
                        eq(relatedObjectId.toString()),
                        anyString(),
                        eq("{}"),
                        eq(10));
        assertThat(sql.getValue()).contains("INSERT INTO outbox_events");
        assertThat(sql.getValue()).contains("ON CONFLICT (event_id)");
    }

    @Test
    @DisplayName("claims only file deletion events")
    void claimsOnlyFileDeletionEvents() {
        repository.claimDeleteObjectEvents(25, "worker-1");

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).query(sql.capture(), any(org.springframework.jdbc.core.RowMapper.class), eq("file.object.delete"), eq(25), eq("worker-1"));
        assertThat(sql.getValue()).contains("FROM outbox_events");
        assertThat(sql.getValue()).contains("WHERE event_type = ?");
    }

    @Test
    @DisplayName("marks failed delete attempts as retryable before max attempts")
    void marksFailedDeleteAttemptsAsRetryableBeforeMaxAttempts() {
        UUID rowId = UUID.randomUUID();
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
                        eq("file.object.delete"),
                        eq("worker-1"));
    }

    @Test
    @DisplayName("marks failed delete attempts as permanent at max attempts")
    void marksFailedDeleteAttemptsAsPermanentAtMaxAttempts() {
        UUID rowId = UUID.randomUUID();
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
                        eq("file.object.delete"),
                        eq("worker-1"));
    }
}
