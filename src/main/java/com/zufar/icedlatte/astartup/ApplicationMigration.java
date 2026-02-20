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

    public ApplicationMigration(@Autowired(required = false) FileUploader fileUploader,
                                @Autowired(required = false) AwsProvider awsProvider,
                                @Autowired(required = false) FileMetadataSaver fileMetadataSaver) {
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
        var executor = Executors.newVirtualThreadPerTaskExecutor();
        CompletableFuture.runAsync(this::uploadFiles, executor)
                .thenComposeAsync(v -> CompletableFuture.supplyAsync(this::fetchMetadata, executor), executor)
                .thenAcceptAsync(this::saveMetadata, executor)
                .orTimeout(5, java.util.concurrent.TimeUnit.MINUTES)
                .whenComplete((v, e) -> {
                    executor.close();
                    if (e != null) log.warn("Migration completed with warnings", e);
                });
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

    private List<FileMetadataDto> fetchMetadata() {
        try {
            var fileMetadataDtos = awsProvider.getProductImagesFromAWS(productPictureBucket);
            log.info("Product pictures metadata retrieved from AWS");
            return fileMetadataDtos;
        } catch (software.amazon.awssdk.core.exception.SdkException e) {
            log.warn("Metadata retrieval failed due to AWS SDK error, continuing without AWS: {}", e.getMessage(), e);
            return List.of();
        }
    }

    private void saveMetadata(List<FileMetadataDto> fileMetadataDtos) {
        if (fileMetadataDtos.isEmpty()) {
            log.info("No metadata to save, skipping database operation");
            return;
        }
        try {
            fileMetadataSaver.saveAll(fileMetadataDtos);
            log.info("Product pictures metadata saved in database");
        } catch (DataAccessException e) {
            log.warn("Failed to save product pictures metadata: {}", e.getMessage(), e);
        }
    }
}
