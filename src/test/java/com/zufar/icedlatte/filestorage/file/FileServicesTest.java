package com.zufar.icedlatte.filestorage.file;

import com.zufar.icedlatte.filestorage.aws.AwsObjectDeleter;
import com.zufar.icedlatte.filestorage.aws.AwsObjectUploader;
import com.zufar.icedlatte.filestorage.aws.AwsTemporaryLinkReceiver;
import com.zufar.icedlatte.filestorage.dto.FileMetadataDto;
import com.zufar.icedlatte.filestorage.filemetadata.FileMetadataDeleter;
import com.zufar.icedlatte.filestorage.filemetadata.FileMetadataProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("File service unit tests")
class FileServicesTest {

    @Nested
    @DisplayName("FileDeleter")
    class FileDeleterTests {

        @Mock AwsObjectDeleter awsObjectDeleter;
        @Mock FileMetadataProvider fileMetadataProvider;
        @Mock FileMetadataDeleter fileMetadataDeleter;

        @Test
        @DisplayName("Deletes file and metadata when AWS is configured and metadata exists")
        void delete_awsConfigured_metadataExists_deletesAll() {
            UUID id = UUID.randomUUID();
            FileMetadataDto dto = new FileMetadataDto(id, "bucket", "file.jpg");
            when(fileMetadataProvider.getFileMetadataDto(id)).thenReturn(Optional.of(dto));

            new FileDeleter(awsObjectDeleter, fileMetadataProvider, fileMetadataDeleter).delete(id);

            verify(awsObjectDeleter).deleteFile(dto);
            verify(fileMetadataDeleter).deleteByRelatedObjectId(id);
        }

        @Test
        @DisplayName("Skips AWS delete when AWS is not configured")
        void delete_awsNull_skipsDelete() {
            UUID id = UUID.randomUUID();
            FileMetadataDto dto = new FileMetadataDto(id, "bucket", "file.jpg");
            when(fileMetadataProvider.getFileMetadataDto(id)).thenReturn(Optional.of(dto));

            new FileDeleter(null, fileMetadataProvider, fileMetadataDeleter).delete(id);

            verifyNoInteractions(fileMetadataDeleter);
        }

        @Test
        @DisplayName("Does nothing when metadata is absent")
        void delete_noMetadata_doesNothing() {
            UUID id = UUID.randomUUID();
            when(fileMetadataProvider.getFileMetadataDto(id)).thenReturn(Optional.empty());

            new FileDeleter(awsObjectDeleter, fileMetadataProvider, fileMetadataDeleter).delete(id);

            verifyNoInteractions(awsObjectDeleter, fileMetadataDeleter);
        }
    }

    @Nested
    @DisplayName("FileProvider")
    class FileProviderTests {

        @Mock AwsTemporaryLinkReceiver awsTemporaryLinkReceiver;
        @Mock FileMetadataProvider fileMetadataProvider;

        @Test
        @DisplayName("Returns URL when AWS configured and metadata exists")
        void getRelatedObjectUrl_awsConfigured_returnsUrl() {
            UUID id = UUID.randomUUID();
            FileMetadataDto dto = new FileMetadataDto(id, "bucket", "file.jpg");
            when(fileMetadataProvider.getFileMetadataDto(id)).thenReturn(Optional.of(dto));
            when(awsTemporaryLinkReceiver.generatePresignedUrlAsString(dto)).thenReturn("https://s3.example.com/file.jpg");

            Optional<String> result = new FileProvider(awsTemporaryLinkReceiver, fileMetadataProvider).getRelatedObjectUrl(id);

            assertThat(result).contains("https://s3.example.com/file.jpg");
        }

        @Test
        @DisplayName("Returns empty when AWS is not configured")
        void getRelatedObjectUrl_awsNull_returnsEmpty() {
            UUID id = UUID.randomUUID();
            Optional<String> result = new FileProvider(null, fileMetadataProvider).getRelatedObjectUrl(id);
            assertThat(result).isEmpty();
            verifyNoInteractions(fileMetadataProvider);
        }

        @Test
        @DisplayName("Returns empty when metadata not found")
        void getRelatedObjectUrl_noMetadata_returnsEmpty() {
            UUID id = UUID.randomUUID();
            when(fileMetadataProvider.getFileMetadataDto(id)).thenReturn(Optional.empty());

            Optional<String> result = new FileProvider(awsTemporaryLinkReceiver, fileMetadataProvider).getRelatedObjectUrl(id);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Returns empty map when AWS is not configured for bulk lookup")
        void getRelatedObjectUrls_awsNull_returnsEmptyMap() {
            Map<UUID, String> result = new FileProvider(null, fileMetadataProvider)
                    .getRelatedObjectUrls(List.of(UUID.randomUUID()));
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("FileUploader")
    class FileUploaderTests {

        @Mock AwsObjectUploader awsObjectUploader;
        @Mock MultipartFile multipartFile;

        @Test
        @DisplayName("Uploads file when AWS is configured")
        void upload_awsConfigured_callsUploader() {
            new FileUploader(awsObjectUploader).upload(multipartFile, "bucket", "file.jpg");
            verify(awsObjectUploader).uploadFile(multipartFile, "bucket", "file.jpg");
        }

        @Test
        @DisplayName("Skips upload when AWS is not configured")
        void upload_awsNull_skips() {
            new FileUploader(null).upload(multipartFile, "bucket", "file.jpg");
            verifyNoInteractions(multipartFile);
        }
    }
}
