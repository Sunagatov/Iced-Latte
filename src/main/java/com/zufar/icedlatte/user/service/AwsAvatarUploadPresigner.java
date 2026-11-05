package com.zufar.icedlatte.user.service;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.zufar.icedlatte.openapi.dto.AvatarUploadTargetResponse;
import com.zufar.icedlatte.user.config.AvatarUploadProperties;
import com.zufar.icedlatte.user.entity.UserAvatarUpload;
import com.zufar.icedlatte.user.exception.UserAvatarUploadException;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Slf4j
@Service
@ConditionalOnProperty(name = "spring.aws.enabled", havingValue = "true")
@ConditionalOnProperty(name = "avatar.upload-mode", havingValue = "presigned")
public class AwsAvatarUploadPresigner implements AvatarUploadPresigner {

    private static final String AVATAR_UPLOAD_VERSION = "1";

    private final S3Presigner s3Presigner;
    private final AvatarUploadProperties properties;
    private final Clock clock;

    @Autowired
    public AwsAvatarUploadPresigner(S3Presigner s3Presigner, AvatarUploadProperties properties) {
        this(s3Presigner, properties, Clock.systemUTC());
    }

    AwsAvatarUploadPresigner(S3Presigner s3Presigner, AvatarUploadProperties properties, Clock clock) {
        this.s3Presigner = s3Presigner;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public AvatarUploadTargetResponse presign(UserAvatarUpload upload) {
        try {
            Duration remaining = Duration.between(clock.instant(), upload.getExpiresAt());
            if (remaining.compareTo(Duration.ofSeconds(1)) < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Upload link expired. Create a new upload.");
            }
            PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                    .signatureDuration(
                            remaining.compareTo(properties.presignedUrlTtl()) < 0
                                    ? remaining
                                    : properties.presignedUrlTtl())
                    .putObjectRequest(putObjectRequest(upload))
                    .build();
            var presigned = s3Presigner.presignPutObject(presignRequest);
            return new AvatarUploadTargetResponse()
                    .method(AvatarUploadTargetResponse.MethodEnum.PUT)
                    .url(URI.create(presigned.url().toString()))
                    .headers(requiredHeaders(presigned.signedHeaders()));
        } catch (SdkClientException ex) {
            log.warn(
                    "avatar.upload_intent.presign_failed: userId={}, uploadId={}, exceptionClass={}, message={}",
                    upload.getUserId(),
                    upload.getId(),
                    ex.getClass().getSimpleName(),
                    ex.getMessage());
            throw new UserAvatarUploadException(upload.getUserId(), upload.getOriginalKey());
        }
    }

    private PutObjectRequest putObjectRequest(UserAvatarUpload upload) {
        return PutObjectRequest.builder()
                .bucket(upload.getOriginalBucket())
                .key(upload.getOriginalKey())
                .contentType(upload.getContentType())
                .contentLength(upload.getOriginalSizeBytes())
                .metadata(metadata(upload))
                .build();
    }

    private Map<String, String> metadata(UserAvatarUpload upload) {
        return Map.of(
                "upload-id", upload.getId().toString(),
                "user-id", upload.getUserId().toString(),
                "requested-content-type", upload.getContentType(),
                "avatar-upload-version", AVATAR_UPLOAD_VERSION);
    }

    private Map<String, String> requiredHeaders(Map<String, ? extends List<String>> signedHeaders) {
        return signedHeaders.entrySet().stream()
                .filter(entry -> !"host".equalsIgnoreCase(entry.getKey()))
                // Browsers set Content-Length from the Blob and forbid scripts from setting it.
                .filter(entry -> !"content-length".equalsIgnoreCase(entry.getKey()))
                .filter(entry -> !entry.getValue().isEmpty())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> String.join(",", entry.getValue()),
                        (_, right) -> right,
                        LinkedHashMap::new));
    }
}
