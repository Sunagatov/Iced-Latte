package com.zufar.icedlatte.user.api;

import com.zufar.icedlatte.common.filestorage.MinioObjectDownloader;
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
    private final MinioObjectDownloader minioObjectDownloader;

    public void uploadUserAvatar(final UUID userId, final MultipartFile file) {
        String fileName = userAvatarNameCoder(userId);
        minioObjectDownloader.saveFile(fileName, file, bucketName);
    }

    public String userAvatarNameCoder(UUID userId) {
        return avatarNamePrefix + userId.toString();
    }
}
