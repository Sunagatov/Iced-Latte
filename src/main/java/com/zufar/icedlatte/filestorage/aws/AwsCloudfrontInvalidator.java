package com.zufar.icedlatte.filestorage.aws;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.cloudfront.CloudFrontClient;
import software.amazon.awssdk.services.cloudfront.model.CreateInvalidationRequest;
import software.amazon.awssdk.services.cloudfront.model.InvalidationBatch;
import software.amazon.awssdk.services.cloudfront.model.Paths;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnBean(CloudFrontClient.class)
public class AwsCloudfrontInvalidator {

    @Value("${spring.aws.cloudfront-distribution-id:}")
    private String distributionId;

    private final CloudFrontClient cloudFrontClient;

    public void invalidate(String fileKey) {
        if (!org.springframework.util.StringUtils.hasText(distributionId)) {
            log.warn("cloudfront.invalidation.skipped: reason=distribution_id_not_configured");
            return;
        }
        cloudFrontClient.createInvalidation(CreateInvalidationRequest.builder()
                .distributionId(distributionId)
                .invalidationBatch(InvalidationBatch.builder()
                        .callerReference(UUID.randomUUID().toString())
                        .paths(Paths.builder().quantity(1).items("/" + fileKey).build())
                        .build())
                .build());
        log.info("cloudfront.invalidation.created: key={}", fileKey);
    }
}
