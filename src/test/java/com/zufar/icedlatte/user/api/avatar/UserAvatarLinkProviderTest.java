package com.zufar.icedlatte.user.api.avatar;

import com.zufar.icedlatte.filestorage.file.FileProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserAvatarLinkProvider unit tests")
class UserAvatarLinkProviderTest {

    private static final String DEFAULT = "default file";

    @Mock private FileProvider fileProvider;
    @InjectMocks private UserAvatarLinkProvider provider;

    @Test
    @DisplayName("getLink returns URL when FileProvider has one")
    void getLink_found_returnsUrl() {
        UUID userId = UUID.randomUUID();
        when(fileProvider.getRelatedObjectUrl(userId)).thenReturn(Optional.of("https://cdn.example.com/avatar.jpg"));

        assertThat(provider.getLink(userId)).isEqualTo("https://cdn.example.com/avatar.jpg");
    }

    @Test
    @DisplayName("getLink returns default when FileProvider returns empty")
    void getLink_notFound_returnsDefault() {
        UUID userId = UUID.randomUUID();
        when(fileProvider.getRelatedObjectUrl(userId)).thenReturn(Optional.empty());

        assertThat(provider.getLink(userId)).isEqualTo(DEFAULT);
    }

    @Test
    @DisplayName("getLink returns default when FileProvider throws RuntimeException")
    void getLink_exception_returnsDefault() {
        UUID userId = UUID.randomUUID();
        when(fileProvider.getRelatedObjectUrl(userId)).thenThrow(new RuntimeException("S3 down"));

        assertThat(provider.getLink(userId)).isEqualTo(DEFAULT);
    }
}
