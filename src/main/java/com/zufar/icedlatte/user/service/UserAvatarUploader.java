package com.zufar.icedlatte.user.service;

import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.zufar.icedlatte.common.exception.BadRequestException;
import com.zufar.icedlatte.common.turnstile.TurnstileProperties;
import com.zufar.icedlatte.common.turnstile.TurnstileVerificationRequest;
import com.zufar.icedlatte.common.turnstile.TurnstileVerifier;
import com.zufar.icedlatte.filestorage.api.FileStorageWriterApi;
import com.zufar.icedlatte.filestorage.api.dto.FileMetadataDto;
import com.zufar.icedlatte.user.exception.InvalidAvatarFileTypeException;
import com.zufar.icedlatte.user.exception.UserAvatarUploadException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserAvatarUploader {

    private final FileStorageWriterApi fileStorageWriterApi;
    private final TurnstileVerifier turnstileVerifier;
    private final TurnstileProperties turnstileProperties;
    private final AvatarUploadAdmission admission;
    private final AvatarUploadLifecycleService lifecycleService;

    @Value("${spring.aws.buckets.user-avatar:}")
    private String bucketName;

    @Value("${avatar.max-bytes:5242880}")
    private long maxBytes;

    @Transactional
    public void uploadUserAvatar(final UUID userId, final MultipartFile file, @Nullable final String turnstileToken) {
        uploadUserAvatar(userId, file, turnstileToken, null);
    }

    @Transactional
    public void uploadUserAvatar(
            final UUID userId,
            final MultipartFile file,
            @Nullable final String turnstileToken,
            @Nullable final String remoteIp) {
        if (turnstileProperties.avatarEnabled()) {
            turnstileVerifier.verify(TurnstileVerificationRequest.forAction(turnstileToken, remoteIp, "avatar"));
        }

        String contentType = AvatarContentTypes.normalize(file);
        validateAvatarFile(userId, file, contentType);

        admission.lockUser(userId);
        admission.reserve(userId);
        lifecycleService.invalidateUserUploadsAfterAvatarDelete(userId);

        String fileName = avatarFileName(userId, contentType);
        uploadAvatarFile(file, userId, fileName);
    }

    private void validateAvatarFile(UUID userId, MultipartFile file, String contentType) {
        if (file.getSize() > maxBytes) {
            throw new BadRequestException("Avatar upload must be at most " + maxBytes + " bytes.");
        }
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
        return AvatarUploadStorageLayout.processedPrefix(userId, UUID.randomUUID()) + "avatar"
                + AvatarContentTypes.extensionFor(contentType);
    }

    private void uploadAvatarFile(MultipartFile file, UUID userId, String fileName) {
        if (!fileStorageWriterApi.isEnabled() || !StringUtils.hasText(bucketName)) {
            throw new UserAvatarUploadException(userId, fileName);
        }
        fileStorageWriterApi.store(file, new FileMetadataDto(userId, bucketName, fileName));
    }
}
