package com.zufar.icedlatte.filestorage.service;

import java.io.IOException;
import java.time.Instant;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zufar.icedlatte.filestorage.api.dto.FileMetadataDto;
import com.zufar.icedlatte.filestorage.config.FileDeletionOutboxProperties;
import com.zufar.icedlatte.filestorage.repository.FileDeletionOutboxRepository;
import com.zufar.icedlatte.filestorage.repository.FileDeletionOutboxRepository.FileDeletionOutboxRow;
import com.zufar.icedlatte.filestorage.repository.FileDeletionOutboxRepository.FileObjectDeletionPayload;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class FileDeletionOutboxWorker {

    private final FileDeletionOutboxRepository outboxRepository;
    private final FileDeletionOutboxProperties properties;
    private final ObjectMapper objectMapper;
    private final ObjectStorage objectStorage;

    @Scheduled(fixedDelayString = "${file-storage.deletion-outbox.poll-interval:PT30S}")
    public void deletePendingObjects() {
        if (!properties.enabled() || !properties.workerEnabled()) {
            return;
        }
        if (!objectStorage.isConfigured()) {
            log.warn("file.deletion_outbox.skipped: reason=object_storage_not_configured");
            return;
        }

        int reclaimed = outboxRepository.reclaimStaleLocks(Instant.now().minus(properties.staleLockTimeout()));
        if (reclaimed > 0) {
            log.warn("file.deletion_outbox.locks.reclaimed: count={}", reclaimed);
        }

        var events = outboxRepository.claimDeleteObjectEvents(properties.batchSize(), properties.workerId());
        for (FileDeletionOutboxRow event : events) {
            deleteObject(event);
        }
    }

    private void deleteObject(FileDeletionOutboxRow event) {
        try {
            FileObjectDeletionPayload payload =
                    objectMapper.readValue(event.payload(), FileObjectDeletionPayload.class);
            objectStorage.delete(new FileMetadataDto(payload.relatedObjectId(), payload.bucketName(), payload.fileName()));
            outboxRepository.markDeleted(event.id(), properties.workerId());
            log.info("file.deletion_outbox.deleted: eventId={}, fileName={}", event.eventId(), payload.fileName());
        } catch (IOException | RuntimeException ex) {
            outboxRepository.markFailed(
                    event.id(), properties.workerId(), event.attemptCount(), event.maxAttempts(), ex);
            log.warn("file.deletion_outbox.failed: eventId={}", event.eventId(), ex);
        }
    }
}
