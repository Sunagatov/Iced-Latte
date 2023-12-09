package com.zufar.icedlatte.common.filestorage;

import io.minio.MinioClient;
import io.minio.UploadObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@Slf4j
@RequiredArgsConstructor
public class MinioObjectDownloader {

    private final MinioClient minioClient;

    @Async
    public void saveFile(String fileName, MultipartFile file, String backedName) {
        try {
            minioClient.uploadObject(
                    UploadObjectArgs.builder()
                            .bucket(backedName)
                            .object(fileName)
                            .filename(fileName, file.getSize())
                            .build()
            );
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }
}

