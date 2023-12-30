package com.zufar.icedlatte;

import com.zufar.icedlatte.filestorage.file.FileUploader;
import com.zufar.icedlatte.filestorage.filemetadata.FileMetadataSaver;
import com.zufar.icedlatte.filestorage.dto.FileMetadataDto;
import com.zufar.icedlatte.filestorage.minio.MinioProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ApplicationMigration implements ApplicationRunner {

    private final static String DIRECTORY_PATH = "./products";

    @Value("${spring.minio.buckets.product-picture}")
    private String productPictureBucket;

    private final FileUploader fileUploader;
    private final MinioProvider minioProvider;
    private final FileMetadataSaver fileMetadataSaver;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        fileUploader.uploadDirectory(productPictureBucket, DIRECTORY_PATH);
        List<FileMetadataDto> fileMetadataDtos = minioProvider.getProductImagesFromMinio(productPictureBucket);
        fileMetadataSaver.saveAll(fileMetadataDtos);
    }
}
