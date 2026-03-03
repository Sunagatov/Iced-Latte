package com.zufar.icedlatte.security.jwt;

import com.zufar.icedlatte.security.configuration.JwtProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.annotation.PreDestroy;

import java.time.Instant;
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

    public synchronized void blacklistToken(String token) {
        if (!StringUtils.hasText(token)) {
            log.warn("jwt.blacklist.empty_token");
            return;
        }

        if (blacklistedTokens.size() >= MAX_TOKENS) {
            log.warn("jwt.blacklist.capacity_cleanup");
            cleanupExpiredTokens();
        }

        Instant expiryTime = Instant.now().plus(jwtProperties.expiration());
        blacklistedTokens.put(sha256(token), new TokenEntry(expiryTime));
        log.debug("jwt.blacklist.added: expiresAt={}", expiryTime);
    }

    public boolean isBlacklisted(String token) {
        if (!StringUtils.hasText(token)) {
            log.warn("jwt.blacklist.validate.empty_token");
            return true;
        }

        String tokenKey = sha256(token);
        TokenEntry entry = blacklistedTokens.get(tokenKey);

        if (entry == null) {
            return false;
        }

        if (entry.isExpired()) {
            blacklistedTokens.remove(tokenKey);
            log.debug("jwt.blacklist.expired_removed");
            return false;
        }

        return true;
    }

    @Scheduled(fixedRateString = "${security.jwt.blacklist.cleanup-interval-ms:300000}")
    public void cleanupExpiredTokens() {
        Instant now = Instant.now();
        blacklistedTokens.entrySet().removeIf(e -> e.getValue().isExpired(now));
    }

    @PreDestroy
    public void shutdown() {
        int finalCount = blacklistedTokens.size();
        blacklistedTokens.clear();
        log.info("jwt.blacklist.shutdown: cleared={}", finalCount);
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