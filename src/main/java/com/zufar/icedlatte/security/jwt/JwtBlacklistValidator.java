package com.zufar.icedlatte.security.jwt;

import com.zufar.icedlatte.security.exception.JwtTokenBlacklistedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtBlacklistValidator {

    private final JwtBlacklistService blacklistService;

    public void addToBlacklist(String token) {
        if (!StringUtils.hasText(token)) {
            log.warn("Attempted to blacklist empty or null token");
            return;
        }
        blacklistService.blacklistToken(token);
    }

    public void validate(String token) {
        if (!StringUtils.hasText(token)) {
            throw new JwtTokenBlacklistedException("Invalid token format");
        }
        if (blacklistService.isBlacklisted(token)) {
            log.warn("Token validation failed: token is blacklisted");
            throw new JwtTokenBlacklistedException("Token has been revoked");
        }
    }
}
