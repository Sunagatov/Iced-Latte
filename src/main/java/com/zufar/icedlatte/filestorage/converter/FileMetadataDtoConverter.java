package com.zufar.icedlatte.filestorage.converter;

import com.zufar.icedlatte.filestorage.dto.FileMetadataDto;
import com.zufar.icedlatte.filestorage.entity.FileMetadata;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface FileMetadataDtoConverter {

    @Named("toFileMetadataDto")
    FileMetadataDto toDto(final FileMetadata entity);

    @Named("toFileMetadata")
    FileMetadata toEntity(final FileMetadataDto dto);

    @Named("toFileMetadataList")
    List<FileMetadata> toEntityList(final List<FileMetadataDto> dtoList);
}
