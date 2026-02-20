package com.zufar.icedlatte.security.jwt;

import com.zufar.icedlatte.security.configuration.JwtProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.annotation.PreDestroy;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@Service
@ConditionalOnMissingBean(RedisJwtBlacklistService.class)
public class InMemoryJwtBlacklistService implements JwtBlacklistService {

    private final ConcurrentMap<String, TokenEntry> blacklistedTokens = new ConcurrentHashMap<>();
    private final JwtProperties jwtProperties;

    public InMemoryJwtBlacklistService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    private static final int MAX_TOKENS = 10000;
    private static final long CLEANUP_INTERVAL_MS = 300000;

    public synchronized void blacklistToken(String token) {
        if (!StringUtils.hasText(token)) {
            log.warn("Attempted to blacklist empty token");
            return;
        }

        if (blacklistedTokens.size() >= MAX_TOKENS) {
            log.warn("Maximum blacklist capacity reached, performing cleanup");
            cleanupExpiredTokens();
        }

        Instant expiryTime = Instant.now().plus(jwtProperties.expiration());
        blacklistedTokens.put(sha256(token), new TokenEntry(expiryTime));
        log.debug("Token blacklisted in memory, expires at: {}", expiryTime);
    }

    public boolean isBlacklisted(String token) {
        if (!StringUtils.hasText(token)) {
            log.warn("Token validation attempted with empty token");
            return true;
        }

        String tokenKey = sha256(token);
        TokenEntry entry = blacklistedTokens.get(tokenKey);

        if (entry == null) {
            return false;
        }

        if (entry.isExpired()) {
            blacklistedTokens.remove(tokenKey);
            log.debug("Expired blacklisted token removed during lookup");
            return false;
        }

        return true;
    }

    @Scheduled(fixedRate = CLEANUP_INTERVAL_MS)
    public void cleanupExpiredTokens() {
        Instant now = Instant.now();
        blacklistedTokens.entrySet().removeIf(e -> e.getValue().isExpired(now));
    }

    @PreDestroy
    public void shutdown() {
        int finalCount = blacklistedTokens.size();
        blacklistedTokens.clear();
        log.info("In-memory JWT blacklist service shutdown, cleared {} tokens", finalCount);
    }

    private static String sha256(String token) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private record TokenEntry(Instant expiryTime) {

        boolean isExpired() {
            return isExpired(Instant.now());
        }

        boolean isExpired(Instant now) {
            return now.isAfter(expiryTime);
        }
    }
}