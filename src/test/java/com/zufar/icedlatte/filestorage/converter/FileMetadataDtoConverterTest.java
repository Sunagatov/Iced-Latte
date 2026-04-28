package com.zufar.icedlatte.filestorage.converter;

import com.zufar.icedlatte.filestorage.dto.FileMetadataDto;
import com.zufar.icedlatte.filestorage.entity.FileMetadata;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FileMetadataDtoConverter")
class FileMetadataDtoConverterTest {

    private final FileMetadataDtoConverter converter = Mappers.getMapper(FileMetadataDtoConverter.class);

    @Test
    @DisplayName("maps entity to dto")
    void toDto_mapsEntityFields() {
        UUID relatedObjectId = UUID.randomUUID();
        FileMetadata entity = FileMetadata.builder()
                .relatedObjectId(relatedObjectId)
                .bucketName("avatars")
                .fileName("user-1.png")
                .build();

        FileMetadataDto dto = converter.toDto(entity);

        assertThat(dto.relatedObjectId()).isEqualTo(relatedObjectId);
        assertThat(dto.bucketName()).isEqualTo("avatars");
        assertThat(dto.fileName()).isEqualTo("user-1.png");
    }

    @Test
    @DisplayName("maps dto to entity")
    void toEntity_mapsDtoFields() {
        UUID relatedObjectId = UUID.randomUUID();
        FileMetadataDto dto = new FileMetadataDto(relatedObjectId, "products", "product-1.jpg");

        FileMetadata entity = converter.toEntity(dto);

        assertThat(entity.getRelatedObjectId()).isEqualTo(relatedObjectId);
        assertThat(entity.getBucketName()).isEqualTo("products");
        assertThat(entity.getFileName()).isEqualTo("product-1.jpg");
    }

    @Test
    @DisplayName("maps dto lists preserving order")
    void toEntityList_mapsListInOrder() {
        FileMetadataDto first = new FileMetadataDto(UUID.randomUUID(), "avatars", "first.png");
        FileMetadataDto second = new FileMetadataDto(UUID.randomUUID(), "avatars", "second.png");

        List<FileMetadata> entities = converter.toEntityList(List.of(first, second));

        assertThat(entities)
                .extracting(FileMetadata::getFileName)
                .containsExactly("first.png", "second.png");
    }
}
