package com.zufar.icedlatte.filestorage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import com.zufar.icedlatte.filestorage.api.dto.FileMetadataDto;
import com.zufar.icedlatte.filestorage.config.FileDeletionOutboxProperties;
import com.zufar.icedlatte.filestorage.converter.FileMetadataDtoConverter;
import com.zufar.icedlatte.filestorage.entity.FileMetadata;
import com.zufar.icedlatte.filestorage.exception.FileListException;
import com.zufar.icedlatte.filestorage.exception.FileUploadException;
import com.zufar.icedlatte.filestorage.repository.FileDeletionOutboxRepository;
import com.zufar.icedlatte.filestorage.repository.FileMetadataRepository;
import com.zufar.icedlatte.filestorage.service.FileStorageService;
import com.zufar.icedlatte.filestorage.service.ObjectStorage;

@ExtendWith(MockitoExtension.class)
@DisplayName("FileStorageService unit tests")
public class FileStorageServiceTest {

    @Mock
    private ObjectStorage objectStorage;

    @Mock
    private FileMetadataRepository fileMetadataRepository;

    @Mock
    private FileMetadataDtoConverter fileMetadataDtoConverter;

    @Mock
    private FileDeletionOutboxRepository fileDeletionOutboxRepository;

    @Mock
    private FileDeletionOutboxProperties fileDeletionOutboxProperties;

    @Mock
    private MultipartFile multipartFile;

    private FileStorageService fileStorageService;

    @BeforeEach
    void setUp() {
        fileStorageService = new FileStorageService(
                objectStorage,
                fileMetadataRepository,
                fileMetadataDtoConverter,
                fileDeletionOutboxRepository,
                fileDeletionOutboxProperties);
    }

    @Test
    @DisplayName("store uploads object and replaces metadata")
    void storeUploadsObjectAndReplacesMetadata() {
        UUID relatedObjectId = UUID.randomUUID();
        FileMetadataDto metadata = new FileMetadataDto(relatedObjectId, "bucket", "key");
        FileMetadata entity = new FileMetadata();
        when(objectStorage.isConfigured()).thenReturn(true);
        when(fileMetadataRepository.findByRelatedObjectIdIn(List.of(relatedObjectId)))
                .thenReturn(List.of());
        when(fileMetadataDtoConverter.toEntity(metadata)).thenReturn(entity);

        fileStorageService.store(multipartFile, metadata);

        verify(objectStorage).upload(multipartFile, "bucket", "key");
        verify(fileMetadataRepository).deleteByRelatedObjectId(relatedObjectId);
        verify(fileMetadataRepository).save(entity);
    }

    @Test
    @DisplayName("store records deletion for replaced object metadata")
    void storeRecordsDeletionForReplacedObjectMetadata() {
        UUID relatedObjectId = UUID.randomUUID();
        FileMetadata oldEntity = fileMetadataEntity(relatedObjectId);
        FileMetadataDto oldMetadata = new FileMetadataDto(relatedObjectId, "bucket", "old-key");
        FileMetadataDto newMetadata = new FileMetadataDto(relatedObjectId, "bucket", "new-key");
        FileMetadata newEntity = new FileMetadata();
        when(objectStorage.isConfigured()).thenReturn(true);
        when(fileMetadataRepository.findByRelatedObjectIdIn(List.of(relatedObjectId)))
                .thenReturn(List.of(oldEntity));
        when(fileMetadataDtoConverter.toDto(oldEntity)).thenReturn(oldMetadata);
        when(fileMetadataDtoConverter.toEntity(newMetadata)).thenReturn(newEntity);
        when(fileDeletionOutboxProperties.maxAttempts()).thenReturn(10);

        fileStorageService.store(multipartFile, newMetadata);

        verify(fileDeletionOutboxRepository).insertDeleteObjectEvent(oldMetadata, 10);
    }

    @Test
    @DisplayName("store does not record deletion when replacing the same object key")
    void storeDoesNotRecordDeletionForSameObjectKey() {
        UUID relatedObjectId = UUID.randomUUID();
        FileMetadata oldEntity = fileMetadataEntity(relatedObjectId);
        FileMetadataDto metadata = new FileMetadataDto(relatedObjectId, "bucket", "key");
        when(objectStorage.isConfigured()).thenReturn(true);
        when(fileMetadataRepository.findByRelatedObjectIdIn(List.of(relatedObjectId)))
                .thenReturn(List.of(oldEntity));
        when(fileMetadataDtoConverter.toDto(oldEntity)).thenReturn(metadata);
        when(fileMetadataDtoConverter.toEntity(metadata)).thenReturn(new FileMetadata());

        fileStorageService.store(multipartFile, metadata);

        verify(fileDeletionOutboxRepository, never()).insertDeleteObjectEvent(any(), anyInt());
    }

