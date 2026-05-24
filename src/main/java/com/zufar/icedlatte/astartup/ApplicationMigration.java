package com.zufar.icedlatte.astartup;

import com.zufar.icedlatte.filestorage.api.FileStorageApi;
import com.zufar.icedlatte.filestorage.exception.FileReadException;
import com.zufar.icedlatte.filestorage.exception.FileUploadException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApplicationMigration implements ApplicationRunner {

    @Value("${spring.aws.buckets.products:}")
    private String productPictureBucket;

    @Value("${spring.aws.default-image-directory.products:}")
    private String directoryPath;

    @Value("${migration.upload.enabled:false}")
    private boolean uploadEnabled;

    @Value("${migration.timeout-minutes:5}")
    private int timeoutMinutes;

    private final FileStorageApi fileStorageApi;

    @Override
    public void run(@NonNull ApplicationArguments args) {
        if (!isAwsConfigured()) {
            log.info("migration.aws.skipped: reason=not_configured");
            return;
        }
        var executor = Executors.newVirtualThreadPerTaskExecutor();
        Runnable uploadFilesJob =
                uploadEnabled ? this::uploadFiles : () -> log.info("migration.upload.skipped: reason=disabled");

        CompletableFuture.runAsync(uploadFilesJob, executor)
                .thenRunAsync(this::refreshMetadataIndex, executor)
                .orTimeout(timeoutMinutes, java.util.concurrent.TimeUnit.MINUTES)
                .whenComplete((_, e) -> {
                    executor.close();
                    if (e != null) {
                        log.error("migration.aws.error: exceptionClass={}", e.getClass().getSimpleName(), e);
                    }
                });
    }

    private boolean isAwsConfigured() {
        return productPictureBucket != null
                && !productPictureBucket.isEmpty()
                && directoryPath != null
                && !directoryPath.isEmpty()
                && fileStorageApi.isEnabled();
    }

    private void uploadFiles() {
        try {
            log.info("migration.upload.start: path={}", directoryPath);
            long t0 = System.currentTimeMillis();
            fileStorageApi.storeDirectory(productPictureBucket, directoryPath);
            long durationMs = System.currentTimeMillis() - t0;
            log.info("migration.upload.finish: bucket={}, path={}, durationMs={}", productPictureBucket, directoryPath, durationMs);
        } catch (FileUploadException e) {
            log.warn("migration.upload.error: exceptionClass={}", e.getClass().getSimpleName(), e);
        } catch (FileReadException e) {
            log.warn("migration.upload.read_error: exceptionClass={}", e.getClass().getSimpleName(), e);
        } catch (java.io.IOException e) {
            log.warn("migration.upload.io_error: exceptionClass={}", e.getClass().getSimpleName(), e);
        }
    }

    private void refreshMetadataIndex() {
        try {
            fileStorageApi.refreshBucketIndex(productPictureBucket);
            log.info("migration.metadata.refreshed: bucket={}", productPictureBucket);
        } catch (software.amazon.awssdk.core.exception.SdkException e) {
            log.warn("migration.metadata.refresh_error: exceptionClass={}", e.getClass().getSimpleName(), e);
        } catch (DataAccessException e) {
            log.warn("migration.metadata.persist_error: exceptionClass={}", e.getClass().getSimpleName(), e);
        }
    }
}
