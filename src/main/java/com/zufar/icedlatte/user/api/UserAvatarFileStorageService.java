package com.zufar.icedlatte.user.api;

import com.zufar.icedlatte.common.filestorage.MinioObjectDeleter;
import com.zufar.icedlatte.common.filestorage.MinioObjectUploader;
import com.zufar.icedlatte.common.filestorage.MinioTemporaryLinkReceiver;
import com.zufar.icedlatte.openapi.dto.UserDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserAvatarFileStorageService {

    @Value("${spring.minio.buckets.user-avatar}")
    private String bucketName;
    private static final String avatarNamePrefix = "user-avatar-";
    private final MinioObjectUploader minioObjectUploader;
    private final MinioObjectDeleter minioObjectDeleter;
    private final UserAvatarUrlService userAvatarUrlService;
    private final MinioTemporaryLinkReceiver minioTemporaryLinkReceiver;

    public UserDto uploadUserAvatar(final UUID userId, final MultipartFile file) {
        String fileName = userAvatarNameCoder(userId);
        String avatarUrl = minioObjectUploader.uploadFile(file, bucketName, fileName);
        return userAvatarUrlService.updateUserUrl(avatarUrl, userId);
    }

    public String getUserAvatarUrl(final UUID userId) {
        String avatarUrl = userAvatarUrlService.getAvatarUrlByUserId(userId);
        userAvatarValidator(avatarUrl);
        return avatarUrl;
    }

    private void userAvatarValidator(final String avatarUrl) {
        if (avatarUrl == null || avatarUrl.isEmpty()) {
            throw new RuntimeException("User avatar not found.");
        }
    }

    public String getUserAvatarTemporaryLink(final UUID userId) {
        String fileName = userAvatarNameCoder(userId);
        return minioTemporaryLinkReceiver.generatePresignedUrl(bucketName, fileName).toString();
    }

    public void deleteUserAvatar(final UUID userId) {
        String fileName = userAvatarNameCoder(userId);
        minioObjectDeleter.deleteFile(fileName, bucketName);
        userAvatarUrlService.deleteUserAvatar(userId);
    }

    private String userAvatarNameCoder(UUID userId) {
        return avatarNamePrefix + userId.toString();
    }
}
