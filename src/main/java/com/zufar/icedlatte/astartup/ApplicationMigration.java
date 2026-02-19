package com.zufar.icedlatte.astartup;

import com.zufar.icedlatte.filestorage.exception.FileReadException;
import com.zufar.icedlatte.filestorage.exception.FileUploadException;
import com.zufar.icedlatte.filestorage.aws.AwsProvider;
import com.zufar.icedlatte.filestorage.dto.FileMetadataDto;
import com.zufar.icedlatte.filestorage.file.FileUploader;
import com.zufar.icedlatte.filestorage.filemetadata.FileMetadataSaver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.Duration;
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

    public ApplicationMigration(@Autowired(required = false) FileUploader fileUploader,
                                @Autowired(required = false) AwsProvider awsProvider,
                                FileMetadataSaver fileMetadataSaver) {
        this.fileUploader = fileUploader;
        this.awsProvider = awsProvider;
        this.fileMetadataSaver = fileMetadataSaver;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!isAwsConfigured()) {
            log.info("AWS configuration not available, skipping file migration");
            return;
        }
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            CompletableFuture.runAsync(this::uploadFiles, executor)
                    .thenCompose(v -> CompletableFuture.supplyAsync(this::fetchMetadata, executor))
                    .thenAccept(this::saveMetadata)
                    .join();
        } catch (java.util.concurrent.CompletionException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            log.warn("Migration completed with warnings: {}", cause.getMessage(), cause);
        }
    }

    private boolean isAwsConfigured() {
        return fileUploader != null && awsProvider != null
                && productPictureBucket != null && !productPictureBucket.isEmpty()
                && directoryPath != null && !directoryPath.isEmpty();
    }

    private void uploadFiles() {
        try {
            log.info("Product pictures upload started, directory path: {}", directoryPath);
            fileUploader.uploadDirectory(productPictureBucket, directoryPath);
            log.info("Product pictures upload finished");
        } catch (FileUploadException e) {
            log.warn("Upload failed, continuing without AWS: {}", e.getMessage(), e);
        } catch (FileReadException e) {
            log.warn("File read error during upload, continuing without AWS: {}", e.getMessage(), e);
        }
    }

    private java.util.List<FileMetadataDto> fetchMetadata() {
        try {
            Thread.sleep(Duration.ofSeconds(5));
            var fileMetadataDtos = awsProvider.getProductImagesFromAWS(productPictureBucket);
            log.info("Product pictures metadata retrieved from AWS");
            return fileMetadataDtos;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Metadata retrieval interrupted: {}", e.getMessage(), e);
            return java.util.Collections.emptyList();
        } catch (software.amazon.awssdk.core.exception.SdkException e) {
            log.warn("Metadata retrieval failed due to AWS SDK error, continuing without AWS: {}", e.getMessage(), e);
            return java.util.Collections.emptyList();
        }
    }

    private void saveMetadata(java.util.List<FileMetadataDto> fileMetadataDtos) {
        if (fileMetadataDtos.isEmpty()) {
            log.info("No metadata to save, skipping database operation");
            return;
        }
        try {
            fileMetadataSaver.saveAll(fileMetadataDtos);
            log.info("Product pictures metadata saved in database");
        } catch (RuntimeException e) {
            log.warn("Failed to save product pictures metadata: {}", e.getMessage(), e);
        }
    }
}
