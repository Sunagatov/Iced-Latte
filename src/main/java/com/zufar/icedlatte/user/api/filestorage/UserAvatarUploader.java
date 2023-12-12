package com.zufar.icedlatte.user.api.filestorage;

import com.zufar.icedlatte.common.filestorage.MinioObjectUploader;
import com.zufar.icedlatte.openapi.dto.UserDto;
import com.zufar.icedlatte.user.api.UserAvatarUrlService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserAvatarUploader {

    @Value("${spring.minio.buckets.user-avatar}")
    private String bucketName;
    private static final String avatarNamePrefix = "user-avatar-";

    private final MinioObjectUploader minioObjectUploader;
    private final UserAvatarUrlService userAvatarUrlService;

    public UserDto uploadUserAvatar(final UUID userId, final MultipartFile file) {
        String fileName = userAvatarNameCoder(userId);
        String avatarUrl = minioObjectUploader.uploadFile(file, bucketName, fileName);
        return userAvatarUrlService.updateUserUrl(avatarUrl, userId);
    }

    private String userAvatarNameCoder(UUID userId) {
        return avatarNamePrefix + userId.toString();
    }
}
