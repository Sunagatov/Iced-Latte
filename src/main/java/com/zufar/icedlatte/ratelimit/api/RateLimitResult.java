package com.zufar.icedlatte.ratelimit.api;

public record RateLimitResult(boolean allowed, int limit, int remaining, long resetTimeMillis, long windowSeconds) {

    public RateLimitResult {
        limit = Math.max(0, limit);
        remaining = Math.max(0, remaining);
        windowSeconds = Math.max(1, windowSeconds);
    }

    public RateLimitResult(boolean allowed, int limit, int remaining, long resetTimeMillis) {
        this(
                allowed,
                limit,
                remaining,
                resetTimeMillis,
                Math.max(1, (resetTimeMillis - System.currentTimeMillis()) / 1000));
    }
}
