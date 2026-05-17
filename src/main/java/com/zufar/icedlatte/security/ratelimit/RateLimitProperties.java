package com.zufar.icedlatte.security.ratelimit;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Getter
@Setter
@ConfigurationProperties(prefix = "security.rate-limit")
public class RateLimitProperties {

    private Bucket global = new Bucket(60, Duration.ofMinutes(1));
    private Bucket auth = new Bucket(10, Duration.ofMinutes(1));
    private Bucket search = new Bucket(30, Duration.ofMinutes(1));
    private Bucket telemetry = new Bucket(120, Duration.ofMinutes(1));
    private Bucket payment = new Bucket(20, Duration.ofMinutes(1));
    private Bucket write = new Bucket(20, Duration.ofMinutes(1));
    private Bucket fileUpload = new Bucket(5, Duration.ofMinutes(1));
    private Bucket preAuth = new Bucket(200, Duration.ofMinutes(1));

    /** Number of 429 responses within a window before an IP is temporarily banned. */
    private int banThreshold = 10;
    /** Duration of the temporary ban for repeat offenders. */
    private Duration banDuration = Duration.ofMinutes(5);

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Bucket {
        private int maxRequests;
        private Duration windowDuration;
    }
}
