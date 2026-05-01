package com.zufar.icedlatte.security.ratelimit;

import java.time.Duration;

public interface RateLimiter {

    RateLimitResult tryConsume(String key,
                               int maxTokens,
                               Duration windowDuration);
}
