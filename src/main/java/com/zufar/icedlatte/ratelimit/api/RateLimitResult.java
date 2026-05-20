package com.zufar.icedlatte.ratelimit.api;

public record RateLimitResult(boolean allowed, int limit, int remaining, long resetTimeMillis) {}
