package com.zufar.icedlatte.security.service;

import com.zufar.icedlatte.security.service.session.AuthSessionService;
import com.zufar.icedlatte.security.service.session.SessionTokenService;

import com.zufar.icedlatte.security.entity.AuthSessionEntity;
import com.zufar.icedlatte.security.exception.JwtTokenBlacklistedException;
import com.zufar.icedlatte.security.jwt.JwtBearerTokenResolver;
import com.zufar.icedlatte.security.jwt.JwtTokenClaims;
import com.zufar.icedlatte.security.jwt.JwtTokenBlacklist;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final JwtBearerTokenResolver jwtBearerTokenResolver;
    private final JwtTokenClaims jwtTokenClaims;
    private final JwtTokenBlacklist jwtTokenBlacklist;
    private final UserDetailsService userDetailsService;
    private final AuthSessionService authSessionService;
    private final SessionTokenService sessionTokenService;

    @Transactional
    public ResponseEntity<com.zufar.icedlatte.openapi.dto.UserAuthenticationResponse> refresh(HttpServletRequest request) {
        log.debug("auth.token.refreshing");
        String rawToken = jwtBearerTokenResolver.extract(request);
        String hash = jwtTokenBlacklist.hash(rawToken);

        AuthSessionEntity session;
        try {
            session = authSessionService.findActiveByHash(hash);
        } catch (JwtTokenBlacklistedException ex) {
            if (jwtTokenClaims.isSessionManagedRefreshToken(rawToken)) {
                jwtTokenClaims.extractRefreshTokenSessionId(rawToken)
                        .ifPresent(authSessionService::revokeAllForUserBySessionId);
                throw ex;
            }
            log.warn("auth.token.refresh_legacy_migrate: reason=token_invalidated");
            String userEmail = extractRefreshTokenEmail(rawToken);
            UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);
            var response = sessionTokenService.migrateLegacyRefreshToken(userDetails, rawToken, request);
            log.info("auth.token.refresh_legacy_migrated");
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }

        String userEmail = extractRefreshTokenEmail(rawToken);
        UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);
        var response = sessionTokenService.rotateSessionTokens(session, hash, userDetails);
        log.debug("auth.token.refreshed");
        return ResponseEntity.ok(response);
    }

    private String extractRefreshTokenEmail(String rawToken) {
        jwtTokenBlacklist.validateNotBlacklisted(rawToken);
        return jwtTokenClaims.extractRefreshTokenEmail(rawToken);
    }
}
