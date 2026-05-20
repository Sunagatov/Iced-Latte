package com.zufar.icedlatte.security.service.session;

import com.zufar.icedlatte.security.exception.AbsentBearerHeaderException;
import com.zufar.icedlatte.security.jwt.JwtBearerTokenResolver;
import com.zufar.icedlatte.security.jwt.JwtTokenBlacklist;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

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
