package com.zufar.icedlatte.user.endpoint;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import com.zufar.icedlatte.common.audit.CurrentUserIdProvider;
import com.zufar.icedlatte.common.exception.BadRequestException;
import com.zufar.icedlatte.security.signin.turnstile.TurnstileVerifier;
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

    @Mock
    private TurnstileVerifier turnstileVerifier;

    private UserEndpoint endpoint;

    @BeforeEach
    void setUp() {
        endpoint = new UserEndpoint(
                userProfileService,
                userAvatarUploader,
                deliveryAddressService,
                currentUserIdProvider,
                turnstileVerifier);
    }

    @Test
    @DisplayName("Uploads avatar without Turnstile verification when avatar protection is disabled")
    void uploadUserAvatar_avatarTurnstileDisabled_skipsVerification() {
        UUID userId = UUID.randomUUID();
        MultipartFile file = avatarFile();
        when(currentUserIdProvider.getUserId()).thenReturn(userId);

        endpoint.uploadUserAvatar(file, null);

        verifyNoInteractions(turnstileVerifier);
        verify(userAvatarUploader).uploadUserAvatar(userId, file);
    }

    @Test
    @DisplayName("Verifies Turnstile token before avatar upload when avatar protection is enabled")
    void uploadUserAvatar_avatarTurnstileEnabled_verifiesToken() {
        ReflectionTestUtils.setField(endpoint, "avatarTurnstileEnabled", true);
        UUID userId = UUID.randomUUID();
        MultipartFile file = avatarFile();
        when(currentUserIdProvider.getUserId()).thenReturn(userId);

        endpoint.uploadUserAvatar(file, "turnstile-token");

        verify(turnstileVerifier).verify("turnstile-token");
        verify(userAvatarUploader).uploadUserAvatar(userId, file);
    }

    @Test
    @DisplayName("Does not upload avatar when enabled Turnstile verification fails")
    void uploadUserAvatar_avatarTurnstileVerificationFails_doesNotUpload() {
        ReflectionTestUtils.setField(endpoint, "avatarTurnstileEnabled", true);
        MultipartFile file = avatarFile();
        doThrow(new BadRequestException("Turnstile verification failed"))
                .when(turnstileVerifier)
                .verify("bad-token");

        assertThatThrownBy(() -> endpoint.uploadUserAvatar(file, "bad-token"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Turnstile verification failed");

        verify(turnstileVerifier).verify("bad-token");
        verifyNoInteractions(currentUserIdProvider, userAvatarUploader);
    }

    private static MultipartFile avatarFile() {
        return new MockMultipartFile("file", "avatar.png", "image/png", new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47});
    }
}
