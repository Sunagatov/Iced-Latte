package com.zufar.icedlatte.filestorage.aws;

import com.zufar.icedlatte.filestorage.exception.FileReadException;
import com.zufar.icedlatte.filestorage.exception.FileUploadException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
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
@ConditionalOnBean(S3Client.class)
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
            throw new FileUploadException(fileName, ase);
        } catch (SdkClientException sce) {
            log.error("AWS couldn't be contacted for a response", sce);
            throw new FileUploadException(fileName, sce);
        } catch (IOException e) {
            throw new FileReadException(fileName, e);
        } catch (Exception e) {
            throw new FileUploadException(fileName, e);
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
                        } catch (S3Exception e) {
                            log.error("AWS S3 error uploading file: {}", filePath, e);
                            throw new FileUploadException(filePath.toString(), e);
                        } catch (SdkClientException e) {
                            log.error("AWS SDK client error uploading file: {}", filePath, e);
                            throw new FileUploadException(filePath.toString(), e);
                        }
                    });
        }
    }
}
