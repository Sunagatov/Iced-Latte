package com.zufar.icedlatte.filestorage.filemetadata;

import com.zufar.icedlatte.filestorage.converter.FileMetadataDtoConverter;
import com.zufar.icedlatte.filestorage.dto.FileMetadataDto;
import com.zufar.icedlatte.filestorage.entity.FileMetadata;
import com.zufar.icedlatte.filestorage.repository.FileMetadataRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("FileMetadataSaver")
class FileMetadataSaverTest {

    @Mock
    private FileMetadataRepository fileMetadataRepository;
    @Mock
    private FileMetadataDtoConverter fileMetadataDtoConverter;
    @InjectMocks
    private FileMetadataSaver saver;

    @Test
    @DisplayName("converts and saves a single metadata dto")
    void convertsAndSavesSingleMetadataDto() {
        FileMetadataDto dto = new FileMetadataDto(UUID.randomUUID(), "avatars", "user.png");
        FileMetadata entity = new FileMetadata();
        when(fileMetadataDtoConverter.toEntity(dto)).thenReturn(entity);

        saver.save(dto);

        inOrder(fileMetadataDtoConverter, fileMetadataRepository)
                .verify(fileMetadataDtoConverter).toEntity(dto);
        inOrder(fileMetadataDtoConverter, fileMetadataRepository)
                .verify(fileMetadataRepository).save(entity);
    }

    @Test
    @DisplayName("converts and saves all metadata dtos")
    void convertsAndSavesAllMetadataDtos() {
        FileMetadataDto dto = new FileMetadataDto(UUID.randomUUID(), "avatars", "user.png");
        FileMetadata entity = new FileMetadata();
        when(fileMetadataDtoConverter.toEntityList(List.of(dto))).thenReturn(List.of(entity));

        saver.saveAll(List.of(dto));

        inOrder(fileMetadataDtoConverter, fileMetadataRepository)
                .verify(fileMetadataDtoConverter).toEntityList(List.of(dto));
        inOrder(fileMetadataDtoConverter, fileMetadataRepository)
                .verify(fileMetadataRepository).saveAll(List.of(entity));
    }

    @Test
    @DisplayName("replaces bucket contents before saving fresh metadata")
    void replacesBucketContentsBeforeSavingFreshMetadata() {
        FileMetadataDto dto = new FileMetadataDto(UUID.randomUUID(), "avatars", "user.png");
        FileMetadata entity = new FileMetadata();
        when(fileMetadataDtoConverter.toEntityList(List.of(dto))).thenReturn(List.of(entity));

        saver.replaceAllByBucket("avatars", List.of(dto));

        var inOrder = inOrder(fileMetadataRepository, fileMetadataDtoConverter);
        inOrder.verify(fileMetadataRepository).deleteByBucketName("avatars");
        inOrder.verify(fileMetadataDtoConverter).toEntityList(List.of(dto));
        inOrder.verify(fileMetadataRepository).saveAll(List.of(entity));
    }
}
