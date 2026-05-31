package com.zufar.icedlatte.astartup;

import java.time.Duration;

import org.jspecify.annotations.NonNull;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import com.zufar.icedlatte.filestorage.api.BucketIndexMaintenanceApi;
import com.zufar.icedlatte.filestorage.exception.FileReadException;
import com.zufar.icedlatte.filestorage.exception.FileUploadException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApplicationMigration implements ApplicationRunner {

    private final BucketIndexMaintenanceApi bucketIndexMaintenanceApi;
    private final MigrationProperties migrationProperties;
    private final ProductImageMigrationProperties productImageMigrationProperties;

    @Override
    public void run(@NonNull ApplicationArguments args) {
        if (!isAwsConfigured()) {
            log.info("migration.aws.skipped: reason=not_configured");
            return;
        }
        StartupTaskRunner.runAsync("AWS startup migration", migrationProperties.timeout(), this::runMigration);
    }

    private boolean isAwsConfigured() {
        String productBucket = productBucket();
        String directoryPath = directoryPath();
        return productBucket != null
                && !productBucket.isEmpty()
                && directoryPath != null
                && !directoryPath.isEmpty()
                && bucketIndexMaintenanceApi.isEnabled();
    }

    private void runMigration() {
        if (migrationProperties.upload().enabled()) {
            uploadFiles();
        } else {
            log.info("migration.upload.skipped: reason=disabled");
        }
        refreshMetadataIndex();
    }

    private void uploadFiles() {
        try {
            log.info("migration.upload.start: path={}", directoryPath());
            long t0 = System.nanoTime();
            bucketIndexMaintenanceApi.storeDirectory(productBucket(), directoryPath());
            long durationMs = Duration.ofNanos(System.nanoTime() - t0).toMillis();
            log.info(
                    "migration.upload.finish: bucket={}, path={}, durationMs={}",
                    productBucket(),
                    directoryPath(),
                    durationMs);
        } catch (FileUploadException e) {
            log.warn("migration.upload.error: exceptionClass={}", e.getClass().getSimpleName(), e);
        } catch (FileReadException e) {
            log.warn(
                    "migration.upload.read_error: exceptionClass={}",
                    e.getClass().getSimpleName(),
                    e);
        } catch (java.io.IOException e) {
            log.warn(
                    "migration.upload.io_error: exceptionClass={}", e.getClass().getSimpleName(), e);
        }
    }

    private void refreshMetadataIndex() {
        try {
            bucketIndexMaintenanceApi.refreshBucketIndex(productBucket());
            log.info("migration.metadata.refreshed: bucket={}", productBucket());
        } catch (software.amazon.awssdk.core.exception.SdkException e) {
            log.warn(
                    "migration.metadata.refresh_error: exceptionClass={}",
                    e.getClass().getSimpleName(),
                    e);
        } catch (DataAccessException e) {
            log.warn(
                    "migration.metadata.persist_error: exceptionClass={}",
                    e.getClass().getSimpleName(),
                    e);
        }
    }

    private String productBucket() {
        return productImageMigrationProperties.productBucket();
    }

    private String directoryPath() {
        return productImageMigrationProperties.productDirectoryPath();
    }
}
