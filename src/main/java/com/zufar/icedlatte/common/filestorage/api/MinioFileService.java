package com.zufar.icedlatte.common.filestorage.api;

import com.zufar.icedlatte.common.filestorage.converter.FileMetadataDtoConverter;
import com.zufar.icedlatte.common.filestorage.dto.FileMetadataDto;
import com.zufar.icedlatte.common.filestorage.entity.FileMetadata;
import com.zufar.icedlatte.common.filestorage.repository.FileMetadataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MinioFileService {

    private final FileMetadataRepository fileMetadataRepository;
    private final FileMetadataDtoConverter fileMetadataDtoConverter;

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public FileMetadataDto save (final FileMetadataDto fileMetadataDto) {
        FileMetadata fileMetadata = fileMetadataDtoConverter.toEntity(fileMetadataDto);
        FileMetadata savedFileMetadata = fileMetadataRepository.save(fileMetadata);
        return fileMetadataDtoConverter.toDto(savedFileMetadata);
    }

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED, readOnly = true)
    public FileMetadataDto getFileMetadataDto(final UUID relatedObjectId) {
        FileMetadata fileMetadata = fileMetadataRepository.findAvatarInfoByRelatedObjectId(relatedObjectId)
                .orElse(null);
        return fileMetadataDtoConverter.toDto(fileMetadata);
    }

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public void deleteByRelatedObjectId(final UUID relatedObjectId) {
        fileMetadataRepository.deleteByRelatedObjectId(relatedObjectId);
    }
}
