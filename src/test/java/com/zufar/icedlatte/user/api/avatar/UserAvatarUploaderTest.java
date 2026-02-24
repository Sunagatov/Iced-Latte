package com.zufar.icedlatte.user.api.avatar;

import com.zufar.icedlatte.filestorage.dto.FileMetadataDto;
import com.zufar.icedlatte.filestorage.file.FileUploader;
import com.zufar.icedlatte.filestorage.filemetadata.FileMetadataDeleter;
import com.zufar.icedlatte.filestorage.filemetadata.FileMetadataSaver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserAvatarUploader unit tests")
class UserAvatarUploaderTest {

    @Mock private FileUploader fileUploader;
    @Mock private FileMetadataSaver fileMetadataSaver;
    @Mock private FileMetadataDeleter fileMetadataDeleter;
    @Mock private MultipartFile file;
    @InjectMocks private UserAvatarUploader uploader;

    private static final String BUCKET = "test-bucket";

    @BeforeEach
    void injectBucket() throws Exception {
        var field = UserAvatarUploader.class.getDeclaredField("bucketName");
        field.setAccessible(true);
        field.set(uploader, BUCKET);
    }

    @Test
    @DisplayName("uploadUserAvatar deletes old metadata, uploads file, and saves new metadata")
    void uploadUserAvatar_fullFlow() {
        UUID userId = UUID.randomUUID();
        String expectedFileName = "user-avatar-" + userId;

        uploader.uploadUserAvatar(userId, file);

        verify(fileMetadataDeleter).deleteByRelatedObjectId(userId);
        verify(fileUploader).upload(file, BUCKET, expectedFileName);

        ArgumentCaptor<FileMetadataDto> captor = ArgumentCaptor.forClass(FileMetadataDto.class);
        verify(fileMetadataSaver).save(captor.capture());
        FileMetadataDto saved = captor.getValue();
        assertThat(saved.relatedObjectId()).isEqualTo(userId);
        assertThat(saved.bucketName()).isEqualTo(BUCKET);
        assertThat(saved.fileName()).isEqualTo(expectedFileName);
    }
}
