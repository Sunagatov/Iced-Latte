package com.zufar.icedlatte.ratelimit.filter;

import com.zufar.icedlatte.ratelimit.configuration.RateLimitProperties;
import com.zufar.icedlatte.ratelimit.configuration.RateLimitProperties.Bucket;

import lombok.experimental.UtilityClass;

@UtilityClass
class RateLimitPropertiesValidator {

    static void validate(RateLimitProperties properties) {
        if (properties == null) {
            throw new IllegalStateException("security.rate-limit configuration must be present");
        }
        assertPositive("pre-auth", properties.getPreAuth());
        assertPositive("auth", properties.getAuth());
        assertPositive("global", properties.getGlobal());
        assertPositive("search", properties.getSearch());
        assertPositive("telemetry", properties.getTelemetry());
        assertPositive("payment", properties.getPayment());
        assertPositive("write", properties.getWrite());
        assertPositive("file-upload", properties.getFileUpload());
        assertPositiveBanConfiguration(properties);
    }

    private static void assertPositiveBanConfiguration(RateLimitProperties properties) {
        if (properties.getBanThreshold() <= 0) {
            throw new IllegalStateException(
                    "security.rate-limit.ban-threshold must be > 0, got: " + properties.getBanThreshold());
        }
        if (properties.getBanDuration() == null
                || properties.getBanDuration().isZero()
                || properties.getBanDuration().isNegative()) {
            String errorMessage = "security.rate-limit.ban-duration must be positive, got: " + properties.getBanDuration();
            throw new IllegalStateException(errorMessage);
        }
        if (properties.getBanDuration().toMillis() < 1) {
            String errorMessage =
                    "security.rate-limit.ban-duration must be at least 1ms, got: " + properties.getBanDuration();
            throw new IllegalStateException(errorMessage);
        }
    }

    private static void assertPositive(String bucketName, Bucket bucket) {
        if (bucket == null) {
            String errorMessage = "security.rate-limit." + bucketName + " must be configured";
            throw new IllegalStateException(errorMessage);
        }
        if (bucket.getMaxRequests() <= 0) {
            String errorMessage = "security.rate-limit." + bucketName + ".max-requests must be > 0, got: " + bucket.getMaxRequests();
            throw new IllegalStateException(errorMessage);
        }
        if (bucket.getWindowDuration() == null
                || bucket.getWindowDuration().isZero()
                || bucket.getWindowDuration().isNegative()) {
            String errorMessage = "security.rate-limit." + bucketName
                    + ".window-duration must be positive, got: " + bucket.getWindowDuration();
            throw new IllegalStateException(errorMessage);
        }
        if (bucket.getWindowDuration().toMillis() < 1) {
            String errorMessage = "security.rate-limit." + bucketName
                    + ".window-duration must be at least 1ms, got: " + bucket.getWindowDuration();
            throw new IllegalStateException(errorMessage);
        }
    }
}
