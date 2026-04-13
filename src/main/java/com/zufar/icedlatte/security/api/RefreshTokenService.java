package com.zufar.icedlatte.security.api;

import com.zufar.icedlatte.security.entity.AuthSessionEntity;
import com.zufar.icedlatte.security.exception.JwtTokenBlacklistedException;
import com.zufar.icedlatte.security.jwt.JwtBlacklistService;
import com.zufar.icedlatte.security.jwt.JwtBlacklistValidator;
import com.zufar.icedlatte.security.jwt.JwtRefreshTokenValidator;
import com.zufar.icedlatte.security.jwt.JwtTokenProvider;
import com.zufar.icedlatte.user.entity.UserEntity;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final String MDC_USER_ID = "userId";
    private static final String MDC_SESSION_ID = "sessionId";

    private final JwtRefreshTokenValidator jwtRefreshTokenValidator;
    private final JwtBlacklistService jwtBlacklistService;
    private final JwtBlacklistValidator jwtBlacklistValidator;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;
    private final AuthSessionService authSessionService;
    private final UserAuthenticationService userAuthenticationService;

    public ResponseEntity<com.zufar.icedlatte.openapi.dto.UserAuthenticationResponse> refresh(HttpServletRequest request) {
        log.debug("auth.token.refreshing");
        String rawToken = jwtRefreshTokenValidator.extractRawToken(request);
        String hash = jwtBlacklistService.sha256(rawToken);

        AuthSessionEntity session;
        try {
            session = authSessionService.findActiveByHash(hash);
        } catch (JwtTokenBlacklistedException ex) {
            if (jwtRefreshTokenValidator.isSessionManaged(rawToken)) {
                jwtRefreshTokenValidator.extractSessionId(rawToken)
                        .ifPresent(authSessionService::revokeAllForUserBySessionId);
                throw ex;
            }
            log.warn("auth.token.refresh_legacy_migrate: reason={}", ex.getMessage());
            String userEmail = jwtRefreshTokenValidator.extractEmail(request);
            UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);
            UUID newSessionId = UUID.randomUUID();
            String newRefreshToken = jwtTokenProvider.generateRefreshToken(userDetails, newSessionId);
            AuthSessionEntity newSession = authSessionService.createSession(
                    newSessionId,
                    ((UserEntity) userDetails).getId(),
                    jwtBlacklistService.sha256(newRefreshToken),
                    request);
            jwtBlacklistValidator.addToBlacklist(rawToken);
            MDC.put(MDC_SESSION_ID, newSession.getId().toString());
            MDC.put(MDC_USER_ID, newSession.getUserId().toString());
            var response = userAuthenticationService.buildTokenPair(userDetails, userEmail, newSession.getId(), newRefreshToken);
            log.info("auth.token.refresh_legacy_migrated");
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }

        MDC.put(MDC_SESSION_ID, session.getId().toString());
        MDC.put(MDC_USER_ID, session.getUserId().toString());
        String userEmail = jwtRefreshTokenValidator.extractEmail(request);
        UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(userDetails, session.getId());
        authSessionService.rotateSession(hash, jwtBlacklistService.sha256(newRefreshToken));
        var response = userAuthenticationService.buildTokenPair(userDetails, userEmail, session.getId(), newRefreshToken);
        log.debug("auth.token.refreshed");
        return ResponseEntity.ok(response);
    }
}
