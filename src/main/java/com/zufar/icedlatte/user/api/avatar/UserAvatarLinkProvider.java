package com.zufar.icedlatte.user.api.avatar;

import com.zufar.icedlatte.filestorage.file.FileProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserAvatarLinkProvider {

    private final FileProvider fileProvider;

    public String getLink(final UUID userId) {
        try {
            return fileProvider.getRelatedObjectUrl(userId)
                    .orElseGet(() -> {
                        log.warn("File with id = {} was not found.", userId);
                        return "default file";
                    });
        } catch (Throwable exception) {
            log.error("FileProvider error", exception);
        }
        return "default file";
    }
}
