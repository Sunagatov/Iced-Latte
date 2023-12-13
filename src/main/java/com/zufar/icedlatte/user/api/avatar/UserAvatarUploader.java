package com.zufar.icedlatte.user.api.avatar;

import com.zufar.icedlatte.common.filestorage.MinioObjectUploader;
import com.zufar.icedlatte.user.dto.AvatarInfoDto;
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
    private final UserAvatarProvider userAvatarProvider;
    private final AvatarInfoSaver avatarInfoSaver;

    public AvatarInfoDto uploadUserAvatar(final UUID userId, final MultipartFile file) {
        String fileName = userAvatarNameCoder(userId);
        minioObjectUploader.uploadFile(file, bucketName, fileName);
        String avatarUrl = userAvatarProvider.getNewTemporaryAvatarUrl(bucketName, fileName);
        AvatarInfoDto avatarInfoDto = new AvatarInfoDto(bucketName, fileName, avatarUrl);
        return avatarInfoSaver.save(avatarInfoDto, userId);
    }

    private String userAvatarNameCoder(UUID userId) {
        return avatarNamePrefix + userId.toString();
    }
}
