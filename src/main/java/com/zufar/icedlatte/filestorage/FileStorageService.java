package com.zufar.icedlatte.filestorage;

import com.zufar.icedlatte.filestorage.converter.FileMetadataDtoConverter;
import com.zufar.icedlatte.filestorage.dto.FileMetadataDto;
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
        List<FileMetadataDto> fileMetadataList = objectStorage.listObjectKeys(bucketName).stream()
                .map(fileName -> toFileMetadata(fileName, bucketName))
                .flatMap(Optional::stream)
                .collect(Collectors.toMap(
                        FileMetadataDto::relatedObjectId,
                        metadata -> metadata,
                        this::selectPreferredMetadata
                ))
                .values()
                .stream()
                .toList();
        fileMetadataRepository.deleteByBucketName(bucketName);
        fileMetadataRepository.saveAll(fileMetadataDtoConverter.toEntityList(fileMetadataList));
    }

    private Optional<FileMetadataDto> findMetadata(UUID relatedObjectId) {
        return findMetadata(List.of(relatedObjectId)).values().stream()
                .findFirst();
    }

    private Map<UUID, FileMetadataDto> findMetadata(List<UUID> relatedObjectIds) {
        return fileMetadataRepository.findAvatarInfoByRelatedObjectIds(relatedObjectIds)
                .stream()
                .map(fileMetadataDtoConverter::toDto)
                .collect(Collectors.toMap(
                        FileMetadataDto::relatedObjectId,
                        metadata -> metadata,
                        this::selectPreferredMetadata
                ));
    }

    private FileMetadataDto selectPreferredMetadata(FileMetadataDto first,
                                                    FileMetadataDto second) {
        FileMetadataDto preferred = compareMetadata(first, second) <= 0 ? first : second;
        FileMetadataDto skipped = preferred == first ? second : first;
        log.warn("storage.metadata.duplicate_related_object: objectId={}, selected={}, skipped={}",
                preferred.relatedObjectId(), preferred.fileName(), skipped.fileName());
        return preferred;
    }

    private int compareMetadata(FileMetadataDto first, FileMetadataDto second) {
        int rankComparison = Integer.compare(metadataRank(first), metadataRank(second));
        if (rankComparison != 0) {
            return rankComparison;
        }
        return first.fileName().compareTo(second.fileName());
    }

    private int metadataRank(FileMetadataDto metadata) {
        String fileName = metadata.fileName().toLowerCase();
        String baseName = fileName.substring(fileName.lastIndexOf('/') + 1);
        switch (baseName) {
            case "card_logo.webp" -> {
                return 0;
            }
            case "card_logo.png" -> {
                return 1;
            }
            case "card_logo.jpg", "card_logo.jpeg" -> {
                return 2;
            }
        }
        if (fileName.endsWith(".webp")) {
            return 3;
        }
        if (fileName.endsWith(".png")) {
            return 4;
        }
        if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
            return 5;
        }
        return 6;
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
