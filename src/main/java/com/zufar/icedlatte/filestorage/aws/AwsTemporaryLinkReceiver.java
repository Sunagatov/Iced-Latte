package com.zufar.icedlatte.filestorage.aws;

import com.amazonaws.HttpMethod;
import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.zufar.icedlatte.filestorage.dto.FileMetadataDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.time.Duration;
import java.util.Date;
import java.util.logging.Level;

@Slf4j
@Service
@RequiredArgsConstructor
public class AwsTemporaryLinkReceiver {

    @Value("${spring.aws.link-expiration-time}")
    private String linkExpirationTime;

    private final AmazonS3 amazonS3;

    public String generatePresignedUrlAsString(FileMetadataDto fileMetadata) {
        return generatePresignedUrl(fileMetadata).toString();
    }


    public URL generatePresignedUrl(FileMetadataDto fileMetadata) {
        final String bucketName = fileMetadata.bucketName();
        final String fileName = fileMetadata.fileName();
        Date expirationDate = new Date(System.currentTimeMillis() + Duration.parse(linkExpirationTime).toMillis());
        HttpMethod httpMethod = HttpMethod.GET;

        try {
            return amazonS3.generatePresignedUrl(bucketName, fileName, expirationDate, httpMethod);
        } catch (SdkClientException e) {
            log.error("Error generating presigned URL", e);
            return null;
        }
    }
}
