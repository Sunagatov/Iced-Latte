package com.zufar.icedlatte.user.api.avatar;

import com.zufar.icedlatte.filestorage.aws.AwsCloudfrontInvalidator;
import com.zufar.icedlatte.filestorage.filemetadata.FileMetadataDeleter;
import com.zufar.icedlatte.filestorage.filemetadata.FileMetadataSaver;
import com.zufar.icedlatte.filestorage.file.FileUploader;
import com.zufar.icedlatte.filestorage.dto.FileMetadataDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Value("${spring.aws.buckets.user-avatar}")
    private String bucketName;
    private static final String AVATAR_NAME_PREFIX = "user-avatar-";

    private final FileUploader fileUploader;
    private final FileMetadataSaver fileMetadataSaver;
    private final FileMetadataDeleter fileMetadataDeleter;

    @Autowired(required = false)
    private AwsCloudfrontInvalidator cloudfrontInvalidator;

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public void uploadUserAvatar(final UUID userId, final MultipartFile file) {
        fileMetadataDeleter.deleteByRelatedObjectId(userId);
        String fileName = AVATAR_NAME_PREFIX + userId.toString();
        fileUploader.upload(file, bucketName, fileName);
        FileMetadataDto fileMetadataDto = new FileMetadataDto(userId, bucketName, fileName);
        fileMetadataSaver.save(fileMetadataDto);
        if (cloudfrontInvalidator != null) {
            cloudfrontInvalidator.invalidate(fileName);
        }
    }
}
