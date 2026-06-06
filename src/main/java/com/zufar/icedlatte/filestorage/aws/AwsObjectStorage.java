package com.zufar.icedlatte.filestorage.aws;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.NonNull;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriUtils;

import com.zufar.icedlatte.common.exception.BadRequestException;
import com.zufar.icedlatte.filestorage.api.dto.FileMetadataDto;
import com.zufar.icedlatte.filestorage.exception.FileListException;
import com.zufar.icedlatte.filestorage.exception.FileReadException;
import com.zufar.icedlatte.filestorage.exception.FileUploadException;
import com.zufar.icedlatte.filestorage.service.ObjectStorage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnBean(S3Client.class)
public class AwsObjectStorage implements ObjectStorage {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final AwsProperties awsProperties;

    @Override
    public boolean isConfigured() {
        return true;
    }

    @Override
    public void upload(@NonNull MultipartFile file, @NonNull String bucketName, @NonNull String fileName) {
        try (InputStream inputStream = file.getInputStream()) {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .contentDisposition("inline")
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(inputStream, file.getSize()));
        } catch (S3Exception ex) {
            log.error(
                    "aws.s3.upload.error: bucket={}, key={}, exceptionClass={}",
                    bucketName,
                    fileName,
                    ex.getClass().getSimpleName(),
                    ex);
            throw new FileUploadException(fileName, ex);
        } catch (SdkClientException ex) {
            log.error(
                    "aws.s3.upload.unreachable: bucket={}, key={}, exceptionClass={}",
                    bucketName,
                    fileName,
                    ex.getClass().getSimpleName(),
                    ex);
            throw new FileUploadException(fileName, ex);
        } catch (IOException ex) {
            throw new FileReadException(fileName, ex);
        } catch (Exception ex) {
            throw new FileUploadException(fileName, ex);
        }
    }

    @Override
    public void uploadDirectory(@NonNull String bucketName, @NonNull String directoryPath) throws IOException {
        Path normalizedPath = Paths.get(directoryPath).normalize().toRealPath(LinkOption.NOFOLLOW_LINKS);
        if (!Files.isDirectory(normalizedPath, LinkOption.NOFOLLOW_LINKS)) {
            throw new BadRequestException("Invalid directory path.");
        }

        try (var pathStream = Files.walk(normalizedPath)) {
            pathStream
                    .filter(filePath -> Files.isRegularFile(filePath, LinkOption.NOFOLLOW_LINKS))
                    .forEach(filePath -> uploadFilePath(bucketName, normalizedPath, filePath));
        }
    }

    @Override
    public void delete(@NonNull FileMetadataDto fileMetadataDto) {
        final String bucketName = fileMetadataDto.bucketName();
        final String fileName = fileMetadataDto.fileName();
        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .build();
            s3Client.deleteObject(deleteObjectRequest);
        } catch (S3Exception ex) {
            log.error(
                    "aws.s3.delete.error: bucket={}, key={}, exceptionClass={}",
                    bucketName,
                    fileName,
                    ex.getClass().getSimpleName(),
                    ex);
            throw ex;
        } catch (SdkClientException ex) {
            log.error(
                    "aws.s3.delete.unreachable: bucket={}, key={}, exceptionClass={}",
                    bucketName,
                    fileName,
                    ex.getClass().getSimpleName(),
                    ex);
            throw ex;
        }
    }

    @Override
    public @NonNull Optional<String> getUrl(@NonNull FileMetadataDto fileMetadataDto) {
        if (StringUtils.hasText(awsProperties.publicUrlBase()) && publicUrlBaseMatchesBucket(fileMetadataDto)) {
            return Optional.of(publicUrl(fileMetadataDto));
        }
        return presignedUrl(fileMetadataDto);
    }

    private @NonNull Optional<String> presignedUrl(@NonNull FileMetadataDto fileMetadataDto) {
        try {
            String url = s3Presigner
                    .presignGetObject(r -> r.signatureDuration(awsProperties.linkExpirationTime())
                            .getObjectRequest(
                                    g -> g.bucket(fileMetadataDto.bucketName()).key(fileMetadataDto.fileName())))
                    .url()
                    .toString();
            return Optional.of(url);
        } catch (SdkClientException ex) {
            log.error(
                    "aws.s3.presign.error: bucket={}, key={}, cause={}",
                    fileMetadataDto.bucketName(),
                    fileMetadataDto.fileName(),
                    ex.getMessage(),
                    ex);
            return Optional.empty();
        }
    }

    private boolean publicUrlBaseMatchesBucket(FileMetadataDto fileMetadataDto) {
        String baseUrl = normalizedPublicUrlBase();
        String bucketName = fileMetadataDto.bucketName();
        if (baseUrl.endsWith("/" + bucketName)) {
            return true;
        }

        int publicSegment = baseUrl.indexOf("/storage/v1/object/public/");
        if (publicSegment < 0) {
            return true;
        }

        String suffix = baseUrl.substring(publicSegment + "/storage/v1/object/public/".length());
        return !StringUtils.hasText(suffix);
    }

    @Override
    public @NonNull List<String> listObjectKeys(@NonNull String bucketName) {
        try {
            ListObjectsV2Request request =
                    ListObjectsV2Request.builder().bucket(bucketName).build();
            return s3Client.listObjectsV2Paginator(request).contents().stream()
                    .map(software.amazon.awssdk.services.s3.model.S3Object::key)
                    .toList();
        } catch (S3Exception ex) {
            log.error(
                    "aws.s3.list.error: bucket={}, exceptionClass={}",
                    bucketName,
                    ex.getClass().getSimpleName(),
                    ex);
            throw new FileListException(bucketName, ex);
        } catch (SdkClientException ex) {
            log.error(
                    "aws.s3.list.unreachable: bucket={}, exceptionClass={}",
                    bucketName,
                    ex.getClass().getSimpleName(),
                    ex);
            throw new FileListException(bucketName, ex);
        }
    }

    private String publicUrl(FileMetadataDto fileMetadataDto) {
        String baseUrl = normalizedPublicUrlBase();
        String bucketName = fileMetadataDto.bucketName();
        String encodedFileName = UriUtils.encodePath(fileMetadataDto.fileName(), StandardCharsets.UTF_8);
        if (baseUrl.endsWith("/" + bucketName)) {
            return baseUrl + "/" + encodedFileName;
        }
        return baseUrl + "/" + bucketName + "/" + encodedFileName;
    }

    private String normalizedPublicUrlBase() {
        String baseUrl = awsProperties.publicUrlBase().strip();
        while (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl;
    }

    private void uploadFilePath(String bucketName, Path normalizedPath, Path filePath) {
        String key = normalizedPath.relativize(filePath).toString().replace("\\", "/");
        try {
            PutObjectRequest putObjectRequest =
                    PutObjectRequest.builder().bucket(bucketName).key(key).build();
            s3Client.putObject(putObjectRequest, RequestBody.fromFile(filePath));
        } catch (S3Exception ex) {
            log.error(
                    "aws.s3.upload.file_error: bucket={}, key={}, exceptionClass={}",
                    bucketName,
                    key,
                    ex.getClass().getSimpleName(),
                    ex);
            throw new FileUploadException(key, ex);
        } catch (SdkClientException ex) {
            log.error(
                    "aws.s3.upload.file_unreachable: bucket={}, key={}, exceptionClass={}",
                    bucketName,
                    key,
                    ex.getClass().getSimpleName(),
                    ex);
            throw new FileUploadException(key, ex);
        }
    }
}
