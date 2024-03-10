package com.zufar.icedlatte.filestorage.file;

import com.zufar.icedlatte.filestorage.aws.AwsObjectUploader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileUploader {

    private final AwsObjectUploader awsObjectUploader;

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public void upload(final MultipartFile file, String bucketName, String fileName) {
        awsObjectUploader.uploadFile(file, bucketName, fileName);
    }

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public void uploadDirectory(String bucketName, String directoryPath) {
        try {
            awsObjectUploader.uploadFileDirectory(bucketName, directoryPath);
        } catch (Exception e) {
            log.error("Failed to upload directory", e);
        }
    }
}
