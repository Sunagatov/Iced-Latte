package com.zufar.icedlatte.ratelimit.filter;

import com.zufar.icedlatte.ratelimit.configuration.RateLimitProperties;
import com.zufar.icedlatte.ratelimit.configuration.RateLimitProperties.Bucket;
import com.zufar.icedlatte.ratelimit.dto.RateLimitCategory;

import lombok.experimental.UtilityClass;

@UtilityClass
class RateLimitBucketResolver {

    static Bucket bucketFor(RateLimitCategory category, RateLimitProperties properties) {
        return switch (category) {
            case AUTH, AUTH_PRE -> properties.getAuth();
            case SEARCH -> properties.getSearch();
            case TELEMETRY -> properties.getTelemetry();
            case PAYMENT -> properties.getPayment();
            case WRITE -> properties.getWrite();
            case FILE_UPLOAD -> properties.getFileUpload();
            case PRE_AUTH -> properties.getPreAuth();
            case GLOBAL -> properties.getGlobal();
        };
    }
}
