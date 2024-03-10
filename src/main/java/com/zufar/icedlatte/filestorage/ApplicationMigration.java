package com.zufar.icedlatte.filestorage;

import com.zufar.icedlatte.filestorage.aws.AwsProvider;
import com.zufar.icedlatte.filestorage.file.FileUploader;
import com.zufar.icedlatte.filestorage.filemetadata.FileMetadataSaver;
import com.zufar.icedlatte.filestorage.dto.FileMetadataDto;
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

    @Value("${spring.aws.buckets.products}")
    private String productPictureBucket;

    @Value("${spring.aws.default-image-directory.products}")
    private String directoryPath;

    private final FileUploader fileUploader;
    private final AwsProvider awsProvider;
    private final FileMetadataSaver fileMetadataSaver;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("Product pictures upload was started, directory path: {}", directoryPath);
        fileUploader.uploadDirectory(productPictureBucket, directoryPath);
        Thread.sleep(5000);
        log.info("Product pictures upload was finished");
        List<FileMetadataDto> fileMetadataDtos = awsProvider.getProductImagesFromAWS(productPictureBucket);
        log.info("Product pictures metadata was retrieved from AWS");
        fileMetadataSaver.saveAll(fileMetadataDtos);
        log.info("Product pictures metadata was saved in postgresql database");
    }
}
