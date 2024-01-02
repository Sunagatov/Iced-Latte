package com.zufar.icedlatte.filestorage.filemetadata;

import com.zufar.icedlatte.filestorage.converter.FileMetadataDtoConverter;
import com.zufar.icedlatte.filestorage.dto.FileMetadataDto;
import com.zufar.icedlatte.filestorage.entity.FileMetadata;
import com.zufar.icedlatte.filestorage.repository.FileMetadataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileMetadataProvider {

    private final FileMetadataRepository fileMetadataRepository;
    private final FileMetadataDtoConverter fileMetadataDtoConverter;

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED, readOnly = true)
    public Optional<FileMetadataDto> getFileMetadataDto(final UUID relatedObjectId) {
        return fileMetadataRepository.findAvatarInfoByRelatedObjectId(relatedObjectId)
                .map(fileMetadataDtoConverter::toDto);
    }

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED, readOnly = true)
    public List<FileMetadataDto> getAllFileMetadataDto(List<UUID> uuids) {
        List<FileMetadata> fileMetadata = fileMetadataRepository.findByRelatedObjectIds(uuids);
        return fileMetadata.stream()
                .map(fileMetadataDtoConverter::toDto)
                .toList();
    }
}
