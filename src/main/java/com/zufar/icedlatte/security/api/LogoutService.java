package com.zufar.icedlatte.security.api;

import com.zufar.icedlatte.security.exception.AbsentBearerHeaderException;
import com.zufar.icedlatte.security.jwt.JwtBlacklistService;
import com.zufar.icedlatte.security.jwt.JwtBlacklistValidator;
import com.zufar.icedlatte.security.jwt.JwtRefreshTokenValidator;
import com.zufar.icedlatte.security.jwt.JwtTokenFromAuthHeaderExtractor;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LogoutService {

    private final JwtRefreshTokenValidator jwtRefreshTokenValidator;
    private final JwtBlacklistService jwtBlacklistService;
    private final JwtBlacklistValidator jwtBlacklistValidator;
    private final JwtTokenFromAuthHeaderExtractor jwtTokenFromAuthHeaderExtractor;
    private final AuthSessionService authSessionService;

    public void logout(String xRefreshToken, HttpServletRequest request) {
        log.debug("auth.logout.processing");

        String refreshToken = xRefreshToken;
        if (!StringUtils.hasText(refreshToken)) {
            try {
                refreshToken = jwtRefreshTokenValidator.extractRawToken(request);
            } catch (AbsentBearerHeaderException _) {
                // no refresh token provided — access token blacklist only
            }
        }

        if (StringUtils.hasText(refreshToken)) {
            authSessionService.revokeByRefreshTokenHash(jwtBlacklistService.sha256(refreshToken));
            jwtBlacklistValidator.addToBlacklist(refreshToken);
        }

        String authHeader = request.getHeader("Authorization");
        if (StringUtils.hasText(authHeader)) {
            try {
                jwtBlacklistValidator.addToBlacklist(jwtTokenFromAuthHeaderExtractor.extract(authHeader));
            } catch (AbsentBearerHeaderException ex) {
                log.debug("auth.logout.token_error: header=Authorization reason={}", ex.getMessage());
            }
        }

        log.debug("auth.logout.completed");
    }

    public void logoutAll(UUID userId) {
        authSessionService.revokeAllForUser(userId);
        log.info("auth.logout_all.completed: userId={}", userId);
    }
}
