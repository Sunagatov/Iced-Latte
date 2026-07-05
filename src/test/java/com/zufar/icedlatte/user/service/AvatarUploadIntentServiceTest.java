package com.zufar.icedlatte.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import com.zufar.icedlatte.common.exception.BadRequestException;
import com.zufar.icedlatte.common.turnstile.TurnstileProperties;
import com.zufar.icedlatte.common.turnstile.TurnstileVerifier;
import com.zufar.icedlatte.filestorage.api.FileUrlResolverApi;
import com.zufar.icedlatte.filestorage.api.dto.FileMetadataDto;
import com.zufar.icedlatte.openapi.dto.AvatarUploadStatus;
import com.zufar.icedlatte.openapi.dto.AvatarUploadTargetResponse;
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

    @Mock
    private ObjectProvider<AvatarUploadPresigner> presignerProvider;

    @Mock
    private AvatarUploadPresigner presigner;

    @Mock
    private FileUrlResolverApi fileUrlResolverApi;

    @Test
    @DisplayName("fails closed in backend mode without creating lifecycle row")
    void createUploadIntentFailsClosedInBackendMode() {
        UUID userId = UUID.randomUUID();
        AvatarUploadIntentService service = service(AvatarUploadMode.BACKEND);

        assertThatThrownBy(() -> service.createUploadIntent(
                        userId,
                        new CreateAvatarUploadRequest(CreateAvatarUploadRequest.ContentTypeEnum.IMAGE_PNG, 1024L),
                        "",
                        null))
                .isInstanceOf(UserAvatarUploadException.class);

        verifyNoInteractions(presignerProvider);
        verifyNoInteractions(lifecycleService);
        verifyNoInteractions(turnstileVerifier);
    }

    @Test
    @DisplayName("rejects blank idempotency key before lifecycle creation")
    void createUploadIntentRejectsBlankIdempotencyKey() {
        AvatarUploadIntentService service = service(AvatarUploadMode.PRESIGNED);
        when(presignerProvider.getIfAvailable()).thenReturn(presigner);

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
    @DisplayName("rejects incomplete upload request before lifecycle creation")
    void createUploadIntentRejectsIncompleteRequest() {
        AvatarUploadIntentService service = service(AvatarUploadMode.PRESIGNED);
        when(presignerProvider.getIfAvailable()).thenReturn(presigner);

        assertThatThrownBy(() -> service.createUploadIntent(
                        UUID.randomUUID(), new CreateAvatarUploadRequest(), "avatar-key-1", null))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Avatar upload contentType and sizeBytes are required.");

        verifyNoInteractions(lifecycleService);
    }

    @Test
    @DisplayName("rejects non-positive upload size before lifecycle creation")
    void createUploadIntentRejectsNonPositiveSize() {
        AvatarUploadIntentService service = service(AvatarUploadMode.PRESIGNED);
        when(presignerProvider.getIfAvailable()).thenReturn(presigner);

        assertThatThrownBy(() -> service.createUploadIntent(
                        UUID.randomUUID(),
                        new CreateAvatarUploadRequest(CreateAvatarUploadRequest.ContentTypeEnum.IMAGE_PNG, 0L),
                        "avatar-key-1",
                        null))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Avatar upload sizeBytes must be positive.");

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
        var target = new AvatarUploadTargetResponse()
                .method(AvatarUploadTargetResponse.MethodEnum.PUT)
                .url(URI.create("https://uploads.example.test/avatar"));
        when(presignerProvider.getIfAvailable()).thenReturn(presigner);
        when(lifecycleService.createPendingUpload(userId, "image/png", 1024L, "avatar-key-1"))
                .thenReturn(upload);
        when(presigner.presign(upload)).thenReturn(target);

        var response = service.createUploadIntent(userId, request, "avatar-key-1", null);

        assertThat(response.getUploadId()).isEqualTo(uploadId);
        assertThat(response.getStatus()).isEqualTo(AvatarUploadStatus.PENDING_UPLOAD);
        assertThat(response.getUpload().isPresent()).isTrue();
        assertThat(response.getUpload().get()).isEqualTo(target);
        assertThat(response.getExpiresAt()).isEqualTo(upload.getExpiresAt().atOffset(java.time.ZoneOffset.UTC));
    }

    @Test
    @DisplayName("fails closed in presigned mode when upload presigner is unavailable")
    void createUploadIntentFailsClosedWhenPresignerUnavailable() {
        AvatarUploadIntentService service = service(AvatarUploadMode.PRESIGNED);
        when(presignerProvider.getIfAvailable()).thenReturn(null);

        assertThatThrownBy(() -> service.createUploadIntent(
                        UUID.randomUUID(),
                        new CreateAvatarUploadRequest(CreateAvatarUploadRequest.ContentTypeEnum.IMAGE_PNG, 1024L),
                        "",
                        null))
                .isInstanceOf(UserAvatarUploadException.class);

        verifyNoInteractions(lifecycleService);
        verifyNoInteractions(turnstileVerifier);
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

    @Test
    @DisplayName("includes avatar link for ready upload status when current avatar URL exists")
    void findUploadStatusIncludesAvatarLinkForReadyUpload() {
        UUID userId = UUID.randomUUID();
        UUID uploadId = UUID.randomUUID();
        var upload = UserAvatarUpload.builder()
                .id(uploadId)
                .userId(userId)
                .status(UserAvatarUploadStatus.READY)
                .contentType("image/png")
                .originalBucket("iced-latte-users")
                .originalKey("avatars/incoming/%s/%s/source".formatted(userId, uploadId))
                .processedBucket("iced-latte-users")
                .processedKey("avatars/processed/%s/%s/avatar.webp".formatted(userId, uploadId))
                .createdAt(Instant.parse("2026-06-26T10:15:30Z"))
                .expiresAt(Instant.parse("2026-06-26T10:20:30Z"))
                .active(false)
                .build();
        AvatarUploadIntentService service = service(AvatarUploadMode.BACKEND);
        when(repository.findById(uploadId)).thenReturn(Optional.of(upload));
        when(fileUrlResolverApi.findFileUrl(new FileMetadataDto(
                        uploadId,
                        "iced-latte-users",
                        "avatars/processed/%s/%s/avatar.webp".formatted(userId, uploadId))))
                .thenReturn(Optional.of("https://cdn.example.com/avatar-specific.webp"));

        var response = service.findUploadStatus(userId, uploadId);

        assertThat(response).isPresent();
        assertThat(response.orElseThrow().getAvatarLink().isPresent()).isTrue();
        assertThat(response.orElseThrow().getAvatarLink().get())
                .isEqualTo("https://cdn.example.com/avatar-specific.webp");
    }

    private AvatarUploadIntentService service(AvatarUploadMode mode) {
        return new AvatarUploadIntentService(
                properties(mode),
                lifecycleService,
                repository,
                fileUrlResolverApi,
                presignerProvider,
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
