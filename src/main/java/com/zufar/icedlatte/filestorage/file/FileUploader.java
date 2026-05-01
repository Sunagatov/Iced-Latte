package com.zufar.icedlatte.filestorage.file;

import com.zufar.icedlatte.filestorage.aws.AwsObjectUploader;
import com.zufar.icedlatte.filestorage.exception.FileUploadException;
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
        if (awsObjectUploader == null) {
            log.info("storage.aws.disabled: file upload will be skipped");
        }
    }

    public boolean isStorageConfigured() {
        return awsObjectUploader != null;
    }

    @Transactional(propagation = Propagation.REQUIRED,
            isolation = Isolation.READ_COMMITTED)
    public boolean upload(final MultipartFile file,
                          String bucketName,
                          String fileName) {
        if (isStorageConfigured()) {
            awsObjectUploader.uploadFile(file, bucketName, fileName);
            return true;
        }
        log.debug("file.upload.skipped: reason=aws_not_configured");
        return false;
    }


    @Transactional(propagation = Propagation.REQUIRED,
            isolation = Isolation.READ_COMMITTED)
    public void uploadDirectory(String bucketName,
                                String directoryPath) {
        if (isStorageConfigured()) {
            try {
                awsObjectUploader.uploadFileDirectory(bucketName, directoryPath);
            } catch (IOException e) {
                log.error("file.upload.io_error: target={}, exceptionClass={}",
                        directoryPath, e.getClass().getSimpleName(), e);
                throw new FileUploadException(directoryPath, e);
            }
        } else {
            log.debug("file.dir_upload.skipped: reason=aws_not_configured");
        }
    }
}
