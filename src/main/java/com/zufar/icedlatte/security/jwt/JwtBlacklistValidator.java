package com.zufar.icedlatte.security.jwt;

import com.zufar.icedlatte.security.exception.JwtTokenBlacklistedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class JwtBlacklistValidator {

    private final RedisJwtBlacklistService redisJwtBlacklistService;
    private final InMemoryJwtBlacklistService inMemoryJwtBlacklistService;

    public JwtBlacklistValidator(@Autowired(required = false) RedisJwtBlacklistService redisJwtBlacklistService,
                                 @Autowired(required = false) InMemoryJwtBlacklistService inMemoryJwtBlacklistService) {
        this.redisJwtBlacklistService = redisJwtBlacklistService;
        this.inMemoryJwtBlacklistService = inMemoryJwtBlacklistService;

        if (this.redisJwtBlacklistService != null) {
            log.info("Using Redis JWT blacklist service");
        } else if (this.inMemoryJwtBlacklistService != null) {
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

        if (redisJwtBlacklistService != null) {
            redisJwtBlacklistService.blacklistToken(token);
            log.debug("Token blacklisted using Redis service");
        } else if (inMemoryJwtBlacklistService != null) {
            inMemoryJwtBlacklistService.blacklistToken(token);
            log.debug("Token blacklisted using in-memory service");
        } else {
            log.error("No blacklist service available - token cannot be blacklisted");
        }
    }

    public void validate(String token) {
        if (!StringUtils.hasText(token)) {
            log.warn("Attempted to validate empty or null token");
            throw new JwtTokenBlacklistedException("Invalid token format");
        }

        boolean isBlacklisted;
        if (redisJwtBlacklistService != null) {
            isBlacklisted = redisJwtBlacklistService.isBlacklisted(token);
        } else if (inMemoryJwtBlacklistService != null) {
            isBlacklisted = inMemoryJwtBlacklistService.isBlacklisted(token);
        } else {
            log.error("No blacklist service available - failing secure for token validation");
            throw new JwtTokenBlacklistedException("Blacklist service unavailable");
        }

        if (isBlacklisted) {
            log.warn("Attempted to use blacklisted JWT token");
            throw new JwtTokenBlacklistedException("Token has been revoked");
        }
    }
}
