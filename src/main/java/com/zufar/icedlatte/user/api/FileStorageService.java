package com.zufar.icedlatte.user.api;

import com.zufar.icedlatte.common.filestorage.MinioObjectDeleter;
import com.zufar.icedlatte.common.filestorage.MinioObjectUploader;
import com.zufar.icedlatte.common.filestorage.MinioObjectGetter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileStorageService {

    @Value("${spring.minio.buckets.user-avatar}")
    private String bucketName;
    private static final String avatarNamePrefix = "user-avatar-";
    private final MinioObjectUploader minioObjectUploader;
    private final MinioObjectGetter minioObjectGetter;
    private final MinioObjectDeleter minioObjectDeleter;

    public void uploadUserAvatar(final UUID userId, final MultipartFile file) {
        String fileName = userAvatarNameCoder(userId);
        minioObjectUploader.saveFile(fileName, file, bucketName);
    }

    public MultipartFile getUserAvatar(final UUID userId) {
        String fileName = userAvatarNameCoder(userId);
        return minioObjectGetter.downloadFile(fileName, bucketName);
    }

    public void deleteUserAvatar(final UUID userId) {
        String fileName = userAvatarNameCoder(userId);
        minioObjectDeleter.deleteFile(fileName, bucketName);
    }

    private String userAvatarNameCoder(UUID userId) {
        return avatarNamePrefix + userId.toString();
    }
}
