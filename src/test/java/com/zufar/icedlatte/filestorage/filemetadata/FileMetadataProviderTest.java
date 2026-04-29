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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("FileMetadataProvider")
class FileMetadataProviderTest {

    @Mock
    private FileMetadataRepository fileMetadataRepository;
    @Mock
    private FileMetadataDtoConverter fileMetadataDtoConverter;
    @InjectMocks
    private FileMetadataProvider provider;

    @Test
    @DisplayName("returns mapped metadata dto when entity exists")
    void returnsMappedMetadataDtoWhenEntityExists() {
        UUID relatedObjectId = UUID.randomUUID();
        FileMetadata entity = new FileMetadata();
        FileMetadataDto dto = new FileMetadataDto(relatedObjectId, "avatars", "user.png");
        when(fileMetadataRepository.findAvatarInfoByRelatedObjectId(relatedObjectId)).thenReturn(Optional.of(entity));
        when(fileMetadataDtoConverter.toDto(entity)).thenReturn(dto);

        assertThat(provider.getFileMetadataDto(relatedObjectId)).contains(dto);
    }

    @Test
    @DisplayName("returns empty when no metadata exists")
    void returnsEmptyWhenMetadataMissing() {
        UUID relatedObjectId = UUID.randomUUID();
        when(fileMetadataRepository.findAvatarInfoByRelatedObjectId(relatedObjectId)).thenReturn(Optional.empty());

        assertThat(provider.getFileMetadataDto(relatedObjectId)).isEmpty();
    }

    @Test
    @DisplayName("returns map keyed by related object id for bulk lookup")
    void returnsMapForBulkLookup() {
        UUID firstId = UUID.randomUUID();
        UUID secondId = UUID.randomUUID();
        FileMetadata first = FileMetadata.builder().relatedObjectId(firstId).build();
        FileMetadata second = FileMetadata.builder().relatedObjectId(secondId).build();
        FileMetadataDto firstDto = new FileMetadataDto(firstId, "avatars", "first.png");
        FileMetadataDto secondDto = new FileMetadataDto(secondId, "avatars", "second.png");
        when(fileMetadataRepository.findAvatarInfoByRelatedObjectIds(List.of(firstId, secondId)))
                .thenReturn(List.of(first, second));
        when(fileMetadataDtoConverter.toDto(first)).thenReturn(firstDto);
        when(fileMetadataDtoConverter.toDto(second)).thenReturn(secondDto);

        assertThat(provider.getFileMetadataDtos(List.of(firstId, secondId)))
                .containsExactlyInAnyOrderEntriesOf(
                        java.util.Map.of(firstId, firstDto, secondId, secondDto)
                );
    }
}
