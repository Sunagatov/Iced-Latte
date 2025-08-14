package com.zufar.icedlatte.filestorage.file;

import com.amazonaws.AmazonServiceException;
import com.zufar.icedlatte.filestorage.aws.AwsObjectUploader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

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
        } catch (AmazonServiceException e) {
            log.error("AWS service error occurred while uploading directory", e);
            throw e;
        } catch (IOException e) {
            log.error("I/O error occurred while uploading directory", e);
            throw new RuntimeException("Failed to upload directory due to I/O error", e);
        }
    }
}
