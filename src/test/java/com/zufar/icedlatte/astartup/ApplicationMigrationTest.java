package com.zufar.icedlatte.astartup;

import com.zufar.icedlatte.filestorage.exception.FileReadException;
import com.zufar.icedlatte.filestorage.exception.FileUploadException;
import com.zufar.icedlatte.filestorage.FileStorageService;
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

    @Mock private FileStorageService fileStorageService;
    @Mock private ApplicationArguments args;

    private ApplicationMigration migration;

    @BeforeEach
    void setUp() {
        migration = new ApplicationMigration(fileStorageService);
        ReflectionTestUtils.setField(migration, "productPictureBucket", "products-bucket");
        ReflectionTestUtils.setField(migration, "directoryPath", "/seed/products");
        ReflectionTestUtils.setField(migration, "uploadEnabled", true);
        ReflectionTestUtils.setField(migration, "timeoutMinutes", 5);
    }

    @Nested
    @DisplayName("run")
    class Run {

        @Test
        @DisplayName("skips immediately when AWS migration is not configured")
        void skipsWhenAwsConfigurationIsIncomplete() {
            ReflectionTestUtils.setField(migration, "productPictureBucket", "");

            migration.run(args);

            verifyNoInteractions(fileStorageService);
        }

        @Test
        @DisplayName("skips immediately when storage is unavailable")
        void skipsWhenStorageIsUnavailable() {
            when(fileStorageService.isEnabled()).thenReturn(false);

            migration.run(args);

            verify(fileStorageService).isEnabled();
            verifyNoMoreInteractions(fileStorageService);
        }

        @Test
        @DisplayName("when upload is disabled it still refreshes metadata")
        void uploadDisabledStillRefreshesMetadata() {
            ReflectionTestUtils.setField(migration, "uploadEnabled", false);
            when(fileStorageService.isEnabled()).thenReturn(true);

            migration.run(args);

            verify(fileStorageService, timeout(1000)).refreshBucketIndex("products-bucket");
            verify(fileStorageService).isEnabled();
            verifyNoMoreInteractions(fileStorageService);
        }
    }

    @Nested
    @DisplayName("uploadFiles")
    class UploadFiles {

        @Test
        @DisplayName("uploads the configured directory to the configured bucket")
        void uploadsConfiguredDirectory() throws Exception {
            invokeVoid("uploadFiles");

            verify(fileStorageService).storeDirectory("products-bucket", "/seed/products");
            verifyNoMoreInteractions(fileStorageService);
        }

        @Test
        @DisplayName("swallows file upload failures")
        void swallowsFileUploadFailures() throws Exception {
            doThrow(new FileUploadException("seed.zip", new RuntimeException("boom")))
                    .when(fileStorageService).storeDirectory("products-bucket", "/seed/products");

            assertThatCode(() -> invokeVoid("uploadFiles")).doesNotThrowAnyException();

            verify(fileStorageService).storeDirectory("products-bucket", "/seed/products");
            verifyNoMoreInteractions(fileStorageService);
        }

        @Test
        @DisplayName("swallows file read failures")
        void swallowsFileReadFailures() throws Exception {
            doThrow(new FileReadException("seed.zip", new RuntimeException("boom")))
                    .when(fileStorageService).storeDirectory("products-bucket", "/seed/products");

            assertThatCode(() -> invokeVoid("uploadFiles")).doesNotThrowAnyException();

            verify(fileStorageService).storeDirectory("products-bucket", "/seed/products");
            verifyNoMoreInteractions(fileStorageService);
        }
    }

    @Nested
    @DisplayName("refreshMetadataIndex")
    class RefreshMetadataIndex {

        @Test
        @DisplayName("refreshes metadata from storage")
        void refreshesMetadataFromStorage() {
            invokeVoid("refreshMetadataIndex");

            verify(fileStorageService).refreshBucketIndex("products-bucket");
            verifyNoMoreInteractions(fileStorageService);
        }

        @Test
        @DisplayName("swallows SDK failures")
        void swallowsSdkFailures() {
            doThrow(SdkClientException.create("unreachable"))
                    .when(fileStorageService).refreshBucketIndex("products-bucket");

            assertThatCode(() -> invokeVoid("refreshMetadataIndex")).doesNotThrowAnyException();

            verify(fileStorageService).refreshBucketIndex("products-bucket");
            verifyNoMoreInteractions(fileStorageService);
        }
    }

    @Nested
    @DisplayName("refreshMetadataIndex persistence")
    class RefreshMetadataPersistence {

        @Test
        @DisplayName("swallows persistence failures")
        void swallowsPersistenceFailures() {
            doThrow(new DataAccessResourceFailureException("db down"))
                    .when(fileStorageService).refreshBucketIndex("products-bucket");

            assertThatCode(() -> invokeVoid("refreshMetadataIndex")).doesNotThrowAnyException();

            verify(fileStorageService).refreshBucketIndex("products-bucket");
            verifyNoMoreInteractions(fileStorageService);
        }
    }

    private void invokeVoid(String methodName, Object... args) {
        ReflectionTestUtils.invokeMethod(migration, methodName, args);
    }
}
