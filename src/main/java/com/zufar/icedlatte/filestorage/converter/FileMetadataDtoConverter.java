package com.zufar.icedlatte.filestorage.converter;

import com.zufar.icedlatte.filestorage.api.dto.FileMetadataDto;
import com.zufar.icedlatte.filestorage.entity.FileMetadata;
import org.mapstruct.IterableMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@SuppressWarnings("NullableProblems")
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface FileMetadataDtoConverter {

    @Named("toFileMetadataDto")
    FileMetadataDto toDto(final FileMetadata entity);

    @Named("toFileMetadata")
    @Mapping(target = "fileId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    FileMetadata toEntity(final FileMetadataDto dto);

    @Named("toFileMetadataList")
    @IterableMapping(qualifiedByName = "toFileMetadata")
    List<FileMetadata> toEntityList(final List<FileMetadataDto> dtoList);
}
