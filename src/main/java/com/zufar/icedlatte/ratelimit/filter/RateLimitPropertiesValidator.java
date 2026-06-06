package com.zufar.icedlatte.ratelimit.filter;

import com.zufar.icedlatte.ratelimit.configuration.RateLimitProperties;
import com.zufar.icedlatte.ratelimit.configuration.RateLimitProperties.Bucket;

import lombok.experimental.UtilityClass;

@UtilityClass
class RateLimitPropertiesValidator {

    static void validate(RateLimitProperties properties) {
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

    static void assertPositiveBanConfiguration(RateLimitProperties properties) {
        if (properties.getBanThreshold() <= 0) {
            throw new IllegalStateException(
                    "security.rate-limit.ban-threshold must be > 0, got: " + properties.getBanThreshold());
        }
        if (properties.getBanDuration() == null
                || properties.getBanDuration().isZero()
                || properties.getBanDuration().isNegative()) {
            throw new IllegalStateException(
                    "security.rate-limit.ban-duration must be positive, got: " + properties.getBanDuration());
        }
    }

    private static void assertPositive(String bucketName, Bucket bucket) {
        if (bucket.getMaxRequests() <= 0) {
            throw new IllegalStateException(
                    "security.rate-limit." + bucketName + ".max-requests must be > 0, got: " + bucket.getMaxRequests());
        }
        if (bucket.getWindowDuration() == null
                || bucket.getWindowDuration().isZero()
                || bucket.getWindowDuration().isNegative()) {
            throw new IllegalStateException("security.rate-limit." + bucketName
                    + ".window-duration must be positive, got: " + bucket.getWindowDuration());
        }
    }
}
