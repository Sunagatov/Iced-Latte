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

    @Test
    @DisplayName("marks pending upload as processing after source object and image validation")
    void markProcessingRecordsValidatedSourceObjectAndImage() {
        UUID userId = UUID.randomUUID();
        UUID uploadId = UUID.randomUUID();
        UserAvatarUpload pending = upload(userId, uploadId, UserAvatarUploadStatus.PENDING_UPLOAD);
        AvatarUploadLifecycleService service = service(repository, UUID.randomUUID());
        when(repository.findById(uploadId)).thenReturn(Optional.of(pending));
        when(repository.save(pending)).thenReturn(pending);

        Optional<UserAvatarUpload> result = service.markProcessing(processingResult(userId, uploadId));

        assertThat(result).containsSame(pending);
        assertThat(pending.getStatus()).isEqualTo(UserAvatarUploadStatus.PROCESSING);
        assertThat(pending.getUploadedAt()).isEqualTo(NOW);
        assertThat(pending.getImageWidth()).isEqualTo(96);
        assertThat(pending.getImageHeight()).isEqualTo(64);
        assertThat(pending.getOriginalSizeBytes()).isEqualTo(24L);
        verify(repository).save(pending);
    }

    @Test
    @DisplayName("does not rewrite duplicate processing event")
    void markProcessingIgnoresDuplicateProcessingEvent() {
        UUID userId = UUID.randomUUID();
        UUID uploadId = UUID.randomUUID();
        UserAvatarUpload processing = upload(userId, uploadId, UserAvatarUploadStatus.PROCESSING);
        AvatarUploadLifecycleService service = service(repository, UUID.randomUUID());
        when(repository.findById(uploadId)).thenReturn(Optional.of(processing));

        Optional<UserAvatarUpload> result = service.markProcessing(processingResult(userId, uploadId));

        assertThat(result).containsSame(processing);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("ignores processing event when source object does not match upload row")
    void markProcessingIgnoresMismatchedSourceObject() {
        UUID userId = UUID.randomUUID();
        UUID uploadId = UUID.randomUUID();
        UserAvatarUpload pending = upload(userId, uploadId, UserAvatarUploadStatus.PENDING_UPLOAD);
        AvatarUploadLifecycleService service = service(repository, UUID.randomUUID());
        when(repository.findById(uploadId)).thenReturn(Optional.of(pending));

        Optional<UserAvatarUpload> result = service.markProcessing(processingResult(UUID.randomUUID(), uploadId));

        assertThat(result).isEmpty();
        assertThat(pending.getStatus()).isEqualTo(UserAvatarUploadStatus.PENDING_UPLOAD);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("expires upload when processing event arrives after upload expiry")
    void markProcessingExpiresExpiredUpload() {
        UUID userId = UUID.randomUUID();
        UUID uploadId = UUID.randomUUID();
        UserAvatarUpload pending = upload(userId, uploadId, UserAvatarUploadStatus.PENDING_UPLOAD);
        pending.setExpiresAt(NOW.minusSeconds(1));
        AvatarUploadLifecycleService service = service(repository, UUID.randomUUID());
        when(repository.findById(uploadId)).thenReturn(Optional.of(pending));
        when(repository.save(pending)).thenReturn(pending);

        Optional<UserAvatarUpload> result = service.markProcessing(processingResult(userId, uploadId));

        assertThat(result).isEmpty();
        assertThat(pending.getStatus()).isEqualTo(UserAvatarUploadStatus.EXPIRED);
        verify(repository).save(pending);
    }

    @Test
    @DisplayName("marks upload as failed with bounded failure details")
    void markFailedRecordsFailureDetails() {
        UUID userId = UUID.randomUUID();
        UUID uploadId = UUID.randomUUID();
        UserAvatarUpload processing = upload(userId, uploadId, UserAvatarUploadStatus.PROCESSING);
        AvatarUploadLifecycleService service = service(repository, UUID.randomUUID());
        when(repository.findById(uploadId)).thenReturn(Optional.of(processing));
        when(repository.save(processing)).thenReturn(processing);

        Optional<UserAvatarUpload> result = service.markFailed(
                uploadId, "INVALID_IMAGE_TOO_LONG_FOR_COLUMN_AND_SHOULD_BE_TRUNCATED_EXTRA_LONG", "x".repeat(600));

        assertThat(result).containsSame(processing);
        assertThat(processing.getStatus()).isEqualTo(UserAvatarUploadStatus.FAILED);
        assertThat(processing.getProcessedAt()).isEqualTo(NOW);
        assertThat(processing.getFailureCode()).hasSize(64).startsWith("INVALID_IMAGE");
        assertThat(processing.getFailureMessage()).hasSize(512);
        verify(repository).save(processing);
    }

    @Test
    @DisplayName("does not rewrite terminal failed upload")
    void markFailedIgnoresTerminalUpload() {
        UUID userId = UUID.randomUUID();
        UUID uploadId = UUID.randomUUID();
        UserAvatarUpload failed = upload(userId, uploadId, UserAvatarUploadStatus.FAILED);
        failed.setFailureCode("INVALID_IMAGE");
        AvatarUploadLifecycleService service = service(repository, UUID.randomUUID());
        when(repository.findById(uploadId)).thenReturn(Optional.of(failed));

        Optional<UserAvatarUpload> result = service.markFailed(uploadId, "DECODE_FAILED", "decode failed");

        assertThat(result).containsSame(failed);
        assertThat(failed.getFailureCode()).isEqualTo("INVALID_IMAGE");
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("marks processing upload as ready with processed object metadata")
    void markReadyRecordsProcessedObjectMetadata() {
        UUID userId = UUID.randomUUID();
        UUID uploadId = UUID.randomUUID();
        UserAvatarUpload processing = upload(userId, uploadId, UserAvatarUploadStatus.PROCESSING);
        AvatarUploadLifecycleService service = service(repository, UUID.randomUUID());
        when(repository.findById(uploadId)).thenReturn(Optional.of(processing));
        when(repository.save(processing)).thenReturn(processing);

        Optional<UserAvatarUpload> result = service.markReady(completion(userId, uploadId));

        assertThat(result).containsSame(processing);
        assertThat(processing.getStatus()).isEqualTo(UserAvatarUploadStatus.READY);
        assertThat(processing.getProcessedBucket()).isEqualTo("iced-latte-users");
        assertThat(processing.getProcessedKey())
                .isEqualTo("avatars/processed/%s/%s/avatar.webp".formatted(userId, uploadId));
        assertThat(processing.getContentType()).isEqualTo("image/webp");
        assertThat(processing.getOriginalSizeBytes()).isEqualTo(1024L);
        assertThat(processing.getProcessedSizeBytes()).isEqualTo(512L);
        assertThat(processing.getImageWidth()).isEqualTo(384);
        assertThat(processing.getImageHeight()).isEqualTo(384);
        assertThat(processing.getSha256()).isEqualTo("0".repeat(64));
        assertThat(processing.getProcessedAt()).isEqualTo(NOW);
        assertThat(processing.isActive()).isFalse();
        verify(repository).save(processing);
    }

    @Test
    @DisplayName("does not rewrite duplicate ready event")
    void markReadyIgnoresDuplicateReadyEvent() {
        UUID userId = UUID.randomUUID();
        UUID uploadId = UUID.randomUUID();
        UserAvatarUpload ready = upload(userId, uploadId, UserAvatarUploadStatus.READY);
        ready.setProcessedKey("avatars/processed/existing/avatar.webp");
        AvatarUploadLifecycleService service = service(repository, UUID.randomUUID());
        when(repository.findById(uploadId)).thenReturn(Optional.of(ready));

        Optional<UserAvatarUpload> result = service.markReady(completion(userId, uploadId));

        assertThat(result).containsSame(ready);
        assertThat(ready.getProcessedKey()).isEqualTo("avatars/processed/existing/avatar.webp");
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("ignores ready event when source object does not match upload row")
    void markReadyIgnoresMismatchedSourceObject() {
        UUID userId = UUID.randomUUID();
        UUID uploadId = UUID.randomUUID();
        UserAvatarUpload processing = upload(userId, uploadId, UserAvatarUploadStatus.PROCESSING);
        AvatarUploadLifecycleService service = service(repository, UUID.randomUUID());
        when(repository.findById(uploadId)).thenReturn(Optional.of(processing));

        Optional<UserAvatarUpload> result = service.markReady(completion(UUID.randomUUID(), uploadId));

        assertThat(result).isEmpty();
        assertThat(processing.getStatus()).isEqualTo(UserAvatarUploadStatus.PROCESSING);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("marks pending upload as ready when completion is the first backend-visible event")
    void markReadyRecordsPendingUploadCompletion() {
        UUID userId = UUID.randomUUID();
        UUID uploadId = UUID.randomUUID();
        UserAvatarUpload pending = upload(userId, uploadId, UserAvatarUploadStatus.PENDING_UPLOAD);
        AvatarUploadLifecycleService service = service(repository, UUID.randomUUID());
        when(repository.findById(uploadId)).thenReturn(Optional.of(pending));
        when(repository.save(pending)).thenReturn(pending);

        Optional<UserAvatarUpload> result = service.markReady(completion(userId, uploadId));

        assertThat(result).containsSame(pending);
        assertThat(pending.getStatus()).isEqualTo(UserAvatarUploadStatus.READY);
        assertThat(pending.getProcessedKey())
                .isEqualTo("avatars/processed/%s/%s/avatar.webp".formatted(userId, uploadId));
        verify(repository).save(pending);
    }

    @Test
    @DisplayName("ignores ready event for terminal upload")
    void markReadyIgnoresTerminalUpload() {
        UUID userId = UUID.randomUUID();
        UUID uploadId = UUID.randomUUID();
        UserAvatarUpload failed = upload(userId, uploadId, UserAvatarUploadStatus.FAILED);
        AvatarUploadLifecycleService service = service(repository, UUID.randomUUID());
        when(repository.findById(uploadId)).thenReturn(Optional.of(failed));

        Optional<UserAvatarUpload> result = service.markReady(completion(userId, uploadId));

        assertThat(result).isEmpty();
        assertThat(failed.getStatus()).isEqualTo(UserAvatarUploadStatus.FAILED);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("invalidates unfinished and ready uploads after avatar delete")
    void invalidateUserUploadsAfterAvatarDeleteSupersedesNonTerminalUploads() {
        UUID userId = UUID.randomUUID();
        UserAvatarUpload pending = upload(userId, UUID.randomUUID(), UserAvatarUploadStatus.PENDING_UPLOAD);
        UserAvatarUpload processing = upload(userId, UUID.randomUUID(), UserAvatarUploadStatus.PROCESSING);
        UserAvatarUpload ready = upload(userId, UUID.randomUUID(), UserAvatarUploadStatus.READY);
        ready.setActive(true);
        AvatarUploadLifecycleService service = service(repository, UUID.randomUUID());
        when(repository.findByUserIdAndStatusIn(eq(userId), any()))
                .thenReturn(java.util.List.of(pending, processing, ready));

        service.invalidateUserUploadsAfterAvatarDelete(userId);

        assertThat(java.util.List.of(pending, processing, ready)).allSatisfy(upload -> {
            assertThat(upload.getStatus()).isEqualTo(UserAvatarUploadStatus.SUPERSEDED);
            assertThat(upload.isActive()).isFalse();
            assertThat(upload.getSupersededAt()).isEqualTo(NOW);
        });
        verify(repository).saveAll(java.util.List.of(pending, processing, ready));
    }

    private static AvatarUploadLifecycleService service(UserAvatarUploadRepository repository, UUID uploadId) {
        return new AvatarUploadLifecycleService(repository, properties(), CLOCK, () -> uploadId);
    }

    private static UserAvatarUpload upload(UUID userId, UUID uploadId, UserAvatarUploadStatus status) {
        return UserAvatarUpload.builder()
                .id(uploadId)
                .userId(userId)
                .status(status)
                .originalBucket("iced-latte-users")
                .originalKey("avatars/incoming/%s/%s/source".formatted(userId, uploadId))
                .contentType("image/png")
                .originalSizeBytes(1024L)
                .createdAt(NOW.minusSeconds(30))
                .expiresAt(NOW.plusSeconds(30))
                .active(false)
                .build();
    }

    private static AvatarUploadProcessingResult processingResult(UUID userId, UUID uploadId) {
        return new AvatarUploadProcessingResult(
                new ValidAvatarUploadSourceObject(
                        "iced-latte-users",
                        "avatars/incoming/%s/%s/source".formatted(userId, uploadId),
                        userId,
                        uploadId,
                        "image/png"),
                new AvatarImageInspection("image/png", 96, 64, 24L));
    }

    private static AvatarUploadCompletion completion(UUID userId, UUID uploadId) {
        return new AvatarUploadCompletion(
                new ValidAvatarUploadSourceObject(
                        "iced-latte-users",
                        "avatars/incoming/%s/%s/source".formatted(userId, uploadId),
                        userId,
                        uploadId,
                        "image/png"),
                "iced-latte-users",
                "avatars/processed/%s/%s/avatar.webp".formatted(userId, uploadId),
                "image/webp",
                384,
                384,
                1024L,
                512L,
                "0".repeat(64));
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
