package com.zufar.icedlatte.security.ratelimit;

public record RateLimitResult(boolean allowed, int limit, int remaining, long resetTimeMillis) {}
