package com.zufar.icedlatte.filestorage.filemetadata;

import com.zufar.icedlatte.filestorage.repository.FileMetadataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileMetadataDeleter {

    private final FileMetadataRepository fileMetadataRepository;

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public void deleteByRelatedObjectId(final UUID relatedObjectId) {
        fileMetadataRepository.deleteByRelatedObjectId(relatedObjectId);
    }
}
