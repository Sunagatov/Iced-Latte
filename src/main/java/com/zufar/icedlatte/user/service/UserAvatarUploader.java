package com.zufar.icedlatte.user.service;

import java.util.UUID;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.zufar.icedlatte.common.turnstile.TurnstileProperties;
import com.zufar.icedlatte.common.turnstile.TurnstileVerifier;
import com.zufar.icedlatte.filestorage.api.FileCacheInvalidationApi;
import com.zufar.icedlatte.filestorage.api.FileStorageWriterApi;
import com.zufar.icedlatte.filestorage.api.dto.FileMetadataDto;
import com.zufar.icedlatte.user.exception.InvalidAvatarFileTypeException;
import com.zufar.icedlatte.user.exception.UserAvatarUploadException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class UserAvatarUploader {

    private static final String AVATAR_NAME_PREFIX = "user-avatar-";

    private final FileStorageWriterApi fileStorageWriterApi;
    private final ObjectProvider<FileCacheInvalidationApi> cacheInvalidator;
    private final TurnstileVerifier turnstileVerifier;
    private final TurnstileProperties turnstileProperties;

    public UserAvatarUploader(
            FileStorageWriterApi fileStorageWriterApi,
            ObjectProvider<FileCacheInvalidationApi> cacheInvalidator,
            TurnstileVerifier turnstileVerifier,
            TurnstileProperties turnstileProperties) {
        this.fileStorageWriterApi = fileStorageWriterApi;
        this.cacheInvalidator = cacheInvalidator;
        this.turnstileVerifier = turnstileVerifier;
        this.turnstileProperties = turnstileProperties;
    }

    @Value("${spring.aws.buckets.user-avatar:}")
    private String bucketName;

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public void uploadUserAvatar(final UUID userId, final MultipartFile file, final String turnstileToken) {
        if (turnstileProperties.avatarEnabled()) {
            turnstileVerifier.verify(turnstileToken);
        }

        String contentType = AvatarContentTypes.normalize(file);
        validateAvatarFile(userId, file, contentType);

        String fileName = avatarFileName(userId, contentType);
        uploadAvatarFile(file, userId, fileName);
        invalidateAvatarCache(fileName);
    }

    private void validateAvatarFile(UUID userId, MultipartFile file, String contentType) {
        if (!AvatarContentTypes.ALLOWED_CONTENT_TYPES.contains(contentType)) {
            log.warn("avatar.upload.rejected: reason=invalid_content_type, userId={}", userId);
            throw new InvalidAvatarFileTypeException(file.getContentType(), AvatarContentTypes.ALLOWED_CONTENT_TYPES);
        }
        if (AvatarContentTypes.detect(file).filter(contentType::equals).isEmpty()) {
            log.warn("avatar.upload.rejected: reason=magic_bytes_mismatch, userId={}", userId);
            throw new InvalidAvatarFileTypeException(file.getContentType(), AvatarContentTypes.ALLOWED_CONTENT_TYPES);
        }
    }

    private String avatarFileName(UUID userId, String contentType) {
        return AVATAR_NAME_PREFIX + userId + AvatarContentTypes.extensionFor(contentType);
    }

    private void uploadAvatarFile(MultipartFile file, UUID userId, String fileName) {
        if (!fileStorageWriterApi.isEnabled()) {
            throw new UserAvatarUploadException(userId, fileName);
        }
        fileStorageWriterApi.store(file, new FileMetadataDto(userId, bucketName, fileName));
    }

    private void invalidateAvatarCache(String fileName) {
        cacheInvalidator.ifAvailable(invalidator -> invalidator.invalidate(fileName));
    }
}
