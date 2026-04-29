package com.zufar.icedlatte.filestorage.file;

import com.zufar.icedlatte.filestorage.aws.AwsObjectDeleter;
import com.zufar.icedlatte.filestorage.dto.FileMetadataDto;
import com.zufar.icedlatte.filestorage.filemetadata.FileMetadataProvider;
import com.zufar.icedlatte.filestorage.repository.FileMetadataRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("FileDeleter")
class FileDeleterTest {

    private final AwsObjectDeleter awsObjectDeleter = mock(AwsObjectDeleter.class);
    private final FileMetadataProvider fileMetadataProvider = mock(FileMetadataProvider.class);
    private final FileMetadataRepository fileMetadataRepository = mock(FileMetadataRepository.class);

    @Test
    @DisplayName("deletes S3 object and metadata when both are available")
    void deletesS3ObjectAndMetadata() {
        UUID relatedObjectId = UUID.randomUUID();
        FileMetadataDto dto = new FileMetadataDto(relatedObjectId, "avatars", "user.png");
        when(fileMetadataProvider.getFileMetadataDto(relatedObjectId)).thenReturn(Optional.of(dto));

        new FileDeleter(awsObjectDeleter, fileMetadataProvider, fileMetadataRepository).delete(relatedObjectId);

        verify(awsObjectDeleter).deleteFile(dto);
        verify(fileMetadataRepository).deleteByRelatedObjectId(relatedObjectId);
    }

    @Test
    @DisplayName("deletes only metadata when AWS is disabled")
    void deletesOnlyMetadataWhenAwsDisabled() {
        UUID relatedObjectId = UUID.randomUUID();
        FileMetadataDto dto = new FileMetadataDto(relatedObjectId, "avatars", "user.png");
        when(fileMetadataProvider.getFileMetadataDto(relatedObjectId)).thenReturn(Optional.of(dto));

        new FileDeleter(null, fileMetadataProvider, fileMetadataRepository).delete(relatedObjectId);

        verify(fileMetadataRepository).deleteByRelatedObjectId(relatedObjectId);
        verifyNoInteractions(awsObjectDeleter);
    }

    @Test
    @DisplayName("does nothing when no metadata exists")
    void doesNothingWhenMetadataMissing() {
        UUID relatedObjectId = UUID.randomUUID();
        when(fileMetadataProvider.getFileMetadataDto(relatedObjectId)).thenReturn(Optional.empty());

        new FileDeleter(awsObjectDeleter, fileMetadataProvider, fileMetadataRepository).delete(relatedObjectId);

        verify(fileMetadataRepository, never()).deleteByRelatedObjectId(relatedObjectId);
        verifyNoInteractions(awsObjectDeleter);
    }
}
