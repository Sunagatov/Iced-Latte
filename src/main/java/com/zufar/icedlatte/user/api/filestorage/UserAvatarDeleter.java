package com.zufar.icedlatte.user.api.filestorage;

import com.zufar.icedlatte.common.filestorage.MinioObjectDeleter;
import com.zufar.icedlatte.user.api.UserAvatarUrlService;
import com.zufar.icedlatte.user.dto.UserAvatarDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserAvatarDeleter {

    private final MinioObjectDeleter minioObjectDeleter;
    private final UserAvatarUrlService userAvatarUrlService;
    private final UserAvatarProvider userAvatarProvider;

    public void deleteUserAvatar(final UUID userId) {
        UserAvatarDto avatarDto = userAvatarProvider.getUserAvatarDto(userId);
        String bucketName = avatarDto.bucketName();
        String fileName = avatarDto.fileName();
        minioObjectDeleter.deleteFile(bucketName, fileName);
        userAvatarUrlService.deleteUserAvatar(userId);
    }
}
