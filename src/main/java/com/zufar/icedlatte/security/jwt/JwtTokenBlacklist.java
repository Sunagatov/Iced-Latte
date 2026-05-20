package com.zufar.icedlatte.security.jwt;

import com.zufar.icedlatte.common.temporarycache.ExpiringKeyValueStore;
import com.zufar.icedlatte.security.configuration.JwtProperties;
import com.zufar.icedlatte.security.exception.JwtTokenBlacklistedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtTokenBlacklist {

    private static final String KEY_PREFIX = "jwt:blacklist:";

    private final ExpiringKeyValueStore temporaryStore;
    private final JwtProperties jwtProperties;

    @Retryable(retryFor = DataAccessException.class, backoff = @Backoff(delay = 100))
    public void blacklist(String token) {
        if (!StringUtils.hasText(token)) {
            log.debug("jwt.blacklist.empty_token");
            return;
        }
        temporaryStore.put(namespacedKey(token), "true", jwtProperties.expiration());
        log.debug("jwt.blacklist.added: ttlSeconds={}", jwtProperties.expiration().toSeconds());
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
            log.error("jwt.blacklist.store_error: exceptionClass={}",
                    ex.getClass().getSimpleName(), ex);
            return true;
        }
    }

    private String namespacedKey(String token) {
        return KEY_PREFIX + hash(token);
    }

    public String hash(String token) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
