package com.zufar.icedlatte.common.filestorage;

import com.zufar.icedlatte.common.exception.filestorage.MinioGetException;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioObjectGetter {

    private final MinioClient minioClient;

    public MultipartFile downloadFile(String fileName, String bucketName) {
        try {
            return (MultipartFile) minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .build()
            );
        } catch (Exception e) {
            log.info("Failed to download file: {}", e.getMessage());
            throw new MinioGetException(fileName);
        }
    }
}
