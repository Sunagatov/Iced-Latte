package com.zufar.icedlatte.common.filestorage;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Service
@RequiredArgsConstructor
public class MinioObjectDownloader {

    private final MinioClient minioClient;
    private final IsMinioBucketAbsentChecker isMinioBucketAbsentChecker;
    private final MinioBucketCreator minioBucketCreator;

    @Value("${spring.minio.bucket}")
    private String bucketName;

    @Value("${spring.minio.url}")
    private String minioStorageUrl;

    public void downloadUserAvatar(String fileUrl, HttpServletResponse response) {
        try {
            String objectName = fileUrl.substring(fileUrl.lastIndexOf('/') + 1);

            GetObjectArgs getObjectArgs = GetObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .build();

            try (InputStream stream = minioClient.getObject(getObjectArgs)) {
                if (objectName.endsWith(".png")) {
                    response.setContentType("image/png");
                } else if (objectName.endsWith(".jpg") || objectName.endsWith(".jpeg")) {
                    response.setContentType("image/jpeg");
                }

                stream.transferTo(response.getOutputStream());
                response.flushBuffer();
            }
        } catch (Exception e) {
            throw new RuntimeException("Error retrieving file", e);
        }
    }
}
