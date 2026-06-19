package com.zufar.icedlatte.filestorage;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;

import com.zufar.icedlatte.filestorage.api.dto.FileMetadataDto;
import com.zufar.icedlatte.filestorage.repository.FileMetadataRepository;
import com.zufar.icedlatte.filestorage.service.FileDeletionOutboxWorker;
import com.zufar.icedlatte.filestorage.service.FileStorageService;
import com.zufar.icedlatte.filestorage.service.ObjectStorage;
import com.zufar.icedlatte.test.config.StorageBackedIntegrationTestBase;

@DisplayName("FileStorageService integration tests")
class FileStorageServiceIntegrationTest extends StorageBackedIntegrationTestBase {

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private FileMetadataRepository fileMetadataRepository;

    @Autowired
    private FileDeletionOutboxWorker fileDeletionOutboxWorker;

    @Autowired
    private ObjectStorage objectStorage;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("store uploads object, persists metadata, and resolves a file URL")
    void storeUploadsObjectPersistsMetadataAndResolvesFileUrl() {
        UUID relatedObjectId = UUID.randomUUID();
        String fileName = "avatars/%s/avatar.png".formatted(relatedObjectId);
        FileMetadataDto metadata = new FileMetadataDto(relatedObjectId, USER_AVATAR_BUCKET, fileName);

        fileStorageService.store(
                new MockMultipartFile("file", "avatar.png", "image/png", "avatar-content".getBytes()), metadata);

        assertThat(fileMetadataRepository.findByRelatedObjectIdIn(List.of(relatedObjectId)))
                .singleElement()
                .satisfies(entity -> {
                    assertThat(entity.getRelatedObjectId()).isEqualTo(relatedObjectId);
                    assertThat(entity.getBucketName()).isEqualTo(USER_AVATAR_BUCKET);
                    assertThat(entity.getFileName()).isEqualTo(fileName);
                });
        assertThat(objectStorage.listObjectKeys(USER_AVATAR_BUCKET)).contains(fileName);
        assertThat(fileStorageService.findFileUrl(relatedObjectId))
                .hasValueSatisfying(url -> assertThat(url).contains("avatar.png"));
    }

    @Test
    @DisplayName("deleteFile removes metadata, enqueues outbox deletion, and worker deletes the object")
    void deleteFileRemovesMetadataEnqueuesOutboxDeletionAndWorkerDeletesObject() {
        UUID relatedObjectId = UUID.randomUUID();
        String fileName = "avatars/%s/old-avatar.png".formatted(relatedObjectId);
        FileMetadataDto metadata = new FileMetadataDto(relatedObjectId, USER_AVATAR_BUCKET, fileName);

        fileStorageService.store(
                new MockMultipartFile("file", "old-avatar.png", "image/png", "avatar-content".getBytes()), metadata);

        fileStorageService.deleteFile(relatedObjectId);

        assertThat(fileMetadataRepository.findByRelatedObjectIdIn(List.of(relatedObjectId)))
                .isEmpty();
        assertThat(selectOutboxStatuses(relatedObjectId)).containsExactly("PENDING");
        assertThat(objectStorage.listObjectKeys(USER_AVATAR_BUCKET)).contains(fileName);

        fileDeletionOutboxWorker.deletePendingObjects();

        assertThat(objectStorage.listObjectKeys(USER_AVATAR_BUCKET)).doesNotContain(fileName);
        assertThat(selectOutboxStatuses(relatedObjectId)).containsExactly("PUBLISHED");
    }

    private List<String> selectOutboxStatuses(UUID relatedObjectId) {
        return jdbcTemplate.queryForList("""
                SELECT status
                FROM outbox_events
                WHERE event_type = 'file.object.delete'
                  AND partition_key = ?
                ORDER BY created_at
                """, String.class, relatedObjectId.toString());
    }
}
