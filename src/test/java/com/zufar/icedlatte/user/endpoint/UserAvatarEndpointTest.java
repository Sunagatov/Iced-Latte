package com.zufar.icedlatte.user.endpoint;

import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.UUID;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import com.zufar.icedlatte.common.audit.CurrentUserIdProvider;
import com.zufar.icedlatte.common.util.ClientIpExtractor;
import com.zufar.icedlatte.openapi.dto.AvatarUploadIntentResponse;
import com.zufar.icedlatte.openapi.dto.AvatarUploadStatus;
import com.zufar.icedlatte.openapi.dto.CreateAvatarUploadRequest;
import com.zufar.icedlatte.user.service.AvatarUploadIntentService;
import com.zufar.icedlatte.user.service.UserAvatarUploader;
import com.zufar.icedlatte.user.service.UserProfileService;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserAvatarEndpoint unit tests")
class UserAvatarEndpointTest {

    @Mock
    private UserProfileService userProfileService;

    @Mock
    private UserAvatarUploader userAvatarUploader;

    @Mock
    private AvatarUploadIntentService avatarUploadIntentService;

    @Mock
    private CurrentUserIdProvider currentUserIdProvider;

    @Mock
    private ClientIpExtractor clientIpExtractor;

    private UserAvatarEndpoint endpoint;
    private MockHttpServletRequest httpServletRequest;

    @BeforeEach
    void setUp() {
        httpServletRequest = new MockHttpServletRequest();
        endpoint = new UserAvatarEndpoint(
                userProfileService,
                userAvatarUploader,
                avatarUploadIntentService,
                currentUserIdProvider,
                httpServletRequest,
                clientIpExtractor);
    }

    @Test
    @DisplayName("Delegates avatar upload with current user and optional Turnstile token")
    void uploadUserAvatar_delegatesWithCurrentUserAndTurnstileToken() {
        UUID userId = UUID.randomUUID();
        MultipartFile file = avatarFile();
        when(currentUserIdProvider.getUserId()).thenReturn(userId);
        when(clientIpExtractor.extract(httpServletRequest)).thenReturn("203.0.113.10");

        endpoint.uploadUserAvatar(file, "turnstile-token");

        verify(userAvatarUploader).uploadUserAvatar(userId, file, "turnstile-token", "203.0.113.10");
    }

    @Test
    @DisplayName("Delegates avatar upload intent creation with current user and idempotency key")
    void createAvatarUpload_delegatesWithCurrentUserAndIdempotencyKey() {
        UUID userId = UUID.randomUUID();
        UUID uploadId = UUID.randomUUID();
        var request = new CreateAvatarUploadRequest(CreateAvatarUploadRequest.ContentTypeEnum.IMAGE_PNG, 1024L)
                .turnstileToken("turnstile-token");
        var response = new AvatarUploadIntentResponse(
                uploadId, AvatarUploadStatus.PENDING_UPLOAD, Instant.now().atOffset(java.time.ZoneOffset.UTC));
        when(currentUserIdProvider.getUserId()).thenReturn(userId);
        when(clientIpExtractor.extract(httpServletRequest)).thenReturn("203.0.113.10");
        when(avatarUploadIntentService.createUploadIntent(userId, request, "avatar-key-1", "203.0.113.10"))
                .thenReturn(response);

        var result = endpoint.createAvatarUpload("avatar-key-1", request);

        Assertions.assertThat(result.getBody()).isSameAs(response);
        verify(avatarUploadIntentService).createUploadIntent(userId, request, "avatar-key-1", "203.0.113.10");
        verifyNoInteractions(userAvatarUploader);
    }

    private static MultipartFile avatarFile() {
        return new MockMultipartFile("file", "avatar.png", "image/png", new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47});
    }
}
