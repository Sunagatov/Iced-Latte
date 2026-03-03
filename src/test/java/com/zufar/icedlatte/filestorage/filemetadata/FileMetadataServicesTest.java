package com.zufar.icedlatte.filestorage.filemetadata;

import com.zufar.icedlatte.filestorage.converter.FileMetadataDtoConverter;
import com.zufar.icedlatte.filestorage.dto.FileMetadataDto;
import com.zufar.icedlatte.filestorage.entity.FileMetadata;
import com.zufar.icedlatte.filestorage.repository.FileMetadataRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("FileMetadata service unit tests")
class FileMetadataServicesTest {

    @Nested
    @DisplayName("FileMetadataProvider")
    class ProviderTests {

        @Mock FileMetadataRepository repo;
        @Mock FileMetadataDtoConverter converter;
        @InjectMocks FileMetadataProvider provider;

        @Test
        @DisplayName("getFileMetadataDto returns mapped DTO when found")
        void getFileMetadataDto_found_returnsDto() {
            UUID id = UUID.randomUUID();
            FileMetadata entity = new FileMetadata();
            FileMetadataDto dto = new FileMetadataDto(id, "bucket", "file.jpg");
            when(repo.findAvatarInfoByRelatedObjectId(id)).thenReturn(Optional.of(entity));
            when(converter.toDto(entity)).thenReturn(dto);

            assertThat(provider.getFileMetadataDto(id)).contains(dto);
        }

        @Test
        @DisplayName("getFileMetadataDto returns empty when not found")
        void getFileMetadataDto_notFound_returnsEmpty() {
            UUID id = UUID.randomUUID();
            when(repo.findAvatarInfoByRelatedObjectId(id)).thenReturn(Optional.empty());

            assertThat(provider.getFileMetadataDto(id)).isEmpty();
        }

        @Test
        @DisplayName("getFileMetadataDtos returns map keyed by relatedObjectId")
        void getFileMetadataDtos_returnsMap() {
            UUID id = UUID.randomUUID();
            FileMetadata entity = FileMetadata.builder().relatedObjectId(id).build();
            FileMetadataDto dto = new FileMetadataDto(id, "bucket", "file.jpg");
            when(repo.findAvatarInfoByRelatedObjectIds(List.of(id))).thenReturn(List.of(entity));
            when(converter.toDto(entity)).thenReturn(dto);

            Map<UUID, FileMetadataDto> result = provider.getFileMetadataDtos(List.of(id));

            assertThat(result).containsEntry(id, dto);
        }
    }

    @Nested
    @DisplayName("FileMetadataDeleter")
    class DeleterTests {

        @Mock FileMetadataRepository repo;
        @InjectMocks FileMetadataDeleter deleter;

        @Test
        @DisplayName("deleteByRelatedObjectId delegates to repository")
        void deleteByRelatedObjectId_callsRepo() {
            UUID id = UUID.randomUUID();
            deleter.deleteByRelatedObjectId(id);
            verify(repo).deleteByRelatedObjectId(id);
        }

        @Test
        @DisplayName("deleteByBucketName delegates to repository")
        void deleteByBucketName_callsRepo() {
            deleter.deleteByBucketName("my-bucket");
            verify(repo).deleteByBucketName("my-bucket");
        }
    }

    @Nested
    @DisplayName("FileMetadataSaver")
    class SaverTests {

        @Mock FileMetadataRepository repo;
        @Mock FileMetadataDtoConverter converter;
        @InjectMocks FileMetadataSaver saver;

        @Test
        @DisplayName("save converts DTO to entity and persists it")
        void save_convertsAndSaves() {
            UUID id = UUID.randomUUID();
            FileMetadataDto dto = new FileMetadataDto(id, "bucket", "file.jpg");
            FileMetadata entity = new FileMetadata();
            when(converter.toEntity(dto)).thenReturn(entity);

            saver.save(dto);

            verify(repo).save(entity);
        }

        @Test
        @DisplayName("saveAll converts list and persists all")
        void saveAll_convertsAndSavesAll() {
            UUID id = UUID.randomUUID();
            FileMetadataDto dto = new FileMetadataDto(id, "bucket", "file.jpg");
            FileMetadata entity = new FileMetadata();
            when(converter.toEntityList(List.of(dto))).thenReturn(List.of(entity));

            saver.saveAll(List.of(dto));

            verify(repo).saveAll(List.of(entity));
        }
    }
}
