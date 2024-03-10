package com.zufar.icedlatte.filestorage.aws;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.zufar.icedlatte.filestorage.exception.FileReadException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

@Service
@Slf4j
@RequiredArgsConstructor
public class AwsObjectUploader {

    private final AmazonS3 amazonS3;

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public void uploadFile(MultipartFile file, String bucketName, String fileName) {
        try (InputStream inputStream = file.getInputStream()) {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(file.getContentType());
            metadata.setContentLength(file.getSize());
            amazonS3.putObject(bucketName, fileName, inputStream, metadata);
        } catch (AmazonServiceException ase) {
            log.error("AWS couldn't process operation", ase);
            throw ase;
        } catch (SdkClientException sce) {
            log.error("AWS couldn't be contacted for a response", sce);
            throw sce;
        } catch (IOException e) {
            throw new FileReadException(fileName);
        }
    }

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public void uploadFileDirectory(String bucketName, String directoryPath) {
        TransferManager transferManager = new TransferManager(amazonS3);
        File directory = new File(directoryPath);
        transferManager.uploadDirectory(bucketName, "", directory, true);
    }
}
