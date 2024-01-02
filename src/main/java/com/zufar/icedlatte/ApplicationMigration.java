package com.zufar.icedlatte;

import com.zufar.icedlatte.filestorage.file.FileDeleter;
import com.zufar.icedlatte.filestorage.file.FileUploader;
import com.zufar.icedlatte.filestorage.filemetadata.FileMetadataSaver;
import com.zufar.icedlatte.filestorage.dto.FileMetadataDto;
import com.zufar.icedlatte.filestorage.minio.MinioObjectDeleter;
import com.zufar.icedlatte.filestorage.minio.MinioProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApplicationMigration implements ApplicationRunner {

    @Value("${spring.minio.buckets.product-picture}")
    private String productPictureBucket;

    @Value("${spring.minio.default-image-directory.products}")
    private String directoryPath;

    private final FileUploader fileUploader;
    private final MinioProvider minioProvider;
    private final FileMetadataSaver fileMetadataSaver;
    private final FileDeleter fileDeleter;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("Deleting old pictures from products bucket was started");
        fileDeleter.deleteAll(productPictureBucket);
        Thread.sleep(5000);
        log.info("Bucket is ready for new pictures");
        log.info("Product pictures upload was started");
        fileUploader.uploadDirectory(productPictureBucket, directoryPath);
        Thread.sleep(5000);
        log.info("Product pictures upload was finished");
        List<FileMetadataDto> fileMetadataDtos = minioProvider.getProductImagesFromMinio(productPictureBucket);
        log.info("Product pictures metadata was retrieved from minio");
        fileMetadataSaver.saveAll(fileMetadataDtos);
        log.info("Product pictures metadata was saved in postgresql database");
    }
}
