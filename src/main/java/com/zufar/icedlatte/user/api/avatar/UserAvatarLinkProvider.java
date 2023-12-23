package com.zufar.icedlatte.user.api.avatar;

import com.zufar.icedlatte.common.filestorage.api.FileProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserAvatarLinkProvider {

    private final FileProvider fileProvider;

    public String getLink(final UUID userId) {
        String avatarFileUrl = fileProvider.getRelatedObjectUrl(userId);
        return avatarFileUrl == null ? "default file" : avatarFileUrl;
    }
}
