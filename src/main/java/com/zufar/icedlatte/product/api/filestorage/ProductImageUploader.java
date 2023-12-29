package com.zufar.icedlatte.product.api.filestorage;

import com.zufar.icedlatte.common.filestorage.api.FileUploader;
import com.zufar.icedlatte.common.filestorage.api.MinioFileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductImageUploader {

    @Value("${spring.minio.buckets.product-picture}")
    private String productPictureBucket;
    private final String directoryPath = "./products";


    private final MinioFileService minioFileService;
    private final FileUploader fileUploader;

    public void updateProductsFileMetadata() {
        minioFileService.uploadProductImagesToDb(productPictureBucket);
    }

    public void uploadProductImagesToMinio() {
        fileUploader.uploadDirectory(productPictureBucket, directoryPath);
    }
}
