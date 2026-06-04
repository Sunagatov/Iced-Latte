package com.zufar.icedlatte.user.endpoint;

import static org.mockito.Mockito.*;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import com.zufar.icedlatte.common.audit.CurrentUserIdProvider;
import com.zufar.icedlatte.user.service.DeliveryAddressService;
import com.zufar.icedlatte.user.service.UserAvatarUploader;
import com.zufar.icedlatte.user.service.UserProfileService;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserEndpoint unit tests")
class UserEndpointTest {

    @Mock
    private UserProfileService userProfileService;

    @Mock
    private UserAvatarUploader userAvatarUploader;

    @Mock
    private DeliveryAddressService deliveryAddressService;

    @Mock
    private CurrentUserIdProvider currentUserIdProvider;

    private UserEndpoint endpoint;

    @BeforeEach
    void setUp() {
        endpoint =
                new UserEndpoint(userProfileService, userAvatarUploader, deliveryAddressService, currentUserIdProvider);
    }

    @Test
    @DisplayName("Delegates avatar upload with current user and optional Turnstile token")
    void uploadUserAvatar_delegatesWithCurrentUserAndTurnstileToken() {
        UUID userId = UUID.randomUUID();
        MultipartFile file = avatarFile();
        when(currentUserIdProvider.getUserId()).thenReturn(userId);

        endpoint.uploadUserAvatar(file, "turnstile-token");

        verify(userAvatarUploader).uploadUserAvatar(userId, file, "turnstile-token");
    }

    private static MultipartFile avatarFile() {
        return new MockMultipartFile("file", "avatar.png", "image/png", new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47});
    }
}
