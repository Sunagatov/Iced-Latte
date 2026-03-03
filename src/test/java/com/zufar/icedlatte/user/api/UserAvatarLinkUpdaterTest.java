package com.zufar.icedlatte.user.api;

import com.zufar.icedlatte.openapi.dto.UserDto;
import com.zufar.icedlatte.user.api.avatar.UserAvatarLinkProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserAvatarLinkUpdater unit tests")
class UserAvatarLinkUpdaterTest {

    @Mock private UserAvatarLinkProvider userAvatarLinkProvider;
    @InjectMocks private UserAvatarLinkUpdater updater;

    @Test
    @DisplayName("update sets avatarLink from provider and returns same dto")
    void update_setsAvatarLink() {
        UUID userId = UUID.randomUUID();
        UserDto dto = new UserDto();
        dto.setId(userId);
        when(userAvatarLinkProvider.getLink(userId)).thenReturn("https://cdn.example.com/avatar.jpg");

        UserDto result = updater.update(dto);

        assertThat(result.getAvatarLink()).isEqualTo("https://cdn.example.com/avatar.jpg");
        assertThat(result).isSameAs(dto);
    }

    @Test
    @DisplayName("update sets default avatarLink when provider returns default")
    void update_setsDefaultLink() {
        UUID userId = UUID.randomUUID();
        UserDto dto = new UserDto();
        dto.setId(userId);
        when(userAvatarLinkProvider.getLink(userId)).thenReturn("default file");

        updater.update(dto);

        assertThat(dto.getAvatarLink()).isEqualTo("default file");
    }
}
