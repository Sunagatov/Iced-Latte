package com.zufar.icedlatte.security.jwt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.data.redis.host")
public class RedisJwtBlacklistService {

    private static final String BLACKLIST_KEY_PREFIX = "jwt:blacklist:";
    private static final String BLACKLIST_VALUE = "revoked";


    private final RedisTemplate<String, String> redisTemplate;

    @Value("${jwt.expiration}")
    private Duration jwtTtl;

    @Retryable(retryFor = DataAccessException.class, backoff = @Backoff(delay = 100))
    public void blacklistToken(String token) {
        if (!StringUtils.hasText(token)) {
            log.warn("Attempted to blacklist empty token");
            return;
        }
        String key = buildBlacklistKey(token);
        redisTemplate.opsForValue().set(key, BLACKLIST_VALUE, jwtTtl);
        log.debug("Token blacklisted successfully with TTL: {} seconds", jwtTtl.toSeconds());
    }

    public boolean isBlacklisted(String token) {
        if (!StringUtils.hasText(token)) {
            log.warn("Token validation attempted with empty token");
            return true;
        }
        String key = buildBlacklistKey(token);
        try {
            Boolean hasKey = redisTemplate.hasKey(key);
            //noinspection ConstantConditions - hasKey is @Nullable per Spring's RedisOperations contract
            boolean isBlacklisted = hasKey != null && hasKey;
            log.debug("Token blacklist check: {} - {}", key.substring(0, Math.min(key.length(), 50)),
                    isBlacklisted ? "BLACKLISTED" : "VALID");
            return isBlacklisted;
        } catch (RuntimeException ex) {
            log.error("Redis isBlacklisted failed - failing secure", ex);
            return true;
        }
    }

    private String buildBlacklistKey(String token) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return BLACKLIST_KEY_PREFIX + HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
