package com.zufar.icedlatte.astartup;

import com.zufar.icedlatte.filestorage.exception.FileReadException;
import com.zufar.icedlatte.filestorage.exception.FileUploadException;
import com.zufar.icedlatte.filestorage.aws.AwsProvider;
import com.zufar.icedlatte.filestorage.dto.FileMetadataDto;
import com.zufar.icedlatte.filestorage.file.FileUploader;
import com.zufar.icedlatte.filestorage.filemetadata.FileMetadataDeleter;
import com.zufar.icedlatte.filestorage.filemetadata.FileMetadataSaver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

@Slf4j
@Component
public class ApplicationMigration implements ApplicationRunner {

    @Value("${spring.aws.buckets.products:}")
    private String productPictureBucket;

    @Value("${spring.aws.default-image-directory.products:}")
    private String directoryPath;

    private final FileUploader fileUploader;
    private final AwsProvider awsProvider;
    private final FileMetadataSaver fileMetadataSaver;
    private final FileMetadataDeleter fileMetadataDeleter;

    public ApplicationMigration(@Autowired(required = false) FileUploader fileUploader,
                                @Autowired(required = false) AwsProvider awsProvider,
                                @Autowired(required = false) FileMetadataSaver fileMetadataSaver,
                                @Autowired(required = false) FileMetadataDeleter fileMetadataDeleter) {
        this.fileUploader = fileUploader;
        this.awsProvider = awsProvider;
        this.fileMetadataSaver = fileMetadataSaver;
        this.fileMetadataDeleter = fileMetadataDeleter;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!isAwsConfigured()) {
            log.info("migration.aws.skipped: reason=not_configured");
            return;
        }
        var executor = Executors.newVirtualThreadPerTaskExecutor();
        CompletableFuture.runAsync(this::uploadFiles, executor)
                .thenComposeAsync(v -> CompletableFuture.supplyAsync(this::fetchMetadata, executor), executor)
                .thenAcceptAsync(this::saveMetadata, executor)
                .orTimeout(5, java.util.concurrent.TimeUnit.MINUTES)
                .whenComplete((v, e) -> {
                    executor.close();
                    if (e != null) log.error("migration.aws.error: message={}", e.getMessage(), e);
                });
    }

    private boolean isAwsConfigured() {
        return fileUploader != null && awsProvider != null
                && productPictureBucket != null && !productPictureBucket.isEmpty()
                && directoryPath != null && !directoryPath.isEmpty();
    }

    private void uploadFiles() {
        try {
            log.info("migration.upload.start: path={}", directoryPath);
            long t0 = System.currentTimeMillis();
            fileUploader.uploadDirectory(productPictureBucket, directoryPath);
            log.info("migration.upload.finish: durationMs={}", System.currentTimeMillis() - t0);
        } catch (FileUploadException e) {
            log.warn("migration.upload.error: reason={}", e.getMessage(), e);
        } catch (FileReadException e) {
            log.warn("migration.upload.read_error: reason={}", e.getMessage(), e);
        }
    }

    private List<FileMetadataDto> fetchMetadata() {
        try {
            var fileMetadataDtos = awsProvider.getProductImagesFromAWS(productPictureBucket);
            log.info("migration.metadata.fetched");
            return fileMetadataDtos;
        } catch (software.amazon.awssdk.core.exception.SdkException e) {
            log.warn("migration.metadata.fetch_error: reason={}", e.getMessage(), e);
            return List.of();
        }
    }

    private void saveMetadata(List<FileMetadataDto> fileMetadataDtos) {
        if (fileMetadataDtos.isEmpty()) {
            log.info("migration.metadata.skipped: reason=empty");
            return;
        }
        try {
            if (fileMetadataDeleter != null) {
                fileMetadataDeleter.deleteByBucketName(productPictureBucket);
            }
            fileMetadataSaver.saveAll(fileMetadataDtos);
            log.info("migration.metadata.saved");
        } catch (DataAccessException e) {
            log.warn("migration.metadata.save_error: reason={}", e.getMessage(), e);
        }
    }
}
