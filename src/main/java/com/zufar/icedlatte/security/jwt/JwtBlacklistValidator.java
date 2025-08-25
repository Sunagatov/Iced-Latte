package com.zufar.icedlatte.security.jwt;

import com.zufar.icedlatte.security.exception.JwtTokenBlacklistedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Slf4j
@Service
public class JwtBlacklistValidator {

    private final Optional<RedisJwtBlacklistService> redisJwtBlacklistService;
    private final Optional<InMemoryJwtBlacklistService> inMemoryJwtBlacklistService;

    public JwtBlacklistValidator(Optional<RedisJwtBlacklistService> redisJwtBlacklistService,
                                 Optional<InMemoryJwtBlacklistService> inMemoryJwtBlacklistService) {
        this.redisJwtBlacklistService = redisJwtBlacklistService;
        this.inMemoryJwtBlacklistService = inMemoryJwtBlacklistService;

        if (redisJwtBlacklistService.isPresent()) {
            log.info("Using Redis JWT blacklist service");
        } else if (inMemoryJwtBlacklistService.isPresent()) {
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

        redisJwtBlacklistService.ifPresentOrElse(
                service -> {
                    service.blacklistToken(token);
                    log.debug("Token blacklisted using Redis service");
                },
                () -> inMemoryJwtBlacklistService.ifPresentOrElse(
                        service -> {
                            service.blacklistToken(token);
                            log.debug("Token blacklisted using in-memory service");
                        },
                        () -> log.error("No blacklist service available - token cannot be blacklisted")
                )
        );
    }

    public void validate(String token) {
        if (!StringUtils.hasText(token)) {
            log.warn("Attempted to validate empty or null token");
            throw new JwtTokenBlacklistedException("Invalid token format");
        }

        boolean isBlacklisted = redisJwtBlacklistService
                .map(service -> service.isBlacklisted(token))
                .orElseGet(() -> inMemoryJwtBlacklistService
                        .map(service -> service.isBlacklisted(token))
                        .orElseThrow(() -> {
                            log.error("No blacklist service available - failing secure for token validation");
                            return new JwtTokenBlacklistedException("Blacklist service unavailable");
                        })
                );

        if (isBlacklisted) {
            log.warn("Attempted to use blacklisted JWT token");
            throw new JwtTokenBlacklistedException("Token has been revoked");
        }
    }
}
