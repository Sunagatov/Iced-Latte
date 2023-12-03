package com.zufar.icedlatte.user.endpoint;

import com.zufar.icedlatte.common.filestorage.IsMinioBucketAbsentChecker;
import com.zufar.icedlatte.common.filestorage.MinioBucketCreator;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileStorageService {

    private final MinioClient minioClient;
    private final IsMinioBucketAbsentChecker isMinioBucketAbsentChecker;
    private final MinioBucketCreator minioBucketCreator;

    @Value("${spring.minio.bucket}")
    private String bucketName;

    @Value("${spring.minio.url}")
    private String minioStorageUrl;

    public void uploadUserAvatar(UUID userId, MultipartFile file) {
        try {
            if (isMinioBucketAbsentChecker.isAbsent(bucketName)) {
                minioBucketCreator.create(bucketName);
            }

            String fileName = "avatars/" + userId + "/" + file.getOriginalFilename();

            PutObjectArgs objectArgs = PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(fileName)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build();

            minioClient.putObject(objectArgs);

            String fileUrl = minioStorageUrl + ":9000/" + bucketName + "/" + fileName;

            GetObjectArgs getObjectArgs = GetObjectArgs.builder().bucket(bucketName).object(fileName).build();

            GetObjectResponse object = minioClient.getObject(getObjectArgs);
        } catch (Exception e) {
            throw new RuntimeException("Error uploading file", e);
        }
    }
}
