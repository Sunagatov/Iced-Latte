package com.zufar.icedlatte.user.service.avatar;

import com.zufar.icedlatte.filestorage.FileStorageService;
import com.zufar.icedlatte.filestorage.aws.AwsCloudFrontInvalidator;
import com.zufar.icedlatte.filestorage.dto.FileMetadataDto;
import com.zufar.icedlatte.filestorage.exception.FileUploadException;
import com.zufar.icedlatte.user.exception.InvalidAvatarFileTypeException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
public class UserAvatarUploader {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp"
    );

    private final FileStorageService fileStorageService;
    private final ObjectProvider<AwsCloudFrontInvalidator> cloudfrontInvalidator;

    public UserAvatarUploader(FileStorageService fileStorageService,
                              ObjectProvider<AwsCloudFrontInvalidator> cloudfrontInvalidator) {
        this.fileStorageService = fileStorageService;
        this.cloudfrontInvalidator = cloudfrontInvalidator;
    }

    @Value("${spring.aws.buckets.user-avatar:}")
    private String bucketName;
    private static final String AVATAR_NAME_PREFIX = "user-avatar-";

    @Transactional(propagation = Propagation.REQUIRED,
            isolation = Isolation.READ_COMMITTED)
    public void uploadUserAvatar(final UUID userId, final MultipartFile file) {
        String contentType = normalizeContentType(file);
        validateAvatarFile(userId, file, contentType);

        String fileName = avatarFileName(userId);
        uploadAvatarFile(file, userId, fileName);
        invalidateAvatarCache(fileName);
    }

    private void validateAvatarFile(UUID userId,
                                    MultipartFile file,
                                    String contentType) {
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            log.warn("avatar.upload.rejected: reason=invalid_content_type, userId={}", userId);
            throw new InvalidAvatarFileTypeException(file.getContentType(), ALLOWED_CONTENT_TYPES);
        }
        if (!hasValidImageSignature(file)) {
            log.warn("avatar.upload.rejected: reason=magic_bytes_mismatch, userId={}", userId);
            throw new InvalidAvatarFileTypeException(file.getContentType(), ALLOWED_CONTENT_TYPES);
        }
    }

    private String normalizeContentType(MultipartFile file) {
        String contentType = file.getContentType();
        return contentType == null ? "" : contentType.toLowerCase(java.util.Locale.ROOT);
    }

    private String avatarFileName(UUID userId) {
        return AVATAR_NAME_PREFIX + userId;
    }

    private void uploadAvatarFile(MultipartFile file,
                                  UUID userId,
                                  String fileName) {
        if (!fileStorageService.isEnabled()) {
            throw new FileUploadException(
                    fileName,
                    new IllegalStateException("File storage is not configured")
            );
        }
        fileStorageService.store(file, new FileMetadataDto(userId, bucketName, fileName));
    }

    private void invalidateAvatarCache(String fileName) {
        cloudfrontInvalidator.ifAvailable(invalidator -> invalidator.invalidate(fileName));
    }

    private static boolean hasValidImageSignature(MultipartFile file) {
        try (InputStream in = file.getInputStream()) {
            byte[] h = in.readNBytes(12);
            return isJpeg(h) || isPng(h) || isWebp(h);
        } catch (IOException ex) {
            return false;
        }
    }

    // FF D8 FF
    private static boolean isJpeg(byte[] h) {
        return h.length >= 3
                && (h[0] & 0xFF) == 0xFF
                && (h[1] & 0xFF) == 0xD8
                && (h[2] & 0xFF) == 0xFF;
    }

    // 89 50 4E 47 0D 0A 1A 0A
    private static boolean isPng(byte[] h) {
        return h.length >= 8
                && (h[0] & 0xFF) == 0x89
                && (h[1] & 0xFF) == 0x50
                && (h[2] & 0xFF) == 0x4E
                && (h[3] & 0xFF) == 0x47
                && (h[4] & 0xFF) == 0x0D
                && (h[5] & 0xFF) == 0x0A
                && (h[6] & 0xFF) == 0x1A
                && (h[7] & 0xFF) == 0x0A;
    }

    // 52 49 46 46 ?? ?? ?? ?? 57 45 42 50  (RIFF....WEBP)
    private static boolean isWebp(byte[] h) {
        return h.length >= 12
                && (h[0] & 0xFF) == 0x52  // R
                && (h[1] & 0xFF) == 0x49  // I
                && (h[2] & 0xFF) == 0x46  // F
                && (h[3] & 0xFF) == 0x46  // F
                && (h[8] & 0xFF) == 0x57  // W
                && (h[9] & 0xFF) == 0x45  // E
                && (h[10] & 0xFF) == 0x42 // B
                && (h[11] & 0xFF) == 0x50; // P
    }
}
