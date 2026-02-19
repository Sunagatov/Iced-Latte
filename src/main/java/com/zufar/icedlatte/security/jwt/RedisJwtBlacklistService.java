package com.zufar.icedlatte.security.jwt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
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
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final Duration RETRY_DELAY = Duration.ofMillis(100);

    private final RedisTemplate<String, String> redisTemplate;

    @Value("${jwt.expiration}")
    private Duration jwtTtl;

    public void blacklistToken(String token) {
        if (!StringUtils.hasText(token)) {
            log.warn("Attempted to blacklist empty token");
            return;
        }

        String key = buildBlacklistKey(token);
        executeWithRetry(() -> {
            redisTemplate.opsForValue().set(key, BLACKLIST_VALUE, jwtTtl);
            log.debug("Token blacklisted successfully with TTL: {} seconds", jwtTtl.getSeconds());
            return null;
        }, "blacklist token");
    }

    public boolean isBlacklisted(String token) {
        if (!StringUtils.hasText(token)) {
            log.warn("Token validation attempted with empty token");
            return true;
        }

        String key = buildBlacklistKey(token);
        
        return executeWithRetry(() -> {
            boolean isBlacklisted = redisTemplate.hasKey(key);
            log.debug("Token blacklist check: {} - {}", key.substring(0, Math.min(key.length(), 50)), 
                     isBlacklisted ? "BLACKLISTED" : "VALID");
            return isBlacklisted;
        }, "check token blacklist status");
    }

    private String buildBlacklistKey(String token) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return BLACKLIST_KEY_PREFIX + HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private <T> T executeWithRetry(RedisOperation<T> operation, String operationName) {
        Exception lastException = null;
        
        for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
            try {
                return operation.execute();
            } catch (Exception ex) {
                lastException = ex;
                log.warn("Redis operation '{}' failed on attempt {}/{}: {}", 
                        operationName, attempt, MAX_RETRY_ATTEMPTS, ex.getMessage());
                
                if (attempt < MAX_RETRY_ATTEMPTS) {
                    try {
                        Thread.sleep(RETRY_DELAY.toMillis() * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        
        log.error("Redis operation '{}' failed after {} attempts - failing secure", 
                 operationName, MAX_RETRY_ATTEMPTS, lastException);
        
        // For blacklist checks, fail secure by returning true (treat as blacklisted)
        // For blacklist operations, throw exception to indicate failure
        if (operationName.contains("check")) {
            @SuppressWarnings("unchecked")
            T result = (T) Boolean.TRUE;
            return result;
        } else {
            throw new RuntimeException("Redis operation failed: " + operationName, lastException);
        }
    }

    @FunctionalInterface
    private interface RedisOperation<T> {
        T execute() throws Exception;
    }
}
