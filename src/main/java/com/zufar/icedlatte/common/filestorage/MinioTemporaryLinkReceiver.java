package com.zufar.icedlatte.common.filestorage;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class MinioTemporaryLinkReceiver {

    private final AmazonS3 amazonS3;

    public URL generatePresignedUrl(String bucketName, String fileName) {
        GeneratePresignedUrlRequest generatePresignedUrlRequest = new GeneratePresignedUrlRequest(bucketName, fileName)
                .withMethod(HttpMethod.GET)
                .withExpiration(getExpirationDate());

        return amazonS3.generatePresignedUrl(generatePresignedUrlRequest);
    }

    private static Date getExpirationDate() {
        return new Date(System.currentTimeMillis() + 5 * 60 * 1000);
    }
}
