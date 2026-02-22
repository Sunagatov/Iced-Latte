package com.zufar.icedlatte.filestorage.file;

import com.zufar.icedlatte.filestorage.aws.AwsObjectUploader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Slf4j
@Service
public class FileUploader {

    private final AwsObjectUploader awsObjectUploader;
    
    public FileUploader(@Autowired(required = false) AwsObjectUploader awsObjectUploader) {
        this.awsObjectUploader = awsObjectUploader;
    }

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public void upload(final MultipartFile file, String bucketName, String fileName) {
        if (awsObjectUploader != null) {
            awsObjectUploader.uploadFile(file, bucketName, fileName);
        } else {
            log.warn("file.upload.skipped: reason=aws_not_configured");
        }
    }


    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public void uploadDirectory(String bucketName, String directoryPath) {
        if (awsObjectUploader != null) {
            try {
                awsObjectUploader.uploadFileDirectory(bucketName, directoryPath);
            } catch (IOException e) {
                log.error("file.upload.io_error", e);
                throw new RuntimeException("Failed to upload directory due to I/O error", e);
            }
        } else {
            log.warn("file.dir_upload.skipped: reason=aws_not_configured");
        }
    }
}
