package com.zufar.icedlatte.user.api.avatar;

import com.zufar.icedlatte.common.entity.FileMetadata;
import com.zufar.icedlatte.common.filestorage.MinioObjectDeleter;
import com.zufar.icedlatte.user.api.SingleUserProvider;
import com.zufar.icedlatte.user.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserAvatarDeleter {

    private final MinioObjectDeleter minioObjectDeleter;
    private final SingleUserProvider singleUserProvider;
    private final UserFileService userFileService;

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public void delete(final UUID userId) {
        UserEntity userEntity = singleUserProvider.getUserEntityById(userId);
        FileMetadata fileMetadata = userEntity.getFileMetadata();
        final String bucketName = fileMetadata.getBucketName();
        final String fileName = fileMetadata.getFileName();
        minioObjectDeleter.deleteFile(bucketName, fileName);
        userFileService.delete(fileMetadata.getFileId());
        userEntity.setFileMetadata(null);
    }
}
