package com.zufar.icedlatte.user.api.avatar;

import com.zufar.icedlatte.common.converter.FileMetadataDtoConverter;
import com.zufar.icedlatte.common.dto.FileMetadataDto;
import com.zufar.icedlatte.common.entity.FileMetadata;
import com.zufar.icedlatte.common.filestorage.MinioTemporaryLinkReceiver;
import com.zufar.icedlatte.user.exception.UserAvatarNotFoundException;
import com.zufar.icedlatte.user.repository.FileMetadataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserAvatarProvider {

    private final MinioTemporaryLinkReceiver minioTemporaryLinkReceiver;
    private final FileMetadataRepository fileMetadataRepository;
    private final FileMetadataDtoConverter fileMetadataDtoConverter;

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED, readOnly = true)
    public String getAvatarUrl(final UUID userId) {
        FileMetadataDto fileMetadataDto = getAvatarInfoDto(userId);
        return getNewTemporaryAvatarUrl(fileMetadataDto);
    }

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED, readOnly = true)
    public FileMetadataDto getAvatarInfoDto(final UUID userId) {
        FileMetadata fileMetadata = fileMetadataRepository.findAvatarInfoByUserId(userId)
                .orElseThrow(() -> new UserAvatarNotFoundException(userId));
        return fileMetadataDtoConverter.toDto(fileMetadata);
    }

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED, readOnly = true)
    public String getNewTemporaryAvatarUrl(final FileMetadataDto fileMetadata) {
        return minioTemporaryLinkReceiver.generatePresignedUrlAsString(fileMetadata);
    }
}
