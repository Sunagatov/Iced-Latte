package com.zufar.icedlatte.common.filestorage;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.zufar.icedlatte.common.dto.FileMetadataDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.time.Duration;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class MinioTemporaryLinkReceiver {

    @Value("${spring.minio.link-expiration-time}")
    private String linkExpirationTime;

    private final AmazonS3 amazonS3;

    public String generatePresignedUrlAsString(FileMetadataDto fileMetadata) {
        return generatePresignedUrl(fileMetadata).toString();
    }

    public URL generatePresignedUrl(FileMetadataDto fileMetadata) {
        final String bucketName = fileMetadata.bucketName();
        final String fileName = fileMetadata.fileName();

        GeneratePresignedUrlRequest generatePresignedUrlRequest = new GeneratePresignedUrlRequest(bucketName, fileName)
                .withMethod(HttpMethod.GET)
                .withExpiration(getExpirationDate());

        return amazonS3.generatePresignedUrl(generatePresignedUrlRequest);
    }

    private Date getExpirationDate() {
        Duration duration = Duration.parse(linkExpirationTime);
        return new Date(System.currentTimeMillis() + duration.toMillis());
    }
}
