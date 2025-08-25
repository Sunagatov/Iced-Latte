package com.zufar.icedlatte.astartup;

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

    public ApplicationMigration(FileUploader fileUploader,
                                @Autowired(required = false) AwsProvider awsProvider,
                                FileMetadataSaver fileMetadataSaver) {
        this.fileUploader = fileUploader;
        this.awsProvider = awsProvider;
        this.fileMetadataSaver = fileMetadataSaver;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (awsProvider == null ||
                productPictureBucket == null || productPictureBucket.isEmpty() ||
                directoryPath == null || directoryPath.isEmpty()) {
            log.info("AWS configuration not available, skipping file migration");
            return;
        }

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var uploadTask = CompletableFuture.runAsync(() -> {
                try {
                    log.info("Product pictures upload started, directory path: {}", directoryPath);
                    fileUploader.uploadDirectory(productPictureBucket, directoryPath);
                    log.info("Product pictures upload finished");
                } catch (Exception e) {
                    log.warn("Upload failed, continuing without AWS: {}", e.getMessage());
                }
            }, executor);

            var metadataTask = uploadTask.thenCompose(result ->
                    CompletableFuture.supplyAsync(() -> {
                        try {
                            Thread.sleep(Duration.ofSeconds(5));
                            var fileMetadataDtos = awsProvider.getProductImagesFromAWS(productPictureBucket);
                            log.info("Product pictures metadata retrieved from AWS");
                            return fileMetadataDtos;
                        } catch (Exception e) {
                            log.warn("Metadata retrieval failed, continuing without AWS: {}", e.getMessage());
                            return java.util.Collections.<FileMetadataDto>emptyList();
                        }
                    }, executor)
            );

            metadataTask.thenAccept(fileMetadataDtos -> {
                if (!fileMetadataDtos.isEmpty()) {
                    fileMetadataSaver.saveAll(fileMetadataDtos);
                    log.info("Product pictures metadata saved in database");
                } else {
                    log.info("No metadata to save, skipping database operation");
                }
            }).join();

        } catch (Exception e) {
            log.warn("Migration completed with warnings: {}", e.getMessage());
        }
    }
}
