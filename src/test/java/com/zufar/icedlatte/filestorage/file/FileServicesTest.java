package com.zufar.icedlatte.filestorage.file;

import com.zufar.icedlatte.filestorage.aws.AwsObjectDeleter;
import com.zufar.icedlatte.filestorage.aws.AwsObjectUploader;
import com.zufar.icedlatte.filestorage.aws.AwsTemporaryLinkReceiver;
import com.zufar.icedlatte.filestorage.dto.FileMetadataDto;
import com.zufar.icedlatte.filestorage.filemetadata.FileMetadataProvider;
import com.zufar.icedlatte.filestorage.repository.FileMetadataRepository;
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
        @Mock FileMetadataRepository fileMetadataRepository;

        @Test
        @DisplayName("Deletes file and metadata when AWS is configured and metadata exists")
        void deleteAwsConfiguredMetadataExistsdeletesAll() {
            UUID id = UUID.randomUUID();
            FileMetadataDto dto = new FileMetadataDto(id, "bucket", "file.jpg");
            when(fileMetadataProvider.getFileMetadataDto(id)).thenReturn(Optional.of(dto));

            new FileDeleter(awsObjectDeleter, fileMetadataProvider, fileMetadataRepository).delete(id);

            verify(awsObjectDeleter).deleteFile(dto);
            verify(fileMetadataRepository).deleteByRelatedObjectId(id);
        }

        @Test
        @DisplayName("Deletes metadata even when AWS is not configured")
        void deleteAwsNullStillDeletesMetadata() {
            UUID id = UUID.randomUUID();
            FileMetadataDto dto = new FileMetadataDto(id, "bucket", "file.jpg");
            when(fileMetadataProvider.getFileMetadataDto(id)).thenReturn(Optional.of(dto));

            new FileDeleter(null, fileMetadataProvider, fileMetadataRepository).delete(id);

            verify(fileMetadataRepository).deleteByRelatedObjectId(id);
            verifyNoInteractions(awsObjectDeleter);
        }

        @Test
        @DisplayName("Does nothing when metadata is absent")
        void deleteNoMetadataDoesNothing() {
            UUID id = UUID.randomUUID();
            when(fileMetadataProvider.getFileMetadataDto(id)).thenReturn(Optional.empty());

            new FileDeleter(awsObjectDeleter, fileMetadataProvider, fileMetadataRepository).delete(id);

            verifyNoInteractions(awsObjectDeleter, fileMetadataRepository);
        }
    }

    @Nested
    @DisplayName("FileProvider")
    class FileProviderTests {

        @Mock AwsTemporaryLinkReceiver awsTemporaryLinkReceiver;
        @Mock FileMetadataProvider fileMetadataProvider;

        @Test
        @DisplayName("Returns URL when AWS configured and metadata exists")
        void getRelatedObjectUrlAwsConfiguredReturnsUrl() {
            UUID id = UUID.randomUUID();
            FileMetadataDto dto = new FileMetadataDto(id, "bucket", "file.jpg");
            when(fileMetadataProvider.getFileMetadataDto(id)).thenReturn(Optional.of(dto));
            when(awsTemporaryLinkReceiver.generatePresignedUrlAsString(dto)).thenReturn("https://s3.example.com/file.jpg");

            Optional<String> result = new FileProvider(awsTemporaryLinkReceiver, fileMetadataProvider).getRelatedObjectUrl(id);

            assertThat(result).contains("https://s3.example.com/file.jpg");
        }

        @Test
        @DisplayName("Returns empty when AWS is not configured")
        void getRelatedObjectUrlAwsNullReturnsEmpty() {
            UUID id = UUID.randomUUID();
            Optional<String> result = new FileProvider(null, fileMetadataProvider).getRelatedObjectUrl(id);
            assertThat(result).isEmpty();
            verifyNoInteractions(fileMetadataProvider);
        }

        @Test
        @DisplayName("Returns empty when metadata not found")
        void getRelatedObjectUrlNoMetadataReturnsEmpty() {
            UUID id = UUID.randomUUID();
            when(fileMetadataProvider.getFileMetadataDto(id)).thenReturn(Optional.empty());

            Optional<String> result = new FileProvider(awsTemporaryLinkReceiver, fileMetadataProvider).getRelatedObjectUrl(id);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Returns empty when generated URL is null")
        void getRelatedObjectUrlNullGeneratedUrlReturnsEmpty() {
            UUID id = UUID.randomUUID();
            FileMetadataDto dto = new FileMetadataDto(id, "bucket", "file.jpg");
            when(fileMetadataProvider.getFileMetadataDto(id)).thenReturn(Optional.of(dto));
            when(awsTemporaryLinkReceiver.generatePresignedUrlAsString(dto)).thenReturn(null);

            Optional<String> result = new FileProvider(awsTemporaryLinkReceiver, fileMetadataProvider).getRelatedObjectUrl(id);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Returns empty map when AWS is not configured for bulk lookup")
        void getRelatedObjectUrlsAwsNullReturnsEmptyMap() {
            Map<UUID, String> result = new FileProvider(null, fileMetadataProvider)
                    .getRelatedObjectUrls(List.of(UUID.randomUUID()));
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Returns only non-null generated URLs for bulk lookup")
        void getRelatedObjectUrlsFiltersNullUrls() {
            UUID firstId = UUID.randomUUID();
            UUID secondId = UUID.randomUUID();
            FileMetadataDto firstDto = new FileMetadataDto(firstId, "bucket", "first.jpg");
            FileMetadataDto secondDto = new FileMetadataDto(secondId, "bucket", "second.jpg");
            when(fileMetadataProvider.getFileMetadataDtos(List.of(firstId, secondId)))
                    .thenReturn(Map.of(firstId, firstDto, secondId, secondDto));
            when(awsTemporaryLinkReceiver.generatePresignedUrlAsString(firstDto)).thenReturn("https://cdn.example.com/first.jpg");
            when(awsTemporaryLinkReceiver.generatePresignedUrlAsString(secondDto)).thenReturn(null);

            Map<UUID, String> result = new FileProvider(awsTemporaryLinkReceiver, fileMetadataProvider)
                    .getRelatedObjectUrls(List.of(firstId, secondId));

            assertThat(result).containsExactly(Map.entry(firstId, "https://cdn.example.com/first.jpg"));
        }
    }

    @Nested
    @DisplayName("FileUploader")
    class FileUploaderTests {

        @Mock AwsObjectUploader awsObjectUploader;
        @Mock MultipartFile multipartFile;

        @Test
        @DisplayName("Uploads file when AWS is configured and returns true")
        void uploadAwsConfiguredCallsUploader() {
            boolean result = new FileUploader(awsObjectUploader).upload(multipartFile, "bucket", "file.jpg");
            verify(awsObjectUploader).uploadFile(multipartFile, "bucket", "file.jpg");
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Skips upload when AWS is not configured and returns false")
        void uploadAwsNullSkips() {
            boolean result = new FileUploader(null).upload(multipartFile, "bucket", "file.jpg");
            verifyNoInteractions(multipartFile);
            assertThat(result).isFalse();
        }
    }
}
