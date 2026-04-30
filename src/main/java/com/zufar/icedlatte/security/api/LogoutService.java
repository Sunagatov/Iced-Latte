package com.zufar.icedlatte.security.api;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LogoutService {

    private final TokenRevocationService tokenRevocationService;
    private final AuthSessionService authSessionService;

    public void logout(String xRefreshToken, HttpServletRequest request) {
        log.debug("auth.logout.processing");
        tokenRevocationService.revokeTokens(xRefreshToken, request);
        log.debug("auth.logout.completed");
    }

    public void logoutAll(UUID userId) {
        authSessionService.revokeAllForUser(userId);
        log.info("auth.logout_all.completed: userId={}", userId);
    }
}
