package com.zufar.icedlatte.filestorage.aws;

import com.zufar.icedlatte.filestorage.ObjectStorage;
import com.zufar.icedlatte.filestorage.dto.FileMetadataDto;
import com.zufar.icedlatte.filestorage.exception.FileReadException;
import com.zufar.icedlatte.filestorage.exception.FileUploadException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnBean(S3Client.class)
public class AwsObjectStorage implements ObjectStorage {

    @Value("${spring.aws.link-expiration-time}")
    private String linkExpirationTime;

    @Value("${spring.aws.public-url-base:}")
    private String publicUrlBase;

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Override
    public boolean isConfigured() {
        return true;
    }

    @Override
    public void upload(MultipartFile file, String bucketName, String fileName) {
        try (InputStream inputStream = file.getInputStream()) {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();

            s3Client.putObject(putObjectRequest,
                    RequestBody.fromInputStream(inputStream, file.getSize()));
        } catch (S3Exception ex) {
            log.error("aws.s3.upload.error: bucket={}, key={}, exceptionClass={}",
                    bucketName, fileName, ex.getClass().getSimpleName(), ex);
            throw new FileUploadException(fileName, ex);
        } catch (SdkClientException ex) {
            log.error("aws.s3.upload.unreachable: bucket={}, key={}, exceptionClass={}",
                    bucketName, fileName, ex.getClass().getSimpleName(), ex);
            throw new FileUploadException(fileName, ex);
        } catch (IOException ex) {
            throw new FileReadException(fileName, ex);
        } catch (Exception ex) {
            throw new FileUploadException(fileName, ex);
        }
    }

    @Override
    public void uploadDirectory(String bucketName, String directoryPath) throws IOException {
        Path normalizedPath = Paths.get(directoryPath).normalize();
        if (!normalizedPath.toFile().getCanonicalPath()
                .startsWith(new java.io.File(directoryPath).getCanonicalPath())) {
            throw new SecurityException("Invalid directory path");
        }

        try (var pathStream = Files.walk(normalizedPath)) {
            pathStream.filter(Files::isRegularFile)
                    .forEach(filePath -> uploadFilePath(bucketName, normalizedPath, filePath));
        }
    }

    @Override
    public void delete(FileMetadataDto fileMetadataDto) {
        final String bucketName = fileMetadataDto.bucketName();
        final String fileName = fileMetadataDto.fileName();
        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .build();
            s3Client.deleteObject(deleteObjectRequest);
        } catch (S3Exception ex) {
            log.error("aws.s3.delete.error: bucket={}, key={}, exceptionClass={}",
                    bucketName, fileName, ex.getClass().getSimpleName(), ex);
            throw ex;
        } catch (SdkClientException ex) {
            log.error("aws.s3.delete.unreachable: bucket={}, key={}, exceptionClass={}",
                    bucketName, fileName, ex.getClass().getSimpleName(), ex);
            throw ex;
        }
    }

    @Override
    public Optional<String> getUrl(FileMetadataDto fileMetadataDto) {
        if (StringUtils.hasText(publicUrlBase)) {
            return Optional.of(publicUrlBase.stripTrailing() + "/" + fileMetadataDto.fileName());
        }
        try {
            String url = s3Presigner.presignGetObject(r -> r
                            .signatureDuration(Duration.parse(linkExpirationTime))
                            .getObjectRequest(g -> g
                                    .bucket(fileMetadataDto.bucketName())
                                    .key(fileMetadataDto.fileName())))
                    .url()
                    .toString();
            return Optional.of(url);
        } catch (SdkClientException ex) {
            log.error("aws.s3.presign.error: bucket={}, key={}, cause={}",
                    fileMetadataDto.bucketName(), fileMetadataDto.fileName(), ex.getMessage(), ex);
            return Optional.empty();
        }
    }

    @Override
    public List<String> listObjectKeys(String bucketName) {
        try {
            ListObjectsV2Request request = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .build();
            return s3Client.listObjectsV2Paginator(request)
                    .contents()
                    .stream()
                    .map(software.amazon.awssdk.services.s3.model.S3Object::key)
                    .toList();
        } catch (S3Exception ex) {
            log.error("aws.s3.list.error: bucket={}, exceptionClass={}", bucketName, ex.getClass().getSimpleName(), ex);
            return List.of();
        } catch (SdkClientException ex) {
            log.error("aws.s3.list.unreachable: bucket={}, exceptionClass={}",
                    bucketName, ex.getClass().getSimpleName(), ex);
            return List.of();
        }
    }

    private void uploadFilePath(String bucketName, Path normalizedPath, Path filePath) {
        String key = normalizedPath.relativize(filePath).toString().replace("\\", "/");
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();
            s3Client.putObject(putObjectRequest, RequestBody.fromFile(filePath));
        } catch (S3Exception ex) {
            log.error("aws.s3.upload.file_error: bucket={}, key={}, exceptionClass={}",
                    bucketName, key, ex.getClass().getSimpleName(), ex);
            throw new FileUploadException(key, ex);
        } catch (SdkClientException ex) {
            log.error("aws.s3.upload.file_unreachable: bucket={}, key={}, exceptionClass={}",
                    bucketName, key, ex.getClass().getSimpleName(), ex);
            throw new FileUploadException(key, ex);
        }
    }
}
