package com.zufar.icedlatte.filestorage.aws;

import com.zufar.icedlatte.filestorage.exception.FileReadException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@Slf4j
@RequiredArgsConstructor
public class AwsObjectUploader {

    private final S3Client s3Client;

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public void uploadFile(MultipartFile file, String bucketName, String fileName) {
        try (InputStream inputStream = file.getInputStream()) {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();
            
            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(inputStream, file.getSize()));
        } catch (S3Exception ase) {
            log.error("AWS couldn't process operation", ase);
            throw ase;
        } catch (SdkClientException sce) {
            log.error("AWS couldn't be contacted for a response", sce);
            throw sce;
        } catch (IOException e) {
            throw new FileReadException(fileName);
        }
    }

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public void uploadFileDirectory(String bucketName, String directoryPath) throws IOException {
        Path normalizedPath = Paths.get(directoryPath).normalize();
        if (!normalizedPath.toFile().getCanonicalPath().startsWith(new java.io.File(directoryPath).getCanonicalPath())) {
            throw new SecurityException("Invalid directory path");
        }
        
        try (var pathStream = Files.walk(normalizedPath)) {
            pathStream
                    .filter(Files::isRegularFile)
                    .forEach(filePath -> {
                        try {
                            String key = normalizedPath.relativize(filePath).toString().replace("\\", "/");
                            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                                    .bucket(bucketName)
                                    .key(key)
                                    .build();
                            s3Client.putObject(putObjectRequest, RequestBody.fromFile(filePath));
                        } catch (Exception e) {
                            log.error("Failed to upload file: {}", filePath, e);
                            throw new RuntimeException(e);
                        }
                    });
        }
    }
}
