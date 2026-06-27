package com.zufar.icedlatte.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.zufar.icedlatte.filestorage.api.FileStorageWriterApi;
import com.zufar.icedlatte.filestorage.api.dto.FileMetadataDto;
import com.zufar.icedlatte.user.entity.UserAvatarUpload;
import com.zufar.icedlatte.user.entity.UserAvatarUploadStatus;
import com.zufar.icedlatte.user.repository.UserAvatarUploadRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("AvatarUploadActivationService unit tests")
class AvatarUploadActivationServiceTest {

    private static final Instant NOW = Instant.parse("2026-06-26T10:15:30Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Mock
    private UserAvatarUploadRepository repository;

    @Mock
    private FileStorageWriterApi fileStorageWriterApi;

    @Test
    @DisplayName("activates ready upload and points current avatar metadata at processed object")
    void activateReadyUploadRecordsProcessedObjectAsCurrentAvatar() {
        UUID userId = UUID.randomUUID();
        UserAvatarUpload oldActive = upload(userId, UUID.randomUUID(), UserAvatarUploadStatus.READY);
        oldActive.setActive(true);
        oldActive.setCreatedAt(NOW.minusSeconds(120));
        UserAvatarUpload ready = upload(userId, UUID.randomUUID(), UserAvatarUploadStatus.READY);
        AvatarUploadActivationService service = service();
        when(repository.findById(ready.getId())).thenReturn(Optional.of(ready));
        when(repository.findByUserIdAndActiveTrue(userId)).thenReturn(Optional.of(oldActive));
        when(repository.existsByUserIdAndCreatedAtAfterAndStatusIn(eq(userId), eq(ready.getCreatedAt()), any()))
                .thenReturn(false);
        when(repository.saveAndFlush(oldActive)).thenReturn(oldActive);
        when(repository.save(ready)).thenReturn(ready);

        Optional<UserAvatarUpload> result = service.activate(ready.getId());

        assertThat(result).containsSame(ready);
        assertThat(oldActive.getStatus()).isEqualTo(UserAvatarUploadStatus.SUPERSEDED);
        assertThat(oldActive.isActive()).isFalse();
        assertThat(oldActive.getSupersededAt()).isEqualTo(NOW);
        assertThat(ready.isActive()).isTrue();
        assertThat(ready.getActivatedAt()).isEqualTo(NOW);
        verify(fileStorageWriterApi)
                .recordExisting(new FileMetadataDto(userId, ready.getProcessedBucket(), ready.getProcessedKey()));
        verify(repository).saveAndFlush(oldActive);
        verify(repository).save(ready);
    }

    @Test
    @DisplayName("does not rewrite already active upload")
    void activateAlreadyActiveUploadIsIdempotent() {
        UserAvatarUpload active = upload(UUID.randomUUID(), UUID.randomUUID(), UserAvatarUploadStatus.READY);
        active.setActive(true);
        AvatarUploadActivationService service = service();
        when(repository.findById(active.getId())).thenReturn(Optional.of(active));

        Optional<UserAvatarUpload> result = service.activate(active.getId());

        assertThat(result).containsSame(active);
        verifyNoInteractions(fileStorageWriterApi);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("ignores non-ready upload")
    void activateIgnoresNonReadyUpload() {
        UserAvatarUpload processing = upload(UUID.randomUUID(), UUID.randomUUID(), UserAvatarUploadStatus.PROCESSING);
        AvatarUploadActivationService service = service();
        when(repository.findById(processing.getId())).thenReturn(Optional.of(processing));

        Optional<UserAvatarUpload> result = service.activate(processing.getId());

        assertThat(result).isEmpty();
        assertThat(processing.isActive()).isFalse();
        verifyNoInteractions(fileStorageWriterApi);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("ignores ready upload without processed object metadata")
    void activateIgnoresReadyUploadWithoutProcessedObject() {
        UserAvatarUpload ready = upload(UUID.randomUUID(), UUID.randomUUID(), UserAvatarUploadStatus.READY);
        ready.setProcessedKey("");
        AvatarUploadActivationService service = service();
        when(repository.findById(ready.getId())).thenReturn(Optional.of(ready));

        Optional<UserAvatarUpload> result = service.activate(ready.getId());

        assertThat(result).isEmpty();
        verifyNoInteractions(fileStorageWriterApi);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("ignores older ready upload when a newer avatar is already active")
    void activateIgnoresOlderReadyUploadWhenNewerUploadIsActive() {
        UUID userId = UUID.randomUUID();
        UserAvatarUpload olderReady = upload(userId, UUID.randomUUID(), UserAvatarUploadStatus.READY);
        olderReady.setCreatedAt(NOW.minusSeconds(120));
        UserAvatarUpload newerActive = upload(userId, UUID.randomUUID(), UserAvatarUploadStatus.READY);
        newerActive.setActive(true);
        newerActive.setCreatedAt(NOW.minusSeconds(30));
        AvatarUploadActivationService service = service();
        when(repository.findById(olderReady.getId())).thenReturn(Optional.of(olderReady));
        when(repository.findByUserIdAndActiveTrue(userId)).thenReturn(Optional.of(newerActive));

        Optional<UserAvatarUpload> result = service.activate(olderReady.getId());

        assertThat(result).isEmpty();
        assertThat(olderReady.isActive()).isFalse();
        assertThat(newerActive.isActive()).isTrue();
        verifyNoInteractions(fileStorageWriterApi);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("ignores older ready upload when a newer upload is still in progress")
    void activateIgnoresOlderReadyUploadWhenNewerUploadExists() {
        UUID userId = UUID.randomUUID();
        UserAvatarUpload olderReady = upload(userId, UUID.randomUUID(), UserAvatarUploadStatus.READY);
        olderReady.setCreatedAt(NOW.minusSeconds(120));
        AvatarUploadActivationService service = service();
        when(repository.findById(olderReady.getId())).thenReturn(Optional.of(olderReady));
        when(repository.findByUserIdAndActiveTrue(userId)).thenReturn(Optional.empty());
        when(repository.existsByUserIdAndCreatedAtAfterAndStatusIn(eq(userId), eq(olderReady.getCreatedAt()), any()))
                .thenReturn(true);

        Optional<UserAvatarUpload> result = service.activate(olderReady.getId());

        assertThat(result).isEmpty();
        assertThat(olderReady.isActive()).isFalse();
        verifyNoInteractions(fileStorageWriterApi);
        verify(repository, never()).save(any());
        verify(repository, never()).saveAndFlush(any());
    }

    private AvatarUploadActivationService service() {
        return new AvatarUploadActivationService(repository, fileStorageWriterApi, CLOCK);
    }

    private static UserAvatarUpload upload(UUID userId, UUID uploadId, UserAvatarUploadStatus status) {
        return UserAvatarUpload.builder()
                .id(uploadId)
                .userId(userId)
                .status(status)
                .originalBucket("iced-latte-users")
                .originalKey("avatars/incoming/%s/%s/source".formatted(userId, uploadId))
                .processedBucket("iced-latte-users")
                .processedKey("avatars/processed/%s/%s/avatar.webp".formatted(userId, uploadId))
                .contentType("image/webp")
                .originalSizeBytes(1024L)
                .processedSizeBytes(512L)
                .imageWidth(384)
                .imageHeight(384)
                .sha256("0".repeat(64))
                .createdAt(NOW.minusSeconds(60))
                .expiresAt(NOW.plusSeconds(60))
                .active(false)
                .build();
    }
}
