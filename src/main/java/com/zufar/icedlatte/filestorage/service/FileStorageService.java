package com.zufar.icedlatte.filestorage.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.zufar.icedlatte.filestorage.api.FileStorageApi;
import com.zufar.icedlatte.filestorage.api.dto.FileMetadataDto;
import com.zufar.icedlatte.filestorage.config.FileDeletionOutboxProperties;
import com.zufar.icedlatte.filestorage.converter.FileMetadataDtoConverter;
import com.zufar.icedlatte.filestorage.exception.FileListException;
import com.zufar.icedlatte.filestorage.exception.FileUploadException;
import com.zufar.icedlatte.filestorage.repository.FileDeletionOutboxRepository;
import com.zufar.icedlatte.filestorage.repository.FileMetadataRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageService implements FileStorageApi {

    private final ObjectStorage objectStorage;
    private final FileMetadataRepository fileMetadataRepository;
    private final FileMetadataDtoConverter fileMetadataDtoConverter;
    private final FileDeletionOutboxRepository fileDeletionOutboxRepository;
    private final FileDeletionOutboxProperties fileDeletionOutboxProperties;

    @Override
    public boolean isEnabled() {
        return objectStorage.isConfigured();
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public void store(MultipartFile file, FileMetadataDto fileMetadataDto) {
        requireStorageEnabledForUpload(fileMetadataDto.fileName());
        List<FileMetadataDto> existingMetadata = findAllMetadata(fileMetadataDto.relatedObjectId());
        objectStorage.upload(file, fileMetadataDto.bucketName(), fileMetadataDto.fileName());
        try {
            fileMetadataRepository.deleteByRelatedObjectId(fileMetadataDto.relatedObjectId());
            fileMetadataRepository.save(fileMetadataDtoConverter.toEntity(fileMetadataDto));
            enqueueReplacedObjects(existingMetadata, fileMetadataDto);
        } catch (RuntimeException ex) {
            deleteUploadedObjectAfterMetadataFailure(fileMetadataDto, ex);
            throw ex;
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public void storeDirectory(String bucketName, String directoryPath) throws IOException {
        requireStorageEnabledForUpload(directoryPath);
        objectStorage.uploadDirectory(bucketName, directoryPath);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED, readOnly = true)
    public Optional<String> findFileUrl(UUID relatedObjectId) {
        return findMetadata(relatedObjectId).flatMap(objectStorage::getUrl);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED, readOnly = true)
    public Map<UUID, String> findFileUrls(List<UUID> relatedObjectIds) {
        return findMetadata(relatedObjectIds).entrySet().stream()
                .flatMap(entry ->
                        objectStorage.getUrl(entry.getValue()).map(url -> Map.entry(entry.getKey(), url)).stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public void deleteFile(UUID relatedObjectId) {
        List<FileMetadataDto> fileMetadataList = findAllMetadata(relatedObjectId);
        if (!fileMetadataList.isEmpty()) {
            int maxAttempts = fileDeletionOutboxProperties.maxAttempts();

            int deletedRows = fileMetadataRepository.deleteByRelatedObjectId(relatedObjectId);
            if (deletedRows > 0) {
                fileMetadataList.forEach(fileMetadataDto ->
                        fileDeletionOutboxRepository.insertDeleteObjectEvent(fileMetadataDto, maxAttempts));
            }
            log.info("file.deleted: objectId={}", relatedObjectId);
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public void refreshBucketIndex(String bucketName) {
        requireStorageEnabledForListing(bucketName);
        List<FileMetadataDto> fileMetadataList = objectStorage.listObjectKeys(bucketName).stream()
                .map(fileName -> StorageKeyMetadataParser.parse(fileName, bucketName))
                .flatMap(Optional::stream)
                .collect(Collectors.toMap(
                        FileMetadataDto::relatedObjectId,
                        metadata -> metadata,
                        FileMetadataSelectionPolicy::selectPreferred))
                .values()
                .stream()
                .toList();
        fileMetadataRepository.deleteByBucketName(bucketName);
        fileMetadataRepository.saveAll(fileMetadataDtoConverter.toEntityList(fileMetadataList));
    }

    private Optional<FileMetadataDto> findMetadata(UUID relatedObjectId) {
        return findMetadata(List.of(relatedObjectId)).values().stream().findFirst();
    }

    private void requireStorageEnabledForUpload(String fileName) {
        if (!objectStorage.isConfigured()) {
            throw new FileUploadException(fileName, new IllegalStateException("File storage is not configured"));
        }
    }

    private void requireStorageEnabledForListing(String bucketName) {
        if (!objectStorage.isConfigured()) {
            throw new FileListException(bucketName, new IllegalStateException("File storage is not configured"));
        }
    }

    private void enqueueReplacedObjects(List<FileMetadataDto> existingMetadata, FileMetadataDto replacementMetadata) {
        int maxAttempts = fileDeletionOutboxProperties.maxAttempts();
        existingMetadata.stream()
                .filter(existing -> !sameObject(existing, replacementMetadata))
                .forEach(existing -> fileDeletionOutboxRepository.insertDeleteObjectEvent(existing, maxAttempts));
    }

    private boolean sameObject(FileMetadataDto first, FileMetadataDto second) {
        return first.bucketName().equals(second.bucketName())
                && first.fileName().equals(second.fileName());
    }

    private void deleteUploadedObjectAfterMetadataFailure(FileMetadataDto uploadedMetadata, RuntimeException failure) {
        try {
            objectStorage.delete(uploadedMetadata);
        } catch (RuntimeException cleanupFailure) {
            failure.addSuppressed(cleanupFailure);
            log.warn(
                    "file.upload.cleanup_failed: objectId={}, bucket={}, key={}",
                    uploadedMetadata.relatedObjectId(),
                    uploadedMetadata.bucketName(),
                    uploadedMetadata.fileName(),
                    cleanupFailure);
        }
    }

    private List<FileMetadataDto> findAllMetadata(UUID relatedObjectId) {
        return fileMetadataRepository.findByRelatedObjectIdIn(List.of(relatedObjectId)).stream()
                .map(fileMetadataDtoConverter::toDto)
                .toList();
    }

    private Map<UUID, FileMetadataDto> findMetadata(List<UUID> relatedObjectIds) {
        return fileMetadataRepository.findByRelatedObjectIdIn(relatedObjectIds).stream()
                .map(fileMetadataDtoConverter::toDto)
                .collect(Collectors.toMap(
                        FileMetadataDto::relatedObjectId,
                        metadata -> metadata,
                        FileMetadataSelectionPolicy::selectPreferred));
    }
}
