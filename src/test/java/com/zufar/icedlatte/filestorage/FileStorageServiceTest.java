package com.zufar.icedlatte.filestorage;

import com.zufar.icedlatte.filestorage.converter.FileMetadataDtoConverter;
import com.zufar.icedlatte.filestorage.dto.FileMetadataDto;
import com.zufar.icedlatte.filestorage.entity.FileMetadata;
import com.zufar.icedlatte.filestorage.repository.FileMetadataRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

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
    private MultipartFile multipartFile;

    @InjectMocks
    private FileStorageService fileStorageService;

    @Test
    @DisplayName("store uploads object and replaces metadata")
    void storeUploadsObjectAndReplacesMetadata() {
        UUID relatedObjectId = UUID.randomUUID();
        FileMetadataDto metadata = new FileMetadataDto(relatedObjectId, "bucket", "key");
        FileMetadata entity = new FileMetadata();
        when(fileMetadataDtoConverter.toEntity(metadata)).thenReturn(entity);

        fileStorageService.store(multipartFile, metadata);

        verify(objectStorage).upload(multipartFile, "bucket", "key");
        verify(fileMetadataRepository).deleteByRelatedObjectId(relatedObjectId);
        verify(fileMetadataRepository).save(entity);
    }

    @Test
    @DisplayName("storeDirectory delegates to object storage")
    void storeDirectoryDelegates() throws IOException {
        fileStorageService.storeDirectory("bucket", "/tmp/assets");

        verify(objectStorage).uploadDirectory("bucket", "/tmp/assets");
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
            when(fileMetadataRepository.findAvatarInfoByRelatedObjectIds(List.of(relatedObjectId))).thenReturn(List.of(entity));
            when(fileMetadataDtoConverter.toDto(entity)).thenReturn(metadata);
            when(objectStorage.getUrl(metadata)).thenReturn(Optional.of("https://cdn.example.com/key"));

            assertThat(fileStorageService.findFileUrl(relatedObjectId)).contains("https://cdn.example.com/key");
        }

        @Test
        @DisplayName("returns empty when metadata is missing")
        void returnsEmptyWhenMetadataMissing() {
            UUID relatedObjectId = UUID.randomUUID();
            when(fileMetadataRepository.findAvatarInfoByRelatedObjectIds(List.of(relatedObjectId))).thenReturn(List.of());

            assertThat(fileStorageService.findFileUrl(relatedObjectId)).isEmpty();
            verifyNoInteractions(objectStorage);
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
        when(fileMetadataRepository.findAvatarInfoByRelatedObjectIds(List.of(id1, id2))).thenReturn(List.of(entity1, entity2));
        when(fileMetadataDtoConverter.toDto(entity1)).thenReturn(metadata1);
        when(fileMetadataDtoConverter.toDto(entity2)).thenReturn(metadata2);
        when(objectStorage.getUrl(metadata1)).thenReturn(Optional.of("https://cdn.example.com/key-1"));
        when(objectStorage.getUrl(metadata2)).thenReturn(Optional.empty());

        Map<UUID, String> result = fileStorageService.findFileUrls(List.of(id1, id2));

        assertThat(result).containsEntry(id1, "https://cdn.example.com/key-1");
        assertThat(result).doesNotContainKey(id2);
    }

    @Test
    @DisplayName("findFileUrls resolves duplicate metadata using preferred image")
    void findFileUrlsResolvesDuplicateMetadataUsingPreferredImage() {
        UUID id = UUID.randomUUID();
        FileMetadata pngEntity = fileMetadataEntity(id);
        FileMetadata webpEntity = fileMetadataEntity(id);
        FileMetadataDto pngMetadata = new FileMetadataDto(id, "bucket", "Product_" + id + "/card_logo.png");
        FileMetadataDto webpMetadata = new FileMetadataDto(id, "bucket", "Product_" + id + "/card_logo.webp");
        when(fileMetadataRepository.findAvatarInfoByRelatedObjectIds(List.of(id))).thenReturn(List.of(pngEntity, webpEntity));
        when(fileMetadataDtoConverter.toDto(pngEntity)).thenReturn(pngMetadata);
        when(fileMetadataDtoConverter.toDto(webpEntity)).thenReturn(webpMetadata);
        when(objectStorage.getUrl(webpMetadata)).thenReturn(Optional.of("https://cdn.example.com/card_logo.webp"));

        Map<UUID, String> result = fileStorageService.findFileUrls(List.of(id));

        assertThat(result).containsEntry(id, "https://cdn.example.com/card_logo.webp");
        verify(objectStorage).getUrl(webpMetadata);
        verify(objectStorage, never()).getUrl(pngMetadata);
    }

    @Test
    @DisplayName("deleteFile removes object and metadata when metadata exists")
    void deleteFileRemovesObjectAndMetadata() {
        UUID relatedObjectId = UUID.randomUUID();
        FileMetadata entity = new FileMetadata();
        entity.setRelatedObjectId(relatedObjectId);
        FileMetadataDto metadata = new FileMetadataDto(relatedObjectId, "bucket", "key");
        when(fileMetadataRepository.findAvatarInfoByRelatedObjectIds(List.of(relatedObjectId))).thenReturn(List.of(entity));
        when(fileMetadataDtoConverter.toDto(entity)).thenReturn(metadata);

        fileStorageService.deleteFile(relatedObjectId);

        verify(objectStorage).delete(metadata);
        verify(fileMetadataRepository).deleteByRelatedObjectId(relatedObjectId);
    }

    @Test
    @DisplayName("refreshBucketIndex clears old bucket metadata before saving")
    void refreshBucketIndexClearsOldBucketMetadataBeforeSaving() {
        UUID relatedObjectId = UUID.randomUUID();
        List<FileMetadataDto> metadata = List.of(new FileMetadataDto(
                relatedObjectId,
                "bucket",
                "product_" + relatedObjectId + "/key"
        ));
        List<FileMetadata> entities = List.of(new FileMetadata());
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
        List<FileMetadataDto> metadata = List.of(new FileMetadataDto(
                relatedObjectId,
                "bucket",
                "product_" + relatedObjectId + "/cover.jpg"
        ));
        when(objectStorage.listObjectKeys("bucket")).thenReturn(List.of(
                "product_" + relatedObjectId + "/cover.jpg",
                "invalid-key"
        ));
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
        FileMetadataDto preferred = new FileMetadataDto(
                relatedObjectId,
                "bucket",
                "product_" + relatedObjectId + "/card_logo.webp"
        );
        when(objectStorage.listObjectKeys("bucket")).thenReturn(List.of(
                "product_" + relatedObjectId + "/card_logo.png",
                "product_" + relatedObjectId + "/card_logo.webp"
        ));
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
