package com.zufar.icedlatte.security.api;

import com.zufar.icedlatte.common.correlation.RequestContextConstants;
import com.zufar.icedlatte.openapi.dto.UserAuthenticationResponse;
import com.zufar.icedlatte.security.entity.AuthSessionEntity;
import com.zufar.icedlatte.security.jwt.JwtBlacklistStore;
import com.zufar.icedlatte.security.jwt.JwtBlacklistValidator;
import com.zufar.icedlatte.security.jwt.JwtTokenProvider;
import com.zufar.icedlatte.user.entity.UserEntity;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class SessionTokenService {

    private final JwtBlacklistStore jwtBlacklistService;
    private final JwtBlacklistValidator jwtBlacklistValidator;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthSessionService authSessionService;
    private final UserAuthenticationService userAuthenticationService;

    public UserAuthenticationResponse issueForNewSession(UserDetails userDetails,
                                                         HttpServletRequest request) {
        SessionAuthentication sessionAuthentication = createManagedSession(userDetails, UUID.randomUUID(), request);
        return withSessionMdc(sessionAuthentication.session(), sessionAuthentication::response);
    }

    public UserAuthenticationResponse rotateSessionTokens(AuthSessionEntity session,
                                                          String currentRefreshTokenHash,
                                                          UserDetails userDetails) {
        return withSessionMdc(session, () -> {
            String newRefreshToken = jwtTokenProvider.generateRefreshToken(userDetails, session.getId());
            authSessionService.rotateSession(currentRefreshTokenHash, jwtBlacklistService.sha256(newRefreshToken));
            return userAuthenticationService.buildTokenPair(userDetails, session.getId(), newRefreshToken);
        });
    }

    public UserAuthenticationResponse migrateLegacyRefreshToken(UserDetails userDetails,
                                                                String legacyRefreshToken,
                                                                HttpServletRequest request) {
        SessionAuthentication sessionAuthentication = createManagedSession(userDetails, UUID.randomUUID(), request);
        jwtBlacklistValidator.addToBlacklist(legacyRefreshToken);
        return withSessionMdc(sessionAuthentication.session(), sessionAuthentication::response);
    }

    private SessionAuthentication createManagedSession(UserDetails userDetails,
                                                       UUID sessionId,
                                                       HttpServletRequest request) {
        if (!(userDetails instanceof UserEntity user)) {
            throw new IllegalArgumentException("Expected UserEntity but got: " + userDetails.getClass().getName());
        }
        String refreshToken = jwtTokenProvider.generateRefreshToken(userDetails, sessionId);
        AuthSessionEntity session = authSessionService.createSession(
                sessionId,
                user.getId(),
                jwtBlacklistService.sha256(refreshToken),
                request
        );
        UserAuthenticationResponse response =
                userAuthenticationService.buildTokenPair(userDetails, session.getId(), refreshToken);
        return new SessionAuthentication(session, response);
    }

    private <T> T withSessionMdc(AuthSessionEntity session, Supplier<T> action) {
        bindSessionToMdc(session);
        try {
            return action.get();
        } finally {
            clearSessionMdc();
        }
    }

    private void bindSessionToMdc(AuthSessionEntity session) {
        MDC.put(RequestContextConstants.USER_ID_MDC_KEY, session.getUserId().toString());
        MDC.put(RequestContextConstants.SESSION_ID_MDC_KEY, session.getId().toString());
    }

    private void clearSessionMdc() {
        MDC.remove(RequestContextConstants.USER_ID_MDC_KEY);
        MDC.remove(RequestContextConstants.SESSION_ID_MDC_KEY);
    }

    private record SessionAuthentication(AuthSessionEntity session, UserAuthenticationResponse response) {
    }
}
