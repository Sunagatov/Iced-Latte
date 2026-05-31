package com.zufar.icedlatte.filestorage.aws;

import java.util.UUID;

import org.jspecify.annotations.NonNull;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.zufar.icedlatte.filestorage.api.FileCacheInvalidationApi;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.cloudfront.CloudFrontClient;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnBean(CloudFrontClient.class)
public class AwsCloudFrontInvalidator implements FileCacheInvalidationApi {

    private final CloudFrontClient cloudFrontClient;
    private final AwsProperties awsProperties;

    @Override
    public void invalidate(@NonNull String fileKey) {
        String distributionId = awsProperties.cloudfrontDistributionId();
        if (!StringUtils.hasText(distributionId)) {
            log.warn("cloudfront.invalidation.skipped: reason=distribution_id_not_configured");
            return;
        }
        cloudFrontClient.createInvalidation(r -> r.distributionId(distributionId)
                .invalidationBatch(b -> b.callerReference(UUID.randomUUID().toString())
                        .paths(p -> p.quantity(1).items("/" + fileKey))));
        log.info("cloudfront.invalidation.created: key={}", fileKey);
    }
}
