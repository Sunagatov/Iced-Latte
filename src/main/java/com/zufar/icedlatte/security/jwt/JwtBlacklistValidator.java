package com.zufar.icedlatte.security.jwt;

import com.zufar.icedlatte.security.exception.JwtTokenBlacklistedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class JwtBlacklistValidator {

    private final RedisJwtBlacklistService redisService;
    private final InMemoryJwtBlacklistService inMemoryService;

    public JwtBlacklistValidator(@Autowired(required = false) RedisJwtBlacklistService redisService,
                                 @Autowired(required = false) InMemoryJwtBlacklistService inMemoryService) {
        this.redisService = redisService;
        this.inMemoryService = inMemoryService;

        if (this.redisService != null) {
            log.info("Using Redis JWT blacklist service");
        } else if (this.inMemoryService != null) {
            log.info("Using in-memory JWT blacklist service");
        } else {
            log.warn("No JWT blacklist service available - tokens cannot be blacklisted");
        }
    }

    public void addToBlacklist(String token) {
        if (!StringUtils.hasText(token)) {
            log.warn("Attempted to blacklist empty or null token");
            return;
        }
        try {
            if (redisService != null) {
                redisService.blacklistToken(token);
                log.debug("Token successfully blacklisted using Redis service");
            } else if (inMemoryService != null) {
                inMemoryService.blacklistToken(token);
                log.debug("Token successfully blacklisted using in-memory service");
            } else {
                log.error("No blacklist service available - token cannot be blacklisted");
                throw new IllegalStateException("Blacklist service unavailable");
            }
        } catch (IllegalStateException ex) {
            log.error("Failed to blacklist token: {}", ex.getMessage(), ex);
            throw ex;
        } catch (RuntimeException ex) {
            log.error("Failed to blacklist token: {}", ex.getMessage(), ex);
            throw new IllegalStateException("Token blacklisting failed", ex);
        }
    }

    public void validate(String token) {
        if (!StringUtils.hasText(token)) {
            log.warn("Token validation failed: empty or null token");
            throw new JwtTokenBlacklistedException("Invalid token format");
        }

        try {
            if (checkTokenBlacklisted(token)) {
                log.warn("Token validation failed: token is blacklisted");
                throw new JwtTokenBlacklistedException("Token has been revoked");
            }
            log.debug("Token validation successful: token is not blacklisted");
        } catch (JwtTokenBlacklistedException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            log.error("Token validation failed due to service error: {}", ex.getMessage(), ex);
            throw new JwtTokenBlacklistedException("Token validation service unavailable");
        }
    }

    private boolean checkTokenBlacklisted(String token) {
        if (redisService != null) {
            return redisService.isBlacklisted(token);
        } else if (inMemoryService != null) {
            return inMemoryService.isBlacklisted(token);
        } else {
            log.error("No blacklist service available - failing secure for token validation");
            return true; // Fail secure
        }
    }
}
