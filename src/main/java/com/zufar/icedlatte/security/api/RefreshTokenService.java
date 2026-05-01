package com.zufar.icedlatte.security.api;

import com.zufar.icedlatte.security.entity.AuthSessionEntity;
import com.zufar.icedlatte.security.exception.JwtTokenBlacklistedException;
import com.zufar.icedlatte.security.jwt.JwtBlacklistStore;
import com.zufar.icedlatte.security.jwt.JwtRefreshTokenValidator;
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

    private final JwtRefreshTokenValidator jwtRefreshTokenValidator;
    private final JwtBlacklistStore jwtBlacklistService;
    private final UserDetailsService userDetailsService;
    private final AuthSessionService authSessionService;
    private final SessionTokenService sessionTokenService;

    @Transactional
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
            log.warn("auth.token.refresh_legacy_migrate: reason=token_invalidated");
            String userEmail = jwtRefreshTokenValidator.extractEmail(request);
            UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);
            var response = sessionTokenService.migrateLegacyRefreshToken(userDetails, rawToken, request);
            log.info("auth.token.refresh_legacy_migrated");
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }

        String userEmail = jwtRefreshTokenValidator.extractEmail(request);
        UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);
        var response = sessionTokenService.rotateSessionTokens(session, hash, userDetails);
        log.debug("auth.token.refreshed");
        return ResponseEntity.ok(response);
    }
}
