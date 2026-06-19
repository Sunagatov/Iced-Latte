package com.zufar.icedlatte.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.multipart.MultipartFile;

import com.zufar.icedlatte.common.exception.BadRequestException;
import com.zufar.icedlatte.common.turnstile.TurnstileProperties;
import com.zufar.icedlatte.common.turnstile.TurnstileVerifier;
import com.zufar.icedlatte.filestorage.api.FileCacheInvalidationApi;
import com.zufar.icedlatte.filestorage.api.FileStorageWriterApi;
import com.zufar.icedlatte.filestorage.api.dto.FileMetadataDto;
import com.zufar.icedlatte.user.exception.InvalidAvatarFileTypeException;
import com.zufar.icedlatte.user.exception.UserAvatarUploadException;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserAvatarUploader unit tests")
class UserAvatarUploaderTest {

    @Mock
    private FileStorageWriterApi fileStorageService;

    @Mock
    private ObjectProvider<FileCacheInvalidationApi> cacheInvalidatorProvider;

    @Mock
    private MultipartFile file;

    @Mock
    private TurnstileVerifier turnstileVerifier;

    @Mock
    private TurnstileProperties turnstileProperties;

    private UserAvatarUploader uploader;

    private static final String BUCKET = "test-bucket";

    // Minimal valid JPEG header: FF D8 FF followed by padding
    private static final byte[] JPEG_HEADER =
            new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    private static final byte[] PNG_HEADER =
            new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};
    private static final byte[] WEBP_HEADER = new byte[] {0x52, 0x49, 0x46, 0x46, 0, 0, 0, 0, 0x57, 0x45, 0x42, 0x50};

    @BeforeEach
    void injectBucket() throws Exception {
        uploader = new UserAvatarUploader(
                fileStorageService, cacheInvalidatorProvider, turnstileVerifier, turnstileProperties);
        var field = UserAvatarUploader.class.getDeclaredField("bucketName");
        field.setAccessible(true);
        field.set(uploader, BUCKET);
    }

    @Test
    @DisplayName("uploadUserAvatar deletes old metadata, uploads file, and saves new metadata")
    void uploadUserAvatarFullFlow() throws Exception {
        UUID userId = UUID.randomUUID();
        String expectedFileName = "user-avatar-" + userId + ".jpg";
        when(file.getContentType()).thenReturn("image/jpeg");
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(JPEG_HEADER));
        when(fileStorageService.isEnabled()).thenReturn(true);

        uploader.uploadUserAvatar(userId, file, "unused-turnstile-token");

        verifyNoInteractions(turnstileVerifier);
        ArgumentCaptor<FileMetadataDto> captor = ArgumentCaptor.forClass(FileMetadataDto.class);
        verify(fileStorageService).store(eq(file), captor.capture());
        FileMetadataDto saved = captor.getValue();
        assertThat(saved.relatedObjectId()).isEqualTo(userId);
        assertThat(saved.bucketName()).isEqualTo(BUCKET);
        assertThat(saved.fileName()).isEqualTo(expectedFileName);
    }

    @Test
    @DisplayName("uploadUserAvatar preserves the validated avatar file extension")
    void uploadUserAvatarPreservesValidatedFileExtension() throws Exception {
        UUID userId = UUID.randomUUID();
        when(file.getContentType()).thenReturn("image/webp; charset=binary");
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(WEBP_HEADER));
        when(fileStorageService.isEnabled()).thenReturn(true);

        uploader.uploadUserAvatar(userId, file, "unused-turnstile-token");

        ArgumentCaptor<FileMetadataDto> captor = ArgumentCaptor.forClass(FileMetadataDto.class);
        verify(fileStorageService).store(eq(file), captor.capture());
        assertThat(captor.getValue().fileName()).isEqualTo("user-avatar-" + userId + ".webp");
    }

    @Test
    @DisplayName("uploadUserAvatar throws user-owned exception when upload is skipped (storage unavailable)")
    void uploadUserAvatarSkipsMetadataWhenUploadSkipped() throws Exception {
        UUID userId = UUID.randomUUID();
        when(file.getContentType()).thenReturn("image/jpeg");
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(JPEG_HEADER));
        when(fileStorageService.isEnabled()).thenReturn(false);

        assertThatThrownBy(() -> uploader.uploadUserAvatar(userId, file, "unused-turnstile-token"))
                .isInstanceOf(UserAvatarUploadException.class);

        verify(fileStorageService).isEnabled();
        verify(fileStorageService, never()).store(any(), any());
    }

    @Test
    @DisplayName("uploadUserAvatar throws user-owned exception when bucket configuration is blank")
    void uploadUserAvatarRejectsBlankBucketConfiguration() throws Exception {
        UUID userId = UUID.randomUUID();
        when(file.getContentType()).thenReturn("image/jpeg");
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(JPEG_HEADER));
        when(fileStorageService.isEnabled()).thenReturn(true);
        var field = UserAvatarUploader.class.getDeclaredField("bucketName");
        field.setAccessible(true);
        field.set(uploader, "   ");

        assertThatThrownBy(() -> uploader.uploadUserAvatar(userId, file, "unused-turnstile-token"))
                .isInstanceOf(UserAvatarUploadException.class)
                .hasMessageContaining("userId=" + userId);

        verify(fileStorageService).isEnabled();
        verify(fileStorageService, never()).store(any(), any());
    }

    @Test
    @DisplayName("uploadUserAvatar rejects unsupported content types before upload")
    void uploadUserAvatarRejectsUnsupportedContentTypes() {
        UUID userId = UUID.randomUUID();
        when(file.getContentType()).thenReturn("image/gif");

        assertThatThrownBy(() -> uploader.uploadUserAvatar(userId, file, "unused-turnstile-token"))
                .isInstanceOf(InvalidAvatarFileTypeException.class)
                .hasMessageContaining("image/gif");

        verify(fileStorageService, never()).store(any(), any());
    }

    @Test
    @DisplayName("uploadUserAvatar rejects files whose magic bytes do not match the declared image type")
    void uploadUserAvatarRejectsMagicByteMismatch() throws Exception {
        UUID userId = UUID.randomUUID();
        when(file.getContentType()).thenReturn("image/jpeg");
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream("not-an-image".getBytes()));

        assertThatThrownBy(() -> uploader.uploadUserAvatar(userId, file, "unused-turnstile-token"))
                .isInstanceOf(InvalidAvatarFileTypeException.class)
                .hasMessageContaining("image/jpeg");

        verify(fileStorageService, never()).store(any(), any());
    }

    @Test
    @DisplayName("uploadUserAvatar rejects valid image bytes when content type does not match")
    void uploadUserAvatarRejectsDeclaredTypeMismatch() throws Exception {
        UUID userId = UUID.randomUUID();
        when(file.getContentType()).thenReturn("image/jpeg");
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(PNG_HEADER));

        assertThatThrownBy(() -> uploader.uploadUserAvatar(userId, file, "unused-turnstile-token"))
                .isInstanceOf(InvalidAvatarFileTypeException.class)
                .hasMessageContaining("image/jpeg");

        verify(fileStorageService, never()).store(any(), any());
    }

    @Test
    @DisplayName("uploadUserAvatar verifies Turnstile token when avatar protection is enabled")
    void uploadUserAvatarAvatarTurnstileEnabledVerifiesToken() throws Exception {
        when(turnstileProperties.avatarEnabled()).thenReturn(true);
        UUID userId = UUID.randomUUID();
        when(file.getContentType()).thenReturn("image/jpeg");
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(JPEG_HEADER));
        when(fileStorageService.isEnabled()).thenReturn(true);

        uploader.uploadUserAvatar(userId, file, "turnstile-token", "203.0.113.10");

        verify(turnstileVerifier).verify("turnstile-token", "203.0.113.10");
        verify(fileStorageService).store(eq(file), any(FileMetadataDto.class));
    }

    @Test
    @DisplayName("uploadUserAvatar stops before upload when enabled Turnstile verification fails")
    void uploadUserAvatarAvatarTurnstileVerificationFailsDoesNotUpload() {
        when(turnstileProperties.avatarEnabled()).thenReturn(true);
        UUID userId = UUID.randomUUID();
        doThrow(new BadRequestException("Turnstile verification failed"))
                .when(turnstileVerifier)
                .verify("bad-token", "203.0.113.10");

        assertThatThrownBy(() -> uploader.uploadUserAvatar(userId, file, "bad-token", "203.0.113.10"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Turnstile verification failed");

        verify(turnstileVerifier).verify("bad-token", "203.0.113.10");
        verifyNoInteractions(fileStorageService);
    }
}
