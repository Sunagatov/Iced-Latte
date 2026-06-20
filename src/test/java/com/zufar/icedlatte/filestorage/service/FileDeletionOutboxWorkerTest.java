package com.zufar.icedlatte.filestorage.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zufar.icedlatte.common.monitoring.SentryJobMonitor;
import com.zufar.icedlatte.filestorage.api.dto.FileMetadataDto;
import com.zufar.icedlatte.filestorage.config.FileDeletionOutboxProperties;
import com.zufar.icedlatte.filestorage.repository.FileDeletionOutboxRepository;
import com.zufar.icedlatte.filestorage.repository.FileDeletionOutboxRepository.FileDeletionOutboxRow;
import com.zufar.icedlatte.filestorage.repository.FileDeletionOutboxRepository.FileObjectDeletionPayload;

@ExtendWith(MockitoExtension.class)
@DisplayName("FileDeletionOutboxWorker")
class FileDeletionOutboxWorkerTest {

    private static final String WORKER_ID = "worker-1";

    @Mock
    private FileDeletionOutboxRepository outboxRepository;

    @Mock
    private ObjectStorage objectStorage;

    @Mock
    private SentryJobMonitor sentryJobMonitor;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        doAnswer(invocation -> {
                    ((Runnable) invocation.getArgument(2)).run();
                    return null;
                })
                .when(sentryJobMonitor)
                .run(any(), any(), any(Runnable.class));
    }

    @Test
    @DisplayName("deletes claimed object and marks event deleted")
    void deletesClaimedObjectAndMarksEventDeleted() throws JsonProcessingException {
        FileDeletionOutboxProperties properties = properties(true);
        UUID relatedObjectId = UUID.randomUUID();
        UUID rowId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        FileDeletionOutboxRow row = new FileDeletionOutboxRow(
                rowId,
                eventId,
                objectMapper.writeValueAsString(new FileObjectDeletionPayload(relatedObjectId, "bucket", "key")),
                0,
                10);
        when(objectStorage.isConfigured()).thenReturn(true);
        when(outboxRepository.claimDeleteObjectEvents(25, WORKER_ID)).thenReturn(List.of(row));
        when(outboxRepository.markDeleted(rowId, WORKER_ID)).thenReturn(true);

        new FileDeletionOutboxWorker(outboxRepository, properties, objectMapper, objectStorage, sentryJobMonitor)
                .deletePendingObjects();

        verify(outboxRepository).reclaimStaleLocks(org.mockito.ArgumentMatchers.any());
        verify(objectStorage).delete(new FileMetadataDto(relatedObjectId, "bucket", "key"));
        verify(outboxRepository).markDeleted(rowId, WORKER_ID);
        verify(sentryJobMonitor).run(eq("file-deletion-outbox-worker"), any(), any(Runnable.class));
    }

    @Test
    @DisplayName("marks event failed when object deletion fails")
    void marksEventFailedWhenObjectDeletionFails() throws JsonProcessingException {
        FileDeletionOutboxProperties properties = properties(true);
        UUID relatedObjectId = UUID.randomUUID();
        UUID rowId = UUID.randomUUID();
        FileDeletionOutboxRow row = new FileDeletionOutboxRow(
                rowId,
                UUID.randomUUID(),
                objectMapper.writeValueAsString(new FileObjectDeletionPayload(relatedObjectId, "bucket", "key")),
                2,
                10);
        RuntimeException failure = new IllegalStateException("storage unavailable");
        when(objectStorage.isConfigured()).thenReturn(true);
        when(outboxRepository.claimDeleteObjectEvents(25, WORKER_ID)).thenReturn(List.of(row));
        when(outboxRepository.markFailed(rowId, WORKER_ID, 2, 10, failure)).thenReturn(true);
        org.mockito.Mockito.doThrow(failure)
                .when(objectStorage)
                .delete(new FileMetadataDto(relatedObjectId, "bucket", "key"));

        new FileDeletionOutboxWorker(outboxRepository, properties, objectMapper, objectStorage, sentryJobMonitor)
                .deletePendingObjects();

        verify(outboxRepository).markFailed(rowId, WORKER_ID, 2, 10, failure);
    }

    @Test
    @DisplayName("does nothing when worker is disabled")
    void doesNothingWhenWorkerIsDisabled() {
        FileDeletionOutboxProperties properties = properties(false);

        new FileDeletionOutboxWorker(outboxRepository, properties, objectMapper, objectStorage, sentryJobMonitor)
                .deletePendingObjects();

        verifyNoInteractions(outboxRepository, objectStorage);
    }

    @Test
    @DisplayName("does not claim rows when object storage is not configured")
    void doesNotClaimRowsWhenObjectStorageIsNotConfigured() {
        FileDeletionOutboxProperties properties = properties(true);
        when(objectStorage.isConfigured()).thenReturn(false);

        new FileDeletionOutboxWorker(outboxRepository, properties, objectMapper, objectStorage, sentryJobMonitor)
                .deletePendingObjects();

        verifyNoInteractions(outboxRepository);
    }

    private static FileDeletionOutboxProperties properties(boolean workerEnabled) {
        return new FileDeletionOutboxProperties(
                true, workerEnabled, 25, 10, Duration.ofSeconds(30), Duration.ofMinutes(5), WORKER_ID);
    }
}