    @Test
    @DisplayName("store deletes uploaded object when metadata persistence fails")
    void storeDeletesUploadedObjectWhenMetadataPersistenceFails() {
        UUID relatedObjectId = UUID.randomUUID();
        FileMetadataDto metadata = new FileMetadataDto(relatedObjectId, "bucket", "key");
        RuntimeException failure = new IllegalStateException("database down");
        when(objectStorage.isConfigured()).thenReturn(true);
        when(fileMetadataRepository.findByRelatedObjectIdIn(List.of(relatedObjectId)))
                .thenReturn(List.of());
        when(fileMetadataDtoConverter.toEntity(metadata)).thenReturn(new FileMetadata());
        when(fileMetadataRepository.save(any())).thenThrow(failure);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> fileStorageService.store(multipartFile, metadata))
                .isSameAs(failure);

        verify(objectStorage).delete(metadata);
    }

    @Test
    @DisplayName("store rejects writes when object storage is disabled")
    void storeRejectsWritesWhenObjectStorageIsDisabled() {
        UUID relatedObjectId = UUID.randomUUID();
        FileMetadataDto metadata = new FileMetadataDto(relatedObjectId, "bucket", "key");
        when(objectStorage.isConfigured()).thenReturn(false);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> fileStorageService.store(multipartFile, metadata))
                .isInstanceOf(FileUploadException.class);

        verifyNoInteractions(fileMetadataRepository, fileDeletionOutboxRepository);
    }

    @Test
    @DisplayName("recordExisting replaces metadata without uploading object")
    void recordExistingReplacesMetadataWithoutUploadingObject() {
        UUID relatedObjectId = UUID.randomUUID();
        FileMetadata oldEntity = fileMetadataEntity(relatedObjectId);
        FileMetadataDto oldMetadata = new FileMetadataDto(relatedObjectId, "bucket", "old-key");
        FileMetadataDto newMetadata = new FileMetadataDto(relatedObjectId, "bucket", "new-key");
        FileMetadata newEntity = new FileMetadata();
        when(objectStorage.isConfigured()).thenReturn(true);
        when(fileMetadataRepository.findByRelatedObjectIdIn(List.of(relatedObjectId)))
                .thenReturn(List.of(oldEntity));
        when(fileMetadataDtoConverter.toDto(oldEntity)).thenReturn(oldMetadata);
        when(fileMetadataDtoConverter.toEntity(newMetadata)).thenReturn(newEntity);
        when(fileDeletionOutboxProperties.maxAttempts()).thenReturn(10);

        fileStorageService.recordExisting(newMetadata);

        verify(objectStorage, never()).upload(any(), any(), any());
        verify(fileMetadataRepository).deleteByRelatedObjectId(relatedObjectId);
        verify(fileMetadataRepository).save(newEntity);
        verify(fileDeletionOutboxRepository).insertDeleteObjectEvent(oldMetadata, 10);
    }

    @Test
    @DisplayName("recordExisting does not record deletion when metadata points at same object")
    void recordExistingDoesNotRecordDeletionForSameObject() {
        UUID relatedObjectId = UUID.randomUUID();
        FileMetadata oldEntity = fileMetadataEntity(relatedObjectId);
        FileMetadataDto metadata = new FileMetadataDto(relatedObjectId, "bucket", "key");
        when(objectStorage.isConfigured()).thenReturn(true);
        when(fileMetadataRepository.findByRelatedObjectIdIn(List.of(relatedObjectId)))
                .thenReturn(List.of(oldEntity));
        when(fileMetadataDtoConverter.toDto(oldEntity)).thenReturn(metadata);
        when(fileMetadataDtoConverter.toEntity(metadata)).thenReturn(new FileMetadata());

        fileStorageService.recordExisting(metadata);

        verify(fileDeletionOutboxRepository, never()).insertDeleteObjectEvent(any(), anyInt());
    }

    @Test
    @DisplayName("recordExisting rejects metadata writes when object storage is disabled")
    void recordExistingRejectsWritesWhenObjectStorageIsDisabled() {
        UUID relatedObjectId = UUID.randomUUID();
        FileMetadataDto metadata = new FileMetadataDto(relatedObjectId, "bucket", "key");
        when(objectStorage.isConfigured()).thenReturn(false);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> fileStorageService.recordExisting(metadata))
                .isInstanceOf(FileUploadException.class);

        verifyNoInteractions(fileMetadataRepository, fileDeletionOutboxRepository);
    }

    @Test
    @DisplayName("enqueueDeleteObject records deletion event without touching metadata")
    void enqueueDeleteObjectRecordsDeletionEvent() {
        FileMetadataDto metadata = new FileMetadataDto(UUID.randomUUID(), "bucket", "key");
        when(fileDeletionOutboxProperties.maxAttempts()).thenReturn(7);

        fileStorageService.enqueueDeleteObject(metadata);

        verify(fileDeletionOutboxRepository).insertDeleteObjectEvent(metadata, 7);
        verifyNoInteractions(objectStorage, fileMetadataRepository, fileMetadataDtoConverter);
    }

    @Test
    @DisplayName("storeDirectory delegates to object storage")
    void storeDirectoryDelegates() throws IOException {
        when(objectStorage.isConfigured()).thenReturn(true);

        fileStorageService.storeDirectory("bucket", "/tmp/assets");

        verify(objectStorage).uploadDirectory("bucket", "/tmp/assets");
    }

    @Test
    @DisplayName("refreshBucketIndex rejects listing when object storage is disabled")
    void refreshBucketIndexRejectsListingWhenObjectStorageIsDisabled() {
        when(objectStorage.isConfigured()).thenReturn(false);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> fileStorageService.refreshBucketIndex("bucket"))
                .isInstanceOf(FileListException.class);

        verify(fileMetadataRepository, never()).deleteByBucketName(any());
        verify(fileMetadataRepository, never()).saveAll(any());
    }

    @Nested
    @DisplayName("findFileUrl")
    class FindFileUrl {

        @Test
        @DisplayName("returns generated URL when metadata exists")
        void returnsGeneratedUrl() {
            UUID relatedObjectId = UUID.randomUUID();
            FileMetadata entity = new FileMetadata();
            entity.setRelatedObjectId(relatedObjectId);
            FileMetadataDto metadata = new FileMetadataDto(relatedObjectId, "bucket", "key");
            when(fileMetadataRepository.findByRelatedObjectIdIn(List.of(relatedObjectId)))
                    .thenReturn(List.of(entity));
            when(fileMetadataDtoConverter.toDto(entity)).thenReturn(metadata);
            when(objectStorage.getUrl(metadata)).thenReturn(Optional.of("https://cdn.example.com/key"));

            assertThat(fileStorageService.findFileUrl(relatedObjectId)).contains("https://cdn.example.com/key");
        }

        @Test
        @DisplayName("returns empty when metadata is missing")
        void returnsEmptyWhenMetadataMissing() {
            UUID relatedObjectId = UUID.randomUUID();
            when(fileMetadataRepository.findByRelatedObjectIdIn(List.of(relatedObjectId)))
                    .thenReturn(List.of());

            assertThat(fileStorageService.findFileUrl(relatedObjectId)).isEmpty();
            verifyNoInteractions(objectStorage);
        }

        @Test
        @DisplayName("resolves URL directly from exact metadata")
        void resolvesUrlDirectlyFromExactMetadata() {
            UUID relatedObjectId = UUID.randomUUID();
            FileMetadataDto metadata = new FileMetadataDto(relatedObjectId, "bucket", "processed/key.webp");
            when(objectStorage.getUrl(metadata)).thenReturn(Optional.of("https://cdn.example.com/processed/key.webp"));

            assertThat(fileStorageService.findFileUrl(metadata)).contains("https://cdn.example.com/processed/key.webp");
            verifyNoInteractions(fileMetadataRepository);
        }
    }

    @Test
    @DisplayName("findFileUrls returns only objects with resolvable URLs")
    void findFileUrlsReturnsOnlyResolvableUrls() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        FileMetadata entity1 = fileMetadataEntity(id1);
        FileMetadata entity2 = fileMetadataEntity(id2);
        FileMetadataDto metadata1 = new FileMetadataDto(id1, "bucket", "key-1");
        FileMetadataDto metadata2 = new FileMetadataDto(id2, "bucket", "key-2");
        when(fileMetadataRepository.findByRelatedObjectIdIn(List.of(id1, id2))).thenReturn(List.of(entity1, entity2));
        when(fileMetadataDtoConverter.toDto(entity1)).thenReturn(metadata1);
        when(fileMetadataDtoConverter.toDto(entity2)).thenReturn(metadata2);
        when(objectStorage.getUrl(metadata1)).thenReturn(Optional.of("https://cdn.example.com/key-1"));
        when(objectStorage.getUrl(metadata2)).thenReturn(Optional.empty());

        Map<UUID, String> result = fileStorageService.findFileUrls(List.of(id1, id2));

        assertThat(result).containsEntry(id1, "https://cdn.example.com/key-1");
        assertThat(result).doesNotContainKey(id2);
    }

    @Test
    @DisplayName("findFileUrls returns empty result without querying repository for empty input")
    void findFileUrlsReturnsEmptyResultWithoutQueryingRepositoryForEmptyInput() {
        Map<UUID, String> result = fileStorageService.findFileUrls(List.of());

        assertThat(result).isEmpty();
        verifyNoInteractions(fileMetadataRepository, objectStorage);
    }

    @Test
    @DisplayName("findFileUrls resolves duplicate metadata using preferred image")
    void findFileUrlsResolvesDuplicateMetadataUsingPreferredImage() {
        UUID id = UUID.randomUUID();
        FileMetadata pngEntity = fileMetadataEntity(id);
        FileMetadata webpEntity = fileMetadataEntity(id);
        FileMetadataDto pngMetadata = new FileMetadataDto(id, "bucket", "Product_" + id + "/card_logo.png");
        FileMetadataDto webpMetadata = new FileMetadataDto(id, "bucket", "Product_" + id + "/card_logo.webp");
        when(fileMetadataRepository.findByRelatedObjectIdIn(List.of(id))).thenReturn(List.of(pngEntity, webpEntity));
        when(fileMetadataDtoConverter.toDto(pngEntity)).thenReturn(pngMetadata);
        when(fileMetadataDtoConverter.toDto(webpEntity)).thenReturn(webpMetadata);
        when(objectStorage.getUrl(webpMetadata)).thenReturn(Optional.of("https://cdn.example.com/card_logo.webp"));

        Map<UUID, String> result = fileStorageService.findFileUrls(List.of(id));

        assertThat(result).containsEntry(id, "https://cdn.example.com/card_logo.webp");
        verify(objectStorage).getUrl(webpMetadata);
        verify(objectStorage, never()).getUrl(pngMetadata);
    }

    @Test
    @DisplayName("deleteFile removes metadata and records object deletion")
    void deleteFileRemovesMetadataAndRecordsObjectDeletion() {
        UUID relatedObjectId = UUID.randomUUID();
        FileMetadata entity = new FileMetadata();
        entity.setRelatedObjectId(relatedObjectId);
        FileMetadataDto metadata = new FileMetadataDto(relatedObjectId, "bucket", "key");
        when(fileMetadataRepository.findByRelatedObjectIdIn(List.of(relatedObjectId)))
                .thenReturn(List.of(entity));
        when(fileMetadataDtoConverter.toDto(entity)).thenReturn(metadata);
        when(fileMetadataRepository.deleteByRelatedObjectId(relatedObjectId)).thenReturn(1);
        when(fileDeletionOutboxProperties.maxAttempts()).thenReturn(10);

        fileStorageService.deleteFile(relatedObjectId);

        verify(fileMetadataRepository).deleteByRelatedObjectId(relatedObjectId);
        verify(fileDeletionOutboxRepository).insertDeleteObjectEvent(metadata, 10);
        verify(objectStorage, never()).delete(any());
    }

    @Test
    @DisplayName("deleteFile records deletion for every duplicate metadata object")
    void deleteFileRecordsDeletionForEveryDuplicateMetadataObject() {
        UUID relatedObjectId = UUID.randomUUID();
        FileMetadata firstEntity = new FileMetadata();
        firstEntity.setRelatedObjectId(relatedObjectId);
        FileMetadata secondEntity = new FileMetadata();
        secondEntity.setRelatedObjectId(relatedObjectId);
        FileMetadataDto first = new FileMetadataDto(relatedObjectId, "bucket", "first-key");
        FileMetadataDto second = new FileMetadataDto(relatedObjectId, "bucket", "second-key");
        when(fileMetadataRepository.findByRelatedObjectIdIn(List.of(relatedObjectId)))
                .thenReturn(List.of(firstEntity, secondEntity));
        when(fileMetadataDtoConverter.toDto(firstEntity)).thenReturn(first);
        when(fileMetadataDtoConverter.toDto(secondEntity)).thenReturn(second);
        when(fileMetadataRepository.deleteByRelatedObjectId(relatedObjectId)).thenReturn(2);
        when(fileDeletionOutboxProperties.maxAttempts()).thenReturn(10);

        fileStorageService.deleteFile(relatedObjectId);

        verify(fileDeletionOutboxRepository).insertDeleteObjectEvent(first, 10);
        verify(fileDeletionOutboxRepository).insertDeleteObjectEvent(second, 10);
    }

    @Test
    @DisplayName("deleteFile skips outbox insert when concurrent delete already removed metadata")
    void deleteFileSkipsOutboxInsertWhenConcurrentDeleteAlreadyRemovedMetadata() {
        UUID relatedObjectId = UUID.randomUUID();
        FileMetadata entity = new FileMetadata();
        entity.setRelatedObjectId(relatedObjectId);
        FileMetadataDto metadata = new FileMetadataDto(relatedObjectId, "bucket", "key");
        when(fileMetadataRepository.findByRelatedObjectIdIn(List.of(relatedObjectId)))
                .thenReturn(List.of(entity));
        when(fileMetadataDtoConverter.toDto(entity)).thenReturn(metadata);
        when(fileMetadataRepository.deleteByRelatedObjectId(relatedObjectId)).thenReturn(0);
        when(fileDeletionOutboxProperties.maxAttempts()).thenReturn(10);

        fileStorageService.deleteFile(relatedObjectId);

        verify(fileDeletionOutboxRepository, never()).insertDeleteObjectEvent(any(), anyInt());
    }

    @Test
    @DisplayName("deleteFile does nothing when metadata is missing")
    void deleteFileDoesNothingWhenMetadataIsMissing() {
        UUID relatedObjectId = UUID.randomUUID();
        when(fileMetadataRepository.findByRelatedObjectIdIn(List.of(relatedObjectId)))
                .thenReturn(List.of());

        fileStorageService.deleteFile(relatedObjectId);

        verify(fileMetadataRepository, never()).deleteByRelatedObjectId(any());
        verifyNoInteractions(fileDeletionOutboxRepository);
        verifyNoInteractions(objectStorage);
    }

    @Test
    @DisplayName("refreshBucketIndex clears old bucket metadata before saving")
    void refreshBucketIndexClearsOldBucketMetadataBeforeSaving() {
        UUID relatedObjectId = UUID.randomUUID();
        List<FileMetadataDto> metadata =
                List.of(new FileMetadataDto(relatedObjectId, "bucket", "product_" + relatedObjectId + "/key"));
        List<FileMetadata> entities = List.of(new FileMetadata());
        when(objectStorage.isConfigured()).thenReturn(true);
        when(objectStorage.listObjectKeys("bucket")).thenReturn(List.of("product_" + relatedObjectId + "/key"));
        when(fileMetadataDtoConverter.toEntityList(metadata)).thenReturn(entities);

        fileStorageService.refreshBucketIndex("bucket");

        verify(fileMetadataRepository).deleteByBucketName("bucket");
        verify(fileMetadataRepository).saveAll(entities);
    }

    @Test
    @DisplayName("refreshBucketIndex maps valid storage keys to metadata before saving")
    @SuppressWarnings("unchecked")
    void refreshBucketIndexMapsValidStorageKeysToMetadataBeforeSaving() {
        UUID relatedObjectId = UUID.randomUUID();
        List<FileMetadataDto> metadata =
                List.of(new FileMetadataDto(relatedObjectId, "bucket", "product_" + relatedObjectId + "/cover.jpg"));
        when(objectStorage.isConfigured()).thenReturn(true);
        when(objectStorage.listObjectKeys("bucket"))
                .thenReturn(List.of("product_" + relatedObjectId + "/cover.jpg", "invalid-key"));
        when(fileMetadataDtoConverter.toEntityList(metadata)).thenReturn(List.of(new FileMetadata()));

        fileStorageService.refreshBucketIndex("bucket");

        ArgumentCaptor<List<FileMetadataDto>> captor = ArgumentCaptor.forClass(List.class);
        verify(fileMetadataDtoConverter).toEntityList(captor.capture());
        assertThat(captor.getValue()).containsExactlyElementsOf(metadata);
    }

    @Test
    @DisplayName("refreshBucketIndex skips folder markers and root keys")
    @SuppressWarnings("unchecked")
    void refreshBucketIndexSkipsFolderMarkersAndRootKeys() {
        UUID relatedObjectId = UUID.randomUUID();
        String validKey = "product_" + relatedObjectId + "/cover.jpg";
        List<FileMetadataDto> metadata = List.of(new FileMetadataDto(relatedObjectId, "bucket", validKey));
        when(objectStorage.isConfigured()).thenReturn(true);
        when(objectStorage.listObjectKeys("bucket"))
                .thenReturn(List.of("product_" + relatedObjectId, "product_" + relatedObjectId + "/", validKey));
        when(fileMetadataDtoConverter.toEntityList(metadata)).thenReturn(List.of(new FileMetadata()));

        fileStorageService.refreshBucketIndex("bucket");

        ArgumentCaptor<List<FileMetadataDto>> captor = ArgumentCaptor.forClass(List.class);
        verify(fileMetadataDtoConverter).toEntityList(captor.capture());
        assertThat(captor.getValue()).containsExactlyElementsOf(metadata);
    }

    @Test
    @DisplayName("refreshBucketIndex accepts product folder names containing underscores")
    @SuppressWarnings("unchecked")
    void refreshBucketIndexAcceptsProductFolderNamesContainingUnderscores() {
        UUID relatedObjectId = UUID.randomUUID();
        String key = "cold_brew_latte_" + relatedObjectId + "/cover.jpg";
        List<FileMetadataDto> metadata = List.of(new FileMetadataDto(relatedObjectId, "bucket", key));
        when(objectStorage.isConfigured()).thenReturn(true);
        when(objectStorage.listObjectKeys("bucket")).thenReturn(List.of(key));
        when(fileMetadataDtoConverter.toEntityList(metadata)).thenReturn(List.of(new FileMetadata()));

        fileStorageService.refreshBucketIndex("bucket");

        ArgumentCaptor<List<FileMetadataDto>> captor = ArgumentCaptor.forClass(List.class);
        verify(fileMetadataDtoConverter).toEntityList(captor.capture());
        assertThat(captor.getValue()).containsExactlyElementsOf(metadata);
    }

    @Test
    @DisplayName("refreshBucketIndex saves one preferred metadata row per related object")
    @SuppressWarnings("unchecked")
    void refreshBucketIndexSavesOnePreferredMetadataRowPerRelatedObject() {
        UUID relatedObjectId = UUID.randomUUID();
        FileMetadataDto preferred =
                new FileMetadataDto(relatedObjectId, "bucket", "product_" + relatedObjectId + "/card_logo.webp");
        when(objectStorage.isConfigured()).thenReturn(true);
        when(objectStorage.listObjectKeys("bucket"))
                .thenReturn(List.of(
                        "product_" + relatedObjectId + "/card_logo.png",
                        "product_" + relatedObjectId + "/card_logo.webp"));
        when(fileMetadataDtoConverter.toEntityList(List.of(preferred))).thenReturn(List.of(new FileMetadata()));

        fileStorageService.refreshBucketIndex("bucket");

        ArgumentCaptor<List<FileMetadataDto>> captor = ArgumentCaptor.forClass(List.class);
        verify(fileMetadataDtoConverter).toEntityList(captor.capture());
        assertThat(captor.getValue()).containsExactly(preferred);
    }

    @Test
    @DisplayName("isEnabled delegates to object storage")
    void isEnabledDelegatesToObjectStorage() {
        when(objectStorage.isConfigured()).thenReturn(true);

        assertThat(fileStorageService.isEnabled()).isTrue();
    }

    private static FileMetadata fileMetadataEntity(UUID relatedObjectId) {
        FileMetadata entity = new FileMetadata();
        entity.setRelatedObjectId(relatedObjectId);
        return entity;
    }
}
