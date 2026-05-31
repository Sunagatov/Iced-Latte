package com.zufar.icedlatte.astartup;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.*;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;
import org.springframework.dao.DataAccessResourceFailureException;

import com.zufar.icedlatte.filestorage.api.FileStorageApi;
import com.zufar.icedlatte.filestorage.exception.FileUploadException;

import software.amazon.awssdk.core.exception.SdkClientException;

@ExtendWith(MockitoExtension.class)
@DisplayName("ApplicationMigration unit tests")
class ApplicationMigrationTest {

    private static final String DEFAULT_DIRECTORY_PATH = "/seed/products";

    @Mock
    private FileStorageApi fileStorageService;

    @Mock
    private ApplicationArguments args;

    private ApplicationMigration migration;

    @BeforeEach
    void setUp() {
        migration = migration(true, Duration.ofSeconds(5), "products-bucket");
    }

    @Nested
    @DisplayName("run")
    class Run {

        @Test
        @DisplayName("skips immediately when AWS migration is not configured")
        void skipsWhenAwsConfigurationIsIncomplete() {
            migration = migration(true, Duration.ofSeconds(5), "");

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
            migration = migration(false, Duration.ofSeconds(5), "products-bucket");
            when(fileStorageService.isEnabled()).thenReturn(true);

            migration.run(args);

            verify(fileStorageService).isEnabled();
            verify(fileStorageService, timeout(1000)).refreshBucketIndex("products-bucket");
            verifyNoMoreInteractions(fileStorageService);
        }

        @Test
        @DisplayName("uploads files before refreshing metadata")
        void uploadsFilesBeforeRefreshingMetadata() throws Exception {
            when(fileStorageService.isEnabled()).thenReturn(true);

            migration.run(args);

            InOrder inOrder = inOrder(fileStorageService);
            inOrder.verify(fileStorageService).isEnabled();
            inOrder.verify(fileStorageService, timeout(1000)).storeDirectory("products-bucket", "/seed/products");
            inOrder.verify(fileStorageService, timeout(1000)).refreshBucketIndex("products-bucket");
            verifyNoMoreInteractions(fileStorageService);
        }

        @Test
        @DisplayName("continues to refresh existing metadata after upload failure")
        void continuesToRefreshExistingMetadataAfterUploadFailure() throws Exception {
            when(fileStorageService.isEnabled()).thenReturn(true);
            doThrow(new FileUploadException("seed.zip", new RuntimeException("boom")))
                    .when(fileStorageService)
                    .storeDirectory("products-bucket", "/seed/products");

            assertThatCode(() -> migration.run(args)).doesNotThrowAnyException();

            verify(fileStorageService).isEnabled();
            verify(fileStorageService, timeout(1000)).storeDirectory("products-bucket", "/seed/products");
            verify(fileStorageService, timeout(1000)).refreshBucketIndex("products-bucket");
            verifyNoMoreInteractions(fileStorageService);
        }

        @Test
        @DisplayName("does not propagate storage metadata refresh failures")
        void doesNotPropagateStorageMetadataRefreshFailures() {
            migration = migration(false, Duration.ofSeconds(5), "products-bucket");
            when(fileStorageService.isEnabled()).thenReturn(true);
            doThrow(SdkClientException.create("unreachable"))
                    .when(fileStorageService)
                    .refreshBucketIndex("products-bucket");

            assertThatCode(() -> migration.run(args)).doesNotThrowAnyException();

            verify(fileStorageService).isEnabled();
            verify(fileStorageService, timeout(1000)).refreshBucketIndex("products-bucket");
            verifyNoMoreInteractions(fileStorageService);
        }

        @Test
        @DisplayName("does not propagate metadata persistence failures")
        void doesNotPropagateMetadataPersistenceFailures() {
            migration = migration(false, Duration.ofSeconds(5), "products-bucket");
            when(fileStorageService.isEnabled()).thenReturn(true);
            doThrow(new DataAccessResourceFailureException("db down"))
                    .when(fileStorageService)
                    .refreshBucketIndex("products-bucket");

            assertThatCode(() -> migration.run(args)).doesNotThrowAnyException();

            verify(fileStorageService).isEnabled();
            verify(fileStorageService, timeout(1000)).refreshBucketIndex("products-bucket");
            verifyNoMoreInteractions(fileStorageService);
        }

        @Test
        @DisplayName("does not block startup when metadata refresh times out")
        void doesNotBlockStartupWhenMetadataRefreshTimesOut() {
            migration = migration(false, Duration.ofMillis(10), "products-bucket");
            when(fileStorageService.isEnabled()).thenReturn(true);
            doAnswer(_ -> {
                        Thread.sleep(Duration.ofSeconds(5));
                        return null;
                    })
                    .when(fileStorageService)
                    .refreshBucketIndex("products-bucket");

            assertThatCode(() -> migration.run(args)).doesNotThrowAnyException();

            verify(fileStorageService).isEnabled();
            verify(fileStorageService, timeout(1000)).refreshBucketIndex("products-bucket");
            verifyNoMoreInteractions(fileStorageService);
        }
    }

    private ApplicationMigration migration(boolean uploadEnabled, Duration timeout, String bucket) {
        return new ApplicationMigration(
                fileStorageService,
                new MigrationProperties(
                        new MigrationProperties.Upload(uploadEnabled),
                        new MigrationProperties.Ratings(false),
                        timeout,
                        null),
                new ProductImageMigrationProperties(
                        new ProductImageMigrationProperties.Buckets(bucket),
                        new ProductImageMigrationProperties.DefaultImageDirectory(DEFAULT_DIRECTORY_PATH)));
    }
}
