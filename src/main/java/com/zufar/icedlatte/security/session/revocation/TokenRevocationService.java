package com.zufar.icedlatte.security.session.revocation;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.zufar.icedlatte.security.jwt.blacklist.JwtTokenBlacklist;
import com.zufar.icedlatte.security.jwt.resolver.JwtBearerTokenResolver;
import com.zufar.icedlatte.security.session.management.AuthSessionService;
import com.zufar.icedlatte.security.signin.exception.AbsentBearerHeaderException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenRevocationService {

    private final JwtTokenBlacklist jwtTokenBlacklist;
    private final JwtBearerTokenResolver jwtBearerTokenResolver;
    private final AuthSessionService authSessionService;

    public void revokeTokens(String refreshTokenHeader, HttpServletRequest request) {
        resolveRefreshToken(refreshTokenHeader, request).ifPresent(this::revokeRefreshToken);
        revokeAccessToken(request);
    }

    private java.util.Optional<String> resolveRefreshToken(String refreshTokenHeader, HttpServletRequest request) {
        if (StringUtils.hasText(refreshTokenHeader)) {
            return java.util.Optional.of(refreshTokenHeader);
        }

        try {
            return java.util.Optional.of(jwtBearerTokenResolver.extract(request));
        } catch (AbsentBearerHeaderException _) {
            return java.util.Optional.empty();
        }
    }

    private void revokeRefreshToken(String refreshToken) {
        authSessionService.revokeByRefreshTokenHash(jwtTokenBlacklist.hash(refreshToken));
        jwtTokenBlacklist.blacklist(refreshToken);
    }

    private void revokeAccessToken(HttpServletRequest request) {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(authHeader)) {
            return;
        }

        try {
            jwtTokenBlacklist.blacklist(jwtBearerTokenResolver.extract(authHeader));
        } catch (AbsentBearerHeaderException ex) {
            log.debug("auth.logout.token_error: header={} reason={}", HttpHeaders.AUTHORIZATION, ex.getMessage());
        }
    }
}
