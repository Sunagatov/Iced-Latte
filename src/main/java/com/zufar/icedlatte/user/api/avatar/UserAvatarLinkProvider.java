package com.zufar.icedlatte.user.api.avatar;

import com.zufar.icedlatte.filestorage.file.FileProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserAvatarLinkProvider {

    private final FileProvider fileProvider;

    public String getLink(final UUID userId) {
        return fileProvider.getRelatedObjectUrl(userId)
                .orElse(null);
    }
}
