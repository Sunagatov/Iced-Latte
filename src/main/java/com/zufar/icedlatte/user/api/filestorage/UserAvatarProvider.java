package com.zufar.icedlatte.user.api.filestorage;

import com.zufar.icedlatte.common.filestorage.MinioTemporaryLinkReceiver;
import com.zufar.icedlatte.user.api.UserAvatarUrlService;
import com.zufar.icedlatte.user.dto.UserAvatarDto;
import com.zufar.icedlatte.user.exception.EmptyUserAvatarException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserAvatarProvider {

    private final UserAvatarUrlService userAvatarUrlService;
    private final MinioTemporaryLinkReceiver minioTemporaryLinkReceiver;

    public UserAvatarDto getUserAvatarDto(final UUID userId) {
        String avatarUrl = userAvatarUrlService.getAvatarUrlByUserId(userId);
        userAvatarValidator(avatarUrl, userId);
        String fileName = extractFileName(avatarUrl);
        String bucketName = extractFolderName(avatarUrl);
        return new UserAvatarDto(bucketName, fileName);
    }

    private void userAvatarValidator(final String avatarUrl, UUID userId) {
        if (avatarUrl == null || avatarUrl.isEmpty()) {
            log.error("User avatar not found for userId " + userId);
            throw new EmptyUserAvatarException(userId);
        }
    }

    public String getUserAvatarTemporaryLink(final UUID userId) {
        UserAvatarDto userAvatarDto = getUserAvatarDto(userId);
        String bucketName = userAvatarDto.bucketName();
        String fileName = userAvatarDto.fileName();
        return minioTemporaryLinkReceiver.generatePresignedUrl(bucketName, fileName).toString();
    }

    private String extractFolderName(String input) {
        int lastSlashIndex = input.lastIndexOf('/');
        int secondToLastSlashIndex = input.lastIndexOf('/', lastSlashIndex - 1);
        return input.substring(secondToLastSlashIndex + 1, lastSlashIndex);
    }

    private String extractFileName(String input) {
        int lastSlashIndex = input.lastIndexOf('/');
        return input.substring(lastSlashIndex + 1);
    }
}
