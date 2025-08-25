package com.zufar.icedlatte.filestorage.aws;

import com.zufar.icedlatte.filestorage.dto.FileMetadataDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnBean(S3Client.class)
public class AwsObjectDeleter {

    private final S3Client s3Client;

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public void deleteFile(FileMetadataDto fileMetadataDto) {
        final String bucketName = fileMetadataDto.bucketName();
        final String fileName = fileMetadataDto.fileName();
        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .build();
            s3Client.deleteObject(deleteObjectRequest);
        } catch (S3Exception ase) {
            log.error("AWS couldn't process operation", ase);
            throw ase;
        } catch (SdkClientException sce) {
            log.error("AWS couldn't be contacted for a response", sce);
            throw sce;
        }
    }
}
