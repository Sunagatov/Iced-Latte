package com.zufar.icedlatte.astartup;

import com.zufar.icedlatte.filestorage.aws.AwsProvider;
import com.zufar.icedlatte.filestorage.dto.FileMetadataDto;
import com.zufar.icedlatte.filestorage.exception.FileReadException;
import com.zufar.icedlatte.filestorage.exception.FileUploadException;
import com.zufar.icedlatte.filestorage.file.FileUploader;
import com.zufar.icedlatte.filestorage.filemetadata.FileMetadataSaver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.core.exception.SdkClientException;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ApplicationMigration unit tests")
class ApplicationMigrationTest {

    @Mock private FileUploader fileUploader;
    @Mock private AwsProvider awsProvider;
    @Mock private FileMetadataSaver fileMetadataSaver;
    @Mock private ApplicationArguments args;

    private ApplicationMigration migration;

    @BeforeEach
    void setUp() {
        migration = new ApplicationMigration(fileUploader, awsProvider, fileMetadataSaver);
        ReflectionTestUtils.setField(migration, "productPictureBucket", "products-bucket");
        ReflectionTestUtils.setField(migration, "directoryPath", "/seed/products");
        ReflectionTestUtils.setField(migration, "uploadEnabled", true);
    }

    @Nested
    @DisplayName("run")
    class Run {

        @Test
        @DisplayName("skips immediately when AWS migration is not configured")
        void skipsWhenAwsConfigurationIsIncomplete() {
            ReflectionTestUtils.setField(migration, "productPictureBucket", "");

            migration.run(args);

            verifyNoInteractions(fileUploader, awsProvider, fileMetadataSaver);
        }

        @Test
        @DisplayName("skips immediately when file upload capability is unavailable")
        void skipsWhenFileUploaderIsPresentButStorageIsUnavailable() {
            when(fileUploader.isStorageConfigured()).thenReturn(false);

            migration.run(args);

            verify(fileUploader).isStorageConfigured();
            verifyNoInteractions(awsProvider, fileMetadataSaver);
        }

        @Test
        @DisplayName("when upload is disabled it still fetches and saves metadata")
        void uploadDisabledStillFetchesAndSavesMetadata() {
            List<FileMetadataDto> metadata = List.of(fileMetadata("cover.jpg"));
            ReflectionTestUtils.setField(migration, "uploadEnabled", false);
            when(fileUploader.isStorageConfigured()).thenReturn(true);
            when(awsProvider.getProductImagesFromAWS("products-bucket")).thenReturn(metadata);

            migration.run(args);

            verify(awsProvider, timeout(1000)).getProductImagesFromAWS("products-bucket");
            verify(fileMetadataSaver, timeout(1000)).replaceAllByBucket("products-bucket", metadata);
            verify(fileUploader).isStorageConfigured();
            verifyNoMoreInteractions(fileUploader, awsProvider, fileMetadataSaver);
        }
    }

    @Nested
    @DisplayName("uploadFiles")
    class UploadFiles {

        @Test
        @DisplayName("uploads the configured directory to the configured bucket")
        void uploadsConfiguredDirectory() {
            invokeVoid("uploadFiles");

            verify(fileUploader).uploadDirectory("products-bucket", "/seed/products");
            verifyNoMoreInteractions(fileUploader);
        }

        @Test
        @DisplayName("swallows file upload failures")
        void swallowsFileUploadFailures() {
            doThrow(new FileUploadException("seed.zip", new RuntimeException("boom")))
                    .when(fileUploader).uploadDirectory("products-bucket", "/seed/products");

            assertThatCode(() -> invokeVoid("uploadFiles")).doesNotThrowAnyException();

            verify(fileUploader).uploadDirectory("products-bucket", "/seed/products");
            verifyNoMoreInteractions(fileUploader);
        }

        @Test
        @DisplayName("swallows file read failures")
        void swallowsFileReadFailures() {
            doThrow(new FileReadException("seed.zip", new RuntimeException("boom")))
                    .when(fileUploader).uploadDirectory("products-bucket", "/seed/products");

            assertThatCode(() -> invokeVoid("uploadFiles")).doesNotThrowAnyException();

            verify(fileUploader).uploadDirectory("products-bucket", "/seed/products");
            verifyNoMoreInteractions(fileUploader);
        }
    }

    @Nested
    @DisplayName("fetchMetadata")
    class FetchMetadata {

        @Test
        @DisplayName("returns metadata fetched from AWS")
        void returnsFetchedMetadata() {
            List<FileMetadataDto> metadata = List.of(fileMetadata("gallery/cover.jpg"));
            when(awsProvider.getProductImagesFromAWS("products-bucket")).thenReturn(metadata);

            List<FileMetadataDto> result = invoke("fetchMetadata");

            assertThat(result).containsExactlyElementsOf(metadata);
            verify(awsProvider).getProductImagesFromAWS("products-bucket");
            verifyNoMoreInteractions(awsProvider);
        }

        @Test
        @DisplayName("returns an empty list when AWS access fails")
        void returnsEmptyListWhenAwsFails() {
            when(awsProvider.getProductImagesFromAWS("products-bucket"))
                    .thenThrow(SdkClientException.create("unreachable"));

            List<FileMetadataDto> result = invoke("fetchMetadata");

            assertThat(result).isEmpty();
            verify(awsProvider).getProductImagesFromAWS("products-bucket");
            verifyNoMoreInteractions(awsProvider);
        }
    }

    @Nested
    @DisplayName("saveMetadata")
    class SaveMetadata {

        @Test
        @DisplayName("skips persistence when metadata is empty")
        void skipsEmptyMetadata() {
            invokeVoid("saveMetadata", List.of());

            verifyNoInteractions(fileMetadataSaver);
        }

        @Test
        @DisplayName("replaces stored metadata when entries exist")
        void replacesMetadataWhenEntriesExist() {
            List<FileMetadataDto> metadata = List.of(fileMetadata("gallery/cover.jpg"));

            invokeVoid("saveMetadata", metadata);

            verify(fileMetadataSaver).replaceAllByBucket("products-bucket", metadata);
            verifyNoMoreInteractions(fileMetadataSaver);
        }

        @Test
        @DisplayName("swallows persistence failures")
        void swallowsPersistenceFailures() {
            List<FileMetadataDto> metadata = List.of(fileMetadata("gallery/cover.jpg"));
            doThrow(new DataAccessResourceFailureException("db down"))
                    .when(fileMetadataSaver).replaceAllByBucket("products-bucket", metadata);

            assertThatCode(() -> invokeVoid("saveMetadata", metadata)).doesNotThrowAnyException();

            verify(fileMetadataSaver).replaceAllByBucket("products-bucket", metadata);
            verifyNoMoreInteractions(fileMetadataSaver);
        }
    }

    private FileMetadataDto fileMetadata(String fileName) {
        return new FileMetadataDto(UUID.randomUUID(), "products-bucket", fileName);
    }

    private <T> T invoke(String methodName, Object... args) {
        return ReflectionTestUtils.invokeMethod(migration, methodName, args);
    }

    private void invokeVoid(String methodName, Object... args) {
        ReflectionTestUtils.invokeMethod(migration, methodName, args);
    }
}
