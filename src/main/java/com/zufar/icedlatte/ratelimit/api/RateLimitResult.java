package com.zufar.icedlatte.ratelimit.api;

public record RateLimitResult(boolean allowed, int limit, int remaining, long resetTimeMillis, long windowSeconds) {

    public RateLimitResult(boolean allowed, int limit, int remaining, long resetTimeMillis) {
        this(
                allowed,
                limit,
                remaining,
                resetTimeMillis,
                Math.max(1, (resetTimeMillis - System.currentTimeMillis()) / 1000));
    }
}
