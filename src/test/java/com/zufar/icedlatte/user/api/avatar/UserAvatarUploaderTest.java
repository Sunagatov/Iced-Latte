package com.zufar.icedlatte.user.api.avatar;

import com.zufar.icedlatte.filestorage.dto.FileMetadataDto;
import com.zufar.icedlatte.filestorage.exception.FileUploadException;
import com.zufar.icedlatte.filestorage.file.FileUploader;
import com.zufar.icedlatte.filestorage.filemetadata.FileMetadataSaver;
import com.zufar.icedlatte.filestorage.repository.FileMetadataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserAvatarUploader unit tests")
class UserAvatarUploaderTest {

    @Mock private FileUploader fileUploader;
    @Mock private FileMetadataSaver fileMetadataSaver;
    @Mock private FileMetadataRepository fileMetadataRepository;
    @Mock private MultipartFile file;
    @InjectMocks private UserAvatarUploader uploader;

    private static final String BUCKET = "test-bucket";

    // Minimal valid JPEG header: FF D8 FF followed by padding
    private static final byte[] JPEG_HEADER = new byte[]{
            (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0, 0, 0, 0, 0, 0, 0, 0, 0
    };

    @BeforeEach
    void injectBucket() throws Exception {
        var field = UserAvatarUploader.class.getDeclaredField("bucketName");
        field.setAccessible(true);
        field.set(uploader, BUCKET);
    }

    @Test
    @DisplayName("uploadUserAvatar deletes old metadata, uploads file, and saves new metadata")
    void uploadUserAvatarFullFlow() throws Exception {
        UUID userId = UUID.randomUUID();
        String expectedFileName = "user-avatar-" + userId;
        when(file.getContentType()).thenReturn("image/jpeg");
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(JPEG_HEADER));
        when(fileUploader.upload(file, BUCKET, expectedFileName)).thenReturn(true);

        uploader.uploadUserAvatar(userId, file);

        verify(fileMetadataRepository).deleteByRelatedObjectId(userId);
        verify(fileUploader).upload(file, BUCKET, expectedFileName);

        ArgumentCaptor<FileMetadataDto> captor = ArgumentCaptor.forClass(FileMetadataDto.class);
        verify(fileMetadataSaver).save(captor.capture());
        FileMetadataDto saved = captor.getValue();
        assertThat(saved.relatedObjectId()).isEqualTo(userId);
        assertThat(saved.bucketName()).isEqualTo(BUCKET);
        assertThat(saved.fileName()).isEqualTo(expectedFileName);
    }

    @Test
    @DisplayName("uploadUserAvatar throws FileUploadException when upload is skipped (storage unavailable)")
    void uploadUserAvatarSkipsMetadataWhenUploadSkipped() throws Exception {
        UUID userId = UUID.randomUUID();
        String expectedFileName = "user-avatar-" + userId;
        when(file.getContentType()).thenReturn("image/jpeg");
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(JPEG_HEADER));
        when(fileUploader.upload(file, BUCKET, expectedFileName)).thenReturn(false);

        assertThatThrownBy(() -> uploader.uploadUserAvatar(userId, file))
                .isInstanceOf(FileUploadException.class);

        verify(fileUploader).upload(file, BUCKET, expectedFileName);
        verify(fileMetadataRepository, never()).deleteByRelatedObjectId(org.mockito.ArgumentMatchers.any());
        verify(fileMetadataSaver, never()).save(org.mockito.ArgumentMatchers.any());
    }
}
