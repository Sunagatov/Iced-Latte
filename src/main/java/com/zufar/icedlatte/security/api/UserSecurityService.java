package com.zufar.icedlatte.security.api;

import com.zufar.icedlatte.email.api.EmailTokenConformer;
import com.zufar.icedlatte.email.api.EmailTokenSender;
import com.zufar.icedlatte.openapi.dto.ChangePasswordRequest;
import com.zufar.icedlatte.openapi.dto.ConfirmEmailRequest;
import com.zufar.icedlatte.openapi.dto.ForgotPasswordRequest;
import com.zufar.icedlatte.openapi.dto.SessionInfo;
import com.zufar.icedlatte.openapi.dto.UserAuthenticationRequest;
import com.zufar.icedlatte.openapi.dto.UserAuthenticationResponse;
import com.zufar.icedlatte.openapi.dto.UserRegistrationRequest;
import com.zufar.icedlatte.security.entity.AuthSessionEntity;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserSecurityService {

    private final UserAuthenticationService userAuthenticationService;
    private final SessionTokenService sessionTokenService;
    private final EmailTokenSender emailTokenSender;
    private final EmailTokenConformer emailTokenConformer;
    private final AuthSessionService authSessionService;
    private final RefreshTokenService refreshTokenService;
    private final LogoutService logoutService;
    private final PasswordResetService passwordResetService;

    public void requestRegistration(UserRegistrationRequest request) {
        log.debug("auth.register.processing");
        emailTokenSender.sendEmailVerificationCode(request);
        log.debug("auth.register.email_sent");
    }

    public UserAuthenticationResponse confirmEmail(ConfirmEmailRequest confirmEmailRequest,
                                                   HttpServletRequest httpRequest) {
        log.debug("auth.email.confirming");
        UserAuthenticationResponse response =
                emailTokenConformer.confirmEmailByCode(confirmEmailRequest, httpRequest);
        log.info("auth.email.confirmed");
        return response;
    }

    public UserAuthenticationResponse authenticate(UserAuthenticationRequest request,
                                                   HttpServletRequest httpRequest) {
        UserDetails userDetails = userAuthenticationService.verifyCredentials(request);
        return sessionTokenService.issueForNewSession(userDetails, httpRequest);
    }

    public org.springframework.http.ResponseEntity<UserAuthenticationResponse> refresh(HttpServletRequest httpRequest) {
        return refreshTokenService.refresh(httpRequest);
    }

    public void logout(String refreshToken, HttpServletRequest httpRequest) {
        logoutService.logout(refreshToken, httpRequest);
    }

    public void logoutAll(UUID userId) {
        log.debug("auth.logout_all.processing");
        logoutService.logoutAll(userId);
    }

    public List<SessionInfo> getSessions(UUID userId) {
        return authSessionService.listActiveSessions(userId).stream()
                .map(this::toSessionInfo)
                .toList();
    }

    public void revokeSession(UUID sessionId, UUID userId) {
        authSessionService.revokeById(sessionId, userId);
        log.info("auth.session.revoked_remote: sessionId={}, userId={}", sessionId, userId);
    }

    public void requestPasswordReset(ForgotPasswordRequest request) {
        log.debug("auth.password.forgot.processing");
        passwordResetService.requestReset(request.getEmail());
    }

    public void confirmPasswordReset(ChangePasswordRequest request) {
        passwordResetService.confirmReset(request.getCode(), request.getPassword());
    }

    private SessionInfo toSessionInfo(AuthSessionEntity session) {
        return new SessionInfo()
                .sessionId(session.getId())
                .createdAt(session.getCreatedAt())
                .expiresAt(session.getExpiresAt())
                .lastUsedAt(session.getLastUsedAt())
                .userAgent(session.getUserAgent())
                .ipAddress(session.getIpAddress());
    }
}
