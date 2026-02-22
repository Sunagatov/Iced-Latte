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
            log.warn("jwt.blacklist.empty_token");
            return;
        }
        blacklistService.blacklistToken(token);
    }

    public void validate(String token) {
        if (!StringUtils.hasText(token)) {
            throw new JwtTokenBlacklistedException("Invalid token format");
        }
        if (blacklistService.isBlacklisted(token)) {
            log.warn("jwt.blacklist.token_revoked");
            throw new JwtTokenBlacklistedException("Token has been revoked");
        }
    }
}
