package com.zufar.icedlatte.common.filestorage.api;

import com.zufar.icedlatte.common.filestorage.converter.FileMetadataDtoConverter;
import com.zufar.icedlatte.common.filestorage.dto.FileMetadataDto;
import com.zufar.icedlatte.common.filestorage.entity.FileMetadata;
import com.zufar.icedlatte.common.filestorage.minio.MinioProvider;
import com.zufar.icedlatte.common.filestorage.repository.FileMetadataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MinioFileService {

    private final FileMetadataRepository fileMetadataRepository;
    private final FileMetadataDtoConverter fileMetadataDtoConverter;
    private final MinioProvider minioProvider;

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public FileMetadataDto save(final FileMetadataDto fileMetadataDto) {
        FileMetadata fileMetadata = fileMetadataDtoConverter.toEntity(fileMetadataDto);
        FileMetadata savedFileMetadata = fileMetadataRepository.save(fileMetadata);
        return fileMetadataDtoConverter.toDto(savedFileMetadata);
    }

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED, readOnly = true)
    public Optional<FileMetadataDto> getFileMetadataDto(final UUID relatedObjectId) {
        return fileMetadataRepository.findAvatarInfoByRelatedObjectId(relatedObjectId)
                .map(fileMetadataDtoConverter::toDto);
    }

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public void deleteByRelatedObjectId(final UUID relatedObjectId) {
        fileMetadataRepository.deleteByRelatedObjectId(relatedObjectId);
    }

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public void saveAll(final List<FileMetadataDto> fileMetadataDtos) {
        List<FileMetadata> fileMetadataList = fileMetadataDtoConverter.toEntityList(fileMetadataDtos);
        fileMetadataRepository.saveAll(fileMetadataList);
    }

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public void uploadProductImagesToDb(String bucketName) {
        List<FileMetadataDto> fileMetadataDtos = minioProvider.getProductImagesFromMinio(bucketName);
        this.saveAll(fileMetadataDtos);
    }
}
