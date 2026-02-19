package com.zufar.icedlatte.security.jwt;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@ConditionalOnMissingBean(RedisJwtBlacklistService.class)
public class InMemoryJwtBlacklistService {

    private final ConcurrentMap<String, TokenEntry> blacklistedTokens = new ConcurrentHashMap<>();
    private final AtomicInteger tokenCount = new AtomicInteger(0);

    @Value("${jwt.expiration}")
    private long jwtExpirationMs;

    private static final int MAX_TOKENS = 10000;
    private static final long CLEANUP_INTERVAL_MS = 300000; // 5 minutes

    @PostConstruct
    public void initialize() {
        log.info("In-memory JWT blacklist service initialized with TTL: {} ms", jwtExpirationMs);
    }

    public void blacklistToken(String token) {
        if (!StringUtils.hasText(token)) {
            log.warn("Attempted to blacklist empty token");
            return;
        }

        if (tokenCount.get() >= MAX_TOKENS) {
            log.warn("Maximum blacklist capacity reached, performing cleanup");
            cleanupExpiredTokens();
        }

        String tokenKey = sha256(token);
        Instant expiryTime = Instant.now().plusMillis(jwtExpirationMs);

        TokenEntry entry = new TokenEntry(expiryTime);
        TokenEntry previous = blacklistedTokens.put(tokenKey, entry);

        if (previous == null) {
            tokenCount.incrementAndGet();
        }

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
            tokenCount.decrementAndGet();
            log.debug("Expired blacklisted token removed during lookup");
            return false;
        }

        return true;
    }

    @Scheduled(fixedRate = CLEANUP_INTERVAL_MS)
    public void cleanupExpiredTokens() {
        Instant now = Instant.now();

        blacklistedTokens.entrySet().removeIf(entry -> {
            if (entry.getValue().isExpired(now)) {
                tokenCount.decrementAndGet();
                return true;
            } else {
                return false;
            }
        });
    }

    @PreDestroy
    public void shutdown() {
        int finalCount = tokenCount.get();
        blacklistedTokens.clear();
        tokenCount.set(0);
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