package com.zufar.icedlatte.common.filestorage;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioObjectDeleter {

    private final AmazonS3 amazonS3;

    public void deleteFile(String fileName, String bucketName) {
        try {
            amazonS3.deleteObject(bucketName, fileName);
        } catch (AmazonServiceException ase) {
            log.error("Amazon S3 couldn't process operation", ase);
            throw ase;
        } catch (SdkClientException sce) {
            log.error("Amazon S3 couldn't be contacted for a response", sce);
            throw sce;
        }
    }
}
