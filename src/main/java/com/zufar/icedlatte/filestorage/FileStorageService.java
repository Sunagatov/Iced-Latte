package com.zufar.icedlatte.filestorage;

import com.zufar.icedlatte.filestorage.converter.FileMetadataDtoConverter;
import com.zufar.icedlatte.filestorage.dto.FileMetadataDto;
import com.zufar.icedlatte.filestorage.entity.FileMetadata;
import com.zufar.icedlatte.filestorage.repository.FileMetadataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageService {

    private final ObjectStorage objectStorage;
    private final FileMetadataRepository fileMetadataRepository;
    private final FileMetadataDtoConverter fileMetadataDtoConverter;

    public boolean isEnabled() {
        return objectStorage.isConfigured();
    }

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public void store(MultipartFile file, FileMetadataDto fileMetadataDto) {
        objectStorage.upload(file, fileMetadataDto.bucketName(), fileMetadataDto.fileName());
        fileMetadataRepository.deleteByRelatedObjectId(fileMetadataDto.relatedObjectId());
        fileMetadataRepository.save(fileMetadataDtoConverter.toEntity(fileMetadataDto));
    }

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public void storeDirectory(String bucketName, String directoryPath) throws IOException {
        objectStorage.uploadDirectory(bucketName, directoryPath);
    }

    @Transactional(propagation = Propagation.REQUIRED,
            isolation = Isolation.READ_COMMITTED,
            readOnly = true)
    public Optional<String> findFileUrl(UUID relatedObjectId) {
        return findMetadata(relatedObjectId)
                .flatMap(objectStorage::getUrl);
    }

    @Transactional(propagation = Propagation.REQUIRED,
            isolation = Isolation.READ_COMMITTED,
            readOnly = true)
    public Map<UUID, String> findFileUrls(List<UUID> relatedObjectIds) {
        return findMetadata(relatedObjectIds).entrySet().stream()
                .flatMap(entry -> objectStorage.getUrl(entry.getValue())
                        .map(url -> Map.entry(entry.getKey(), url))
                        .stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public void deleteFile(UUID relatedObjectId) {
        findMetadata(relatedObjectId).ifPresent(fileMetadataDto -> {
            objectStorage.delete(fileMetadataDto);
            fileMetadataRepository.deleteByRelatedObjectId(relatedObjectId);
            log.info("file.deleted: objectId={}", relatedObjectId);
        });
    }

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public void refreshBucketIndex(String bucketName) {
        List<FileMetadataDto> fileMetadataDtos = objectStorage.listObjectKeys(bucketName).stream()
                .map(fileName -> toFileMetadata(fileName, bucketName))
                .flatMap(Optional::stream)
                .toList();
        fileMetadataRepository.deleteByBucketName(bucketName);
        fileMetadataRepository.saveAll(fileMetadataDtoConverter.toEntityList(fileMetadataDtos));
    }

    private Optional<FileMetadataDto> findMetadata(UUID relatedObjectId) {
        return fileMetadataRepository.findAvatarInfoByRelatedObjectId(relatedObjectId)
                .map(fileMetadataDtoConverter::toDto);
    }

    private Map<UUID, FileMetadataDto> findMetadata(List<UUID> relatedObjectIds) {
        return fileMetadataRepository.findAvatarInfoByRelatedObjectIds(relatedObjectIds)
                .stream()
                .collect(Collectors.toMap(
                        FileMetadata::getRelatedObjectId,
                        fileMetadataDtoConverter::toDto
                ));
    }

    private Optional<FileMetadataDto> toFileMetadata(String fileName, String bucketName) {
        String[] parts = fileName.split("/");
        String[] packageName = parts[0].split("_");
        if (packageName.length < 2) {
            log.warn("storage.key.skipped: key={}", fileName);
            return Optional.empty();
        }
        try {
            return Optional.of(new FileMetadataDto(UUID.fromString(packageName[1]), bucketName, fileName));
        } catch (IllegalArgumentException ex) {
            log.warn("storage.key.invalid_uuid: key={}", fileName);
            return Optional.empty();
        }
    }
}
