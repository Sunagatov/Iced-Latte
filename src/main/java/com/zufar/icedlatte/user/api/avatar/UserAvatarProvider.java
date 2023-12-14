package com.zufar.icedlatte.user.api.avatar;

import com.zufar.icedlatte.common.filestorage.MinioTemporaryLinkReceiver;
import com.zufar.icedlatte.user.converter.AvatarInfoDtoConverter;
import com.zufar.icedlatte.user.dto.AvatarInfoDto;
import com.zufar.icedlatte.user.entity.AvatarInfo;
import com.zufar.icedlatte.user.exception.EmptyUserAvatarException;
import com.zufar.icedlatte.user.repository.AvatarInfoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserAvatarProvider {

    @Value("${spring.minio.expiration-time.avatar-link}")
    private String expirationTime;

    private final MinioTemporaryLinkReceiver minioTemporaryLinkReceiver;
    private final AvatarInfoRepository avatarInfoRepository;
    private final AvatarInfoDtoConverter avatarInfoDtoConverter;
    private final AvatarInfoSaver avatarInfoSaver;

    public String getNewTemporaryAvatarUrl(final String bucketName, final String fileName) {
        return minioTemporaryLinkReceiver.generatePresignedUrl(bucketName, fileName).toString();
    }

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public AvatarInfoDto getAvatarInfoDto(final UUID userId) {
        AvatarInfo avatarInfo = avatarInfoRepository.findAvatarInfoByUserId(userId)
                .orElseThrow(() -> new EmptyUserAvatarException(userId));

        AvatarInfo validatedAvatarInfo = avatarInfoValidation(avatarInfo, userId);
        return avatarInfoDtoConverter.toDto(validatedAvatarInfo);
    }

    private AvatarInfo avatarInfoValidation(final AvatarInfo avatarInfo, final UUID userId) {
        final String avatarUrl = avatarInfo.getAvatarUrl();
        if (avatarUrl == null || avatarUrl.isEmpty()) {
            throw new EmptyUserAvatarException(userId);
        }

        final OffsetDateTime updatedAt = avatarInfo.getUpdatedAt()
                .plus(Duration.parse(expirationTime))
                .plus(Duration.ofHours(1)); // extra time for validation
        final OffsetDateTime now = OffsetDateTime.now();
        if (updatedAt.isBefore(now)) {
            String bucketName = avatarInfo.getBucketName();
            String fileName = avatarInfo.getFileName();
            String newUrl = getNewTemporaryAvatarUrl(bucketName, fileName);
            return avatarInfoSaver.update(avatarInfo, newUrl);
        }

        return avatarInfo;
    }
}
