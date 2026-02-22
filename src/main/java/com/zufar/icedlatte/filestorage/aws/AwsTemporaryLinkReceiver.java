package com.zufar.icedlatte.filestorage.aws;

import com.zufar.icedlatte.filestorage.dto.FileMetadataDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnBean(S3Client.class)
public class AwsTemporaryLinkReceiver {

    @Value("${spring.aws.link-expiration-time}")
    private String linkExpirationTime;

    /**
     * Optional: when set, files are served via a direct public URL
     * (e.g. {@code https://<bucket>.<region>.render.com}).
     */
    @Value("${spring.aws.public-url-base:}")
    private String publicUrlBase;

    private final S3Presigner s3Presigner;

    public String generatePresignedUrlAsString(FileMetadataDto fileMetadata) {
        if (StringUtils.hasText(publicUrlBase)) {
            return publicUrlBase.stripTrailing() + "/" + fileMetadata.fileName();
        }
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(fileMetadata.bucketName())
                    .key(fileMetadata.fileName())
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.parse(linkExpirationTime))
                    .getObjectRequest(getObjectRequest)
                    .build();

            return s3Presigner.presignGetObject(presignRequest).url().toString();
        } catch (SdkClientException e) {
            log.error("Error generating presigned URL", e);
            return null;
        }
    }
}
