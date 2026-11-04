package com.zufar.icedlatte.ratelimit.filter;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

import com.zufar.icedlatte.common.config.CaffeineSizeProperties;
import com.zufar.icedlatte.ratelimit.configuration.RateLimitProperties;

class RateLimitBanTrackerTest {
    private static final CaffeineSizeProperties CACHE_SIZES =
            new CaffeineSizeProperties(1000, 5000, 10000, 1000, 10000);

    @Test
    void banLastsForTheFullDurationAfterReachingTheThreshold() {
        RateLimitProperties properties = new RateLimitProperties();
        properties.setBanThreshold(2);
        properties.setBanDuration(Duration.ofMinutes(5));
        AtomicLong nanos = new AtomicLong();
        var tracker = new RateLimitBanTracker(
                properties, new CaffeineSizeProperties(1000, 5000, 10000, 1000, 10000), nanos::get);
        tracker.recordBlock("test-client");
        nanos.addAndGet(Duration.ofMinutes(4).toNanos());
        tracker.recordBlock("test-client");
        assertThat(tracker.isBanned("test-client")).isTrue();

        nanos.addAndGet(Duration.ofMinutes(1).plusNanos(1).toNanos());
        assertThat(tracker.isBanned("test-client")).isTrue();

        nanos.addAndGet(Duration.ofMinutes(4).toNanos());
        assertThat(tracker.isBanned("test-client")).isFalse();
    }

    @Test
    void furtherBlocksDoNotRestartAnExistingBan() {
        RateLimitProperties properties = new RateLimitProperties();
        properties.setBanThreshold(1);
        AtomicLong nanos = new AtomicLong();
        var tracker = new RateLimitBanTracker(properties, CACHE_SIZES, nanos::get);
        tracker.recordBlock("client");
        nanos.addAndGet(Duration.ofMinutes(2).toNanos());
        tracker.recordBlock("client");

        assertThat(tracker.remainingBanDuration("client")).isEqualTo(Duration.ofMinutes(3));
    }

    @Test
    void oldBlocksOutsideTheObservationWindowDoNotCountTowardANewBan() {
        RateLimitProperties properties = new RateLimitProperties();
        properties.setBanThreshold(2);
        AtomicLong nanos = new AtomicLong();
        var tracker = new RateLimitBanTracker(properties, CACHE_SIZES, nanos::get);
        tracker.recordBlock("client");
        nanos.addAndGet(Duration.ofMinutes(6).toNanos());
        tracker.recordBlock("client");

        assertThat(tracker.isBanned("client")).isFalse();
    }
}
