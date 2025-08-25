package com.zufar.icedlatte.filestorage.aws;

import com.zufar.icedlatte.filestorage.dto.FileMetadataDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.net.URL;
import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnBean(S3Client.class)
public class AwsTemporaryLinkReceiver {

    @Value("${spring.aws.link-expiration-time}")
    private String linkExpirationTime;

    private final S3Client s3Client;

    public String generatePresignedUrlAsString(FileMetadataDto fileMetadata) {
        URL url = generatePresignedUrl(fileMetadata);
        return url != null ? url.toString() : null;
    }

    public URL generatePresignedUrl(FileMetadataDto fileMetadata) {
        final String bucketName = fileMetadata.bucketName();
        final String fileName = fileMetadata.fileName();
        Duration expiration = Duration.parse(linkExpirationTime);

        try (S3Presigner presigner = S3Presigner.builder()
                .region(s3Client.serviceClientConfiguration().region())
                .credentialsProvider(s3Client.serviceClientConfiguration().credentialsProvider())
                .build()) {
            
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .build();

            GetObjectPresignRequest getObjectPresignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(expiration)
                    .getObjectRequest(getObjectRequest)
                    .build();

            PresignedGetObjectRequest presignedGetObjectRequest = presigner.presignGetObject(getObjectPresignRequest);
            return presignedGetObjectRequest.url();
        } catch (SdkClientException e) {
            log.error("Error generating presigned URL", e);
            return null;
        }
    }
}
