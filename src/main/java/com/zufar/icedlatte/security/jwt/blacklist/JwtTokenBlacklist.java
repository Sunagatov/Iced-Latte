package com.zufar.icedlatte.security.jwt.blacklist;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.function.Supplier;

import org.springframework.dao.DataAccessException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.zufar.icedlatte.security.jwt.config.JwtProperties;
import com.zufar.icedlatte.security.jwt.exception.JwtTokenBlacklistedException;
import com.zufar.icedlatte.security.service.cache.ExpiringKeyValueStore;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtTokenBlacklist {

    private static final String KEY_PREFIX = "jwt:blacklist:";

    private final ExpiringKeyValueStore temporaryStore;
    private final JwtProperties jwtProperties;

    @Retryable(retryFor = DataAccessException.class, backoff = @Backoff(delay = 100))
    public void blacklist(String token) {
        blacklist(token, jwtProperties::expiration);
    }

    @Retryable(retryFor = DataAccessException.class, backoff = @Backoff(delay = 100))
    public void blacklistRefreshToken(String token) {
        blacklist(token, jwtProperties::refreshExpiration);
    }

    private void blacklist(String token, Supplier<Duration> ttl) {
        if (!StringUtils.hasText(token)) {
            log.debug("jwt.blacklist.empty_token");
            return;
        }
        Duration tokenTtl = ttl.get();
        temporaryStore.put(namespacedKey(token), "true", tokenTtl);
        log.debug("jwt.blacklist.added: ttlSeconds={}", tokenTtl.toSeconds());
    }

    public void validateNotBlacklisted(String token) {
        if (!StringUtils.hasText(token)) {
            throw new JwtTokenBlacklistedException("Invalid token format");
        }
        if (isBlacklisted(token)) {
            log.debug("jwt.blacklist.token_revoked");
            throw new JwtTokenBlacklistedException("Token has been revoked");
        }
    }

    public boolean isBlacklisted(String token) {
        if (!StringUtils.hasText(token)) {
            log.debug("jwt.blacklist.validate.empty_token");
            return true;
        }
        try {
            return temporaryStore.contains(namespacedKey(token));
        } catch (RuntimeException ex) {
            String logMessage = "jwt.blacklist.store_error: exceptionClass={}";
            log.error(logMessage, ex.getClass().getSimpleName(), ex);
            return true;
        }
    }

    private String namespacedKey(String token) {
        return KEY_PREFIX + hash(token);
    }

    public String hash(String token) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
