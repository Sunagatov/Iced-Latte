package com.zufar.icedlatte.filestorage.aws;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.cloudfront.CloudFrontClient;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnBean(CloudFrontClient.class)
public class AwsCloudFrontInvalidator {

    @Value("${spring.aws.cloudfront-distribution-id:}")
    private String distributionId;

    private final CloudFrontClient cloudFrontClient;

    public void invalidate(String fileKey) {
        if (!org.springframework.util.StringUtils.hasText(distributionId)) {
            log.warn("cloudfront.invalidation.skipped: reason=distribution_id_not_configured");
            return;
        }
        cloudFrontClient.createInvalidation(r -> r
                .distributionId(distributionId)
                .invalidationBatch(b -> b
                        .callerReference(UUID.randomUUID().toString())
                        .paths(p -> p.quantity(1).items("/" + fileKey))));
        log.info("cloudfront.invalidation.created: key={}", fileKey);
    }
}
