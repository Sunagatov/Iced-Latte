package com.zufar.icedlatte.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.zufar.icedlatte.common.exception.BadRequestException;
import com.zufar.icedlatte.common.turnstile.TurnstileProperties;
import com.zufar.icedlatte.common.turnstile.TurnstileVerifier;
import com.zufar.icedlatte.openapi.dto.AvatarUploadStatus;
import com.zufar.icedlatte.openapi.dto.CreateAvatarUploadRequest;
import com.zufar.icedlatte.user.config.AvatarUploadMode;
import com.zufar.icedlatte.user.config.AvatarUploadProperties;
import com.zufar.icedlatte.user.entity.UserAvatarUpload;
import com.zufar.icedlatte.user.entity.UserAvatarUploadStatus;
import com.zufar.icedlatte.user.exception.UserAvatarUploadException;
import com.zufar.icedlatte.user.repository.UserAvatarUploadRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("AvatarUploadIntentService unit tests")
class AvatarUploadIntentServiceTest {

    @Mock
    private AvatarUploadLifecycleService lifecycleService;

    @Mock
    private UserAvatarUploadRepository repository;

    @Mock
    private TurnstileVerifier turnstileVerifier;

    @Test
    @DisplayName("fails closed in backend mode without creating lifecycle row")
    void createUploadIntentFailsClosedInBackendMode() {
        UUID userId = UUID.randomUUID();
        AvatarUploadIntentService service = service(AvatarUploadMode.BACKEND);

        assertThatThrownBy(() -> service.createUploadIntent(
                        userId,
                        new CreateAvatarUploadRequest(CreateAvatarUploadRequest.ContentTypeEnum.IMAGE_PNG, 1024L),
                        "avatar-key-1",
                        null))
                .isInstanceOf(UserAvatarUploadException.class);

        verifyNoInteractions(lifecycleService);
    }

    @Test
    @DisplayName("rejects blank idempotency key before lifecycle creation")
    void createUploadIntentRejectsBlankIdempotencyKey() {
        AvatarUploadIntentService service = service(AvatarUploadMode.PRESIGNED);

        assertThatThrownBy(() -> service.createUploadIntent(
                        UUID.randomUUID(),
                        new CreateAvatarUploadRequest(CreateAvatarUploadRequest.ContentTypeEnum.IMAGE_PNG, 1024L),
                        " ",
                        null))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Idempotency-Key header is required and must not be blank.");

        verifyNoInteractions(lifecycleService);
    }

    @Test
    @DisplayName("creates pending lifecycle row in presigned mode")
    void createUploadIntentCreatesPendingLifecycleRowInPresignedMode() {
        UUID userId = UUID.randomUUID();
        UUID uploadId = UUID.randomUUID();
        var request = new CreateAvatarUploadRequest(CreateAvatarUploadRequest.ContentTypeEnum.IMAGE_PNG, 1024L);
        var upload = UserAvatarUpload.builder()
                .id(uploadId)
                .userId(userId)
                .status(UserAvatarUploadStatus.PENDING_UPLOAD)
                .originalBucket("iced-latte-users")
                .originalKey("avatars/incoming/%s/%s/source".formatted(userId, uploadId))
                .contentType("image/png")
                .originalSizeBytes(1024L)
                .clientIdempotencyKey("avatar-key-1")
                .createdAt(Instant.parse("2026-06-26T10:15:30Z"))
                .expiresAt(Instant.parse("2026-06-26T10:20:30Z"))
                .active(false)
                .build();
        AvatarUploadIntentService service = service(AvatarUploadMode.PRESIGNED);
        when(lifecycleService.createPendingUpload(userId, "image/png", 1024L, "avatar-key-1"))
                .thenReturn(upload);

        var response = service.createUploadIntent(userId, request, "avatar-key-1", null);

        assertThat(response.getUploadId()).isEqualTo(uploadId);
        assertThat(response.getStatus()).isEqualTo(AvatarUploadStatus.PENDING_UPLOAD);
        assertThat(response.getUpload().isPresent()).isFalse();
        assertThat(response.getExpiresAt()).isEqualTo(upload.getExpiresAt().atOffset(java.time.ZoneOffset.UTC));
    }

    @Test
    @DisplayName("returns status only for upload owned by current user")
    void findUploadStatusRequiresUploadOwnership() {
        UUID userId = UUID.randomUUID();
        UUID uploadId = UUID.randomUUID();
        var upload = UserAvatarUpload.builder()
                .id(uploadId)
                .userId(userId)
                .status(UserAvatarUploadStatus.PENDING_UPLOAD)
                .contentType("image/png")
                .originalBucket("iced-latte-users")
                .originalKey("avatars/incoming/%s/%s/source".formatted(userId, uploadId))
                .createdAt(Instant.parse("2026-06-26T10:15:30Z"))
                .expiresAt(Instant.parse("2026-06-26T10:20:30Z"))
                .active(false)
                .build();
        AvatarUploadIntentService service = service(AvatarUploadMode.BACKEND);
        when(repository.findById(uploadId)).thenReturn(Optional.of(upload));

        assertThat(service.findUploadStatus(userId, uploadId)).isPresent();
        assertThat(service.findUploadStatus(UUID.randomUUID(), uploadId)).isEmpty();
    }

    private AvatarUploadIntentService service(AvatarUploadMode mode) {
        return new AvatarUploadIntentService(
                properties(mode),
                lifecycleService,
                repository,
                turnstileVerifier,
                new TurnstileProperties(
                        false,
                        false,
                        false,
                        false,
                        false,
                        "",
                        Duration.ofSeconds(2),
                        Duration.ofSeconds(3),
                        java.util.List.of()));
    }

    private static AvatarUploadProperties properties(AvatarUploadMode mode) {
        return new AvatarUploadProperties(
                mode,
                Duration.ofMinutes(5),
                5_242_880L,
                12_000_000L,
                Duration.ofMinutes(10),
                "iced-latte-users",
                "iced-latte-users",
                "");
    }
}
