package com.zufar.icedlatte.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.zufar.icedlatte.common.exception.BadRequestException;
import com.zufar.icedlatte.user.config.AvatarUploadMode;
import com.zufar.icedlatte.user.config.AvatarUploadProperties;
import com.zufar.icedlatte.user.entity.UserAvatarUpload;
import com.zufar.icedlatte.user.entity.UserAvatarUploadStatus;
import com.zufar.icedlatte.user.repository.UserAvatarUploadRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("AvatarUploadLifecycleService unit tests")
class AvatarUploadLifecycleServiceTest {

    private static final Instant NOW = Instant.parse("2026-06-26T10:15:30Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Mock
    private UserAvatarUploadRepository repository;

    @Test
    @DisplayName("creates pending upload with immutable incoming key and expiry")
    void createPendingUploadCreatesLifecycleRow() {
        UUID userId = UUID.randomUUID();
        UUID uploadId = UUID.randomUUID();
        AvatarUploadLifecycleService service = service(repository, uploadId);
        when(repository.findByUserIdAndClientIdempotencyKey(userId, "avatar-key-1"))
                .thenReturn(Optional.empty());
        when(repository.save(any(UserAvatarUpload.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserAvatarUpload upload = service.createPendingUpload(userId, "image/png", 1234L, "avatar-key-1");

        assertThat(upload.getId()).isEqualTo(uploadId);
        assertThat(upload.getUserId()).isEqualTo(userId);
        assertThat(upload.getStatus()).isEqualTo(UserAvatarUploadStatus.PENDING_UPLOAD);
        assertThat(upload.getOriginalBucket()).isEqualTo("iced-latte-users");
        assertThat(upload.getOriginalKey()).isEqualTo("avatars/incoming/" + userId + "/" + uploadId + "/source");
        assertThat(upload.getContentType()).isEqualTo("image/png");
        assertThat(upload.getOriginalSizeBytes()).isEqualTo(1234L);
        assertThat(upload.getClientIdempotencyKey()).isEqualTo("avatar-key-1");
        assertThat(upload.getCreatedAt()).isEqualTo(NOW);
        assertThat(upload.getExpiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(5)));

        ArgumentCaptor<UserAvatarUpload> captor = ArgumentCaptor.forClass(UserAvatarUpload.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue()).isSameAs(upload);
    }

    @Test
    @DisplayName("returns existing unexpired pending upload for same idempotency key")
    void createPendingUploadReturnsExistingUnexpiredIntent() {
        UUID userId = UUID.randomUUID();
        UserAvatarUpload existing = UserAvatarUpload.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .status(UserAvatarUploadStatus.PENDING_UPLOAD)
                .originalBucket("iced-latte-users")
                .originalKey("avatars/incoming/%s/existing/source".formatted(userId))
                .contentType("image/png")
                .clientIdempotencyKey("avatar-key-1")
                .createdAt(NOW.minusSeconds(10))
                .expiresAt(NOW.plusSeconds(30))
                .active(false)
                .build();
        AvatarUploadLifecycleService service = service(repository, UUID.randomUUID());
        when(repository.findByUserIdAndClientIdempotencyKey(userId, "avatar-key-1"))
                .thenReturn(Optional.of(existing));

        UserAvatarUpload upload = service.createPendingUpload(userId, "image/png", 1234L, "avatar-key-1");

        assertThat(upload).isSameAs(existing);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("rejects expired idempotency retry so client uses a new key")
    void createPendingUploadRejectsExpiredIdempotencyRetry() {
        UUID userId = UUID.randomUUID();
        UserAvatarUpload expired = UserAvatarUpload.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .status(UserAvatarUploadStatus.EXPIRED)
                .originalBucket("iced-latte-users")
                .originalKey("avatars/incoming/%s/expired/source".formatted(userId))
                .contentType("image/png")
                .clientIdempotencyKey("avatar-key-1")
                .createdAt(NOW.minus(Duration.ofHours(1)))
                .expiresAt(NOW.minusSeconds(1))
                .active(false)
                .build();
        AvatarUploadLifecycleService service = service(repository, UUID.randomUUID());
        when(repository.findByUserIdAndClientIdempotencyKey(userId, "avatar-key-1"))
                .thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> service.createPendingUpload(userId, "image/png", 1234L, "avatar-key-1"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Previous avatar upload intent expired. Please retry with a new Idempotency-Key.");

        verify(repository, never()).save(any());
    }

    private static AvatarUploadLifecycleService service(UserAvatarUploadRepository repository, UUID uploadId) {
        return new AvatarUploadLifecycleService(repository, properties(), CLOCK, () -> uploadId);
    }

    private static AvatarUploadProperties properties() {
        return new AvatarUploadProperties(
                AvatarUploadMode.PRESIGNED,
                Duration.ofMinutes(5),
                5_242_880L,
                12_000_000L,
                Duration.ofMinutes(10),
                "iced-latte-users",
                "iced-latte-users",
                "");
    }
}
