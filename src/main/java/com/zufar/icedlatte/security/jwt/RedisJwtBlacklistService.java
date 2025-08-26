package com.zufar.icedlatte.security.jwt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.data.redis.host")
public class RedisJwtBlacklistService {

    private static final String BLACKLIST_KEY_PREFIX = "jwt:blacklist:";

    private final RedisTemplate<String, String> redisTemplate;

    @Value("${jwt.expiration}")
    private Duration jwtTtl;

    public void blacklistToken(String token) {
        try {
            String key = BLACKLIST_KEY_PREFIX + token;
            redisTemplate.opsForValue().set(key, "blacklisted", jwtTtl);
            log.debug("Token blacklisted with TTL: {} seconds", jwtTtl.getSeconds());
        } catch (Exception e) {
            log.error("Failed to blacklist token", e);
        }
    }

    public boolean isBlacklisted(String token) {
        try {
            String key = BLACKLIST_KEY_PREFIX + token;
            return redisTemplate.hasKey(key);
        } catch (Exception e) {
            log.error("Redis unavailable - failing secure by rejecting token", e);
            return true;
        }
    }
}
