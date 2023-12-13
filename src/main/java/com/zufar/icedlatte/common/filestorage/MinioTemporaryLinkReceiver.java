package com.zufar.icedlatte.common.filestorage;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.time.Duration;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class MinioTemporaryLinkReceiver {

    @Value("${spring.minio.expiration-time.avatar-link}")
    private String expirationTime;

    private final AmazonS3 amazonS3;

    public URL generatePresignedUrl(String bucketName, String fileName) {
        GeneratePresignedUrlRequest generatePresignedUrlRequest = new GeneratePresignedUrlRequest(bucketName, fileName)
                .withMethod(HttpMethod.GET)
                .withExpiration(getExpirationDate());

        return amazonS3.generatePresignedUrl(generatePresignedUrlRequest);
    }

    private Date getExpirationDate() {
        Duration duration = Duration.parse(expirationTime);
        return new Date(System.currentTimeMillis() + duration.toMillis());
    }
}
