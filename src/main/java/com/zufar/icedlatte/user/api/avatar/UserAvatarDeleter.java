package com.zufar.icedlatte.user.api.avatar;

import com.zufar.icedlatte.common.filestorage.MinioObjectDeleter;
import com.zufar.icedlatte.user.api.SingleUserProvider;
import com.zufar.icedlatte.user.entity.AvatarInfo;
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

    public void delete(final UUID userId) {
        UserEntity userEntity = singleUserProvider.getUserEntityById(userId);
        AvatarInfo avatarInfo = userEntity.getAvatarInfo();
        String bucketName = avatarInfo.getBucketName();
        String fileName = avatarInfo.getFileName();
        minioObjectDeleter.deleteFile(bucketName, fileName);
        deleteUserAvatar(userEntity);
    }

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    private void deleteUserAvatar(final UserEntity userEntity) {
        AvatarInfo avatarinfo = userEntity.getAvatarInfo();
        avatarinfo.setAvatarUrl(null);
    }
}
