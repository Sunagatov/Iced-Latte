package com.zufar.icedlatte.user.api.avatar;

import com.zufar.icedlatte.common.dto.FileMetadataDto;
import com.zufar.icedlatte.common.filestorage.MinioObjectUploader;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserAvatarUploader {

    @Value("${spring.minio.buckets.user-avatar}")
    private String bucketName;
    private static final String avatarNamePrefix = "user-avatar-";

    private final MinioObjectUploader minioObjectUploader;
    private final UserFileService userFileService;

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public FileMetadataDto uploadUserAvatar(final UUID userId, final MultipartFile file) {
        String fileName =  avatarNamePrefix + userId.toString();
        minioObjectUploader.uploadFile(file, bucketName, fileName);
        FileMetadataDto fileMetadataDto = new FileMetadataDto(bucketName, fileName);
        return userFileService.save(fileMetadataDto, userId);
    }
}
