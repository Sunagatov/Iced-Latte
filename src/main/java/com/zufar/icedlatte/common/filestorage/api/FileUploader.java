package com.zufar.icedlatte.common.filestorage.api;

import com.zufar.icedlatte.common.filestorage.minio.MinioObjectUploader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class FileUploader {

    private final MinioObjectUploader minioObjectUploader;

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public void upload(final MultipartFile file, String bucketName, String fileName) {
        minioObjectUploader.uploadFile(file, bucketName, fileName);
    }

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public void uploadDirectory(String bucketName, String directoryPath) {
        minioObjectUploader.uploadFileDirectory(bucketName, directoryPath);
    }
}
