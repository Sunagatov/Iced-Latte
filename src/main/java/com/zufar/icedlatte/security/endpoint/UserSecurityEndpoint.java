package com.zufar.icedlatte.security.endpoint;

import com.zufar.icedlatte.openapi.dto.ConfirmEmailRequest;
import com.zufar.icedlatte.openapi.dto.ForgotPasswordRequest;
import com.zufar.icedlatte.openapi.dto.ChangePasswordRequest;
import com.zufar.icedlatte.openapi.dto.SessionInfo;
import com.zufar.icedlatte.openapi.dto.UserAuthenticationRequest;
import com.zufar.icedlatte.openapi.dto.UserAuthenticationResponse;
import com.zufar.icedlatte.openapi.dto.UserRegistrationRequest;
import com.zufar.icedlatte.openapi.security.api.SecurityApi;
import com.zufar.icedlatte.security.api.AuthSessionService;
import com.zufar.icedlatte.security.api.LogoutService;
import com.zufar.icedlatte.security.api.PasswordResetService;
import com.zufar.icedlatte.security.api.RefreshTokenService;
import com.zufar.icedlatte.security.api.SecurityPrincipalProvider;
import com.zufar.icedlatte.security.api.UserAuthenticationService;
import com.zufar.icedlatte.security.entity.AuthSessionEntity;
import com.zufar.icedlatte.email.api.EmailTokenConformer;
import com.zufar.icedlatte.email.api.EmailTokenSender;
import com.zufar.icedlatte.security.jwt.JwtBlacklistService;
import com.zufar.icedlatte.security.jwt.JwtTokenProvider;
import com.zufar.icedlatte.user.entity.UserEntity;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Slf4j
@Validated
@RestController
@RequestMapping(UserSecurityEndpoint.USER_SECURITY_API_URL)
@RequiredArgsConstructor
public class UserSecurityEndpoint implements SecurityApi {

    public static final String USER_SECURITY_API_URL = "/api/v1/auth/";

    private static final String MDC_USER_ID = "userId";
    private static final String MDC_SESSION_ID = "sessionId";

    private final UserAuthenticationService userAuthenticationService;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtBlacklistService jwtBlacklistService;
    private final EmailTokenSender emailTokenSender;
    private final EmailTokenConformer emailTokenConformer;
    private final AuthSessionService authSessionService;
    private final SecurityPrincipalProvider securityPrincipalProvider;
    private final RefreshTokenService refreshTokenService;
    private final LogoutService logoutService;
    private final PasswordResetService passwordResetService;
    private final HttpServletRequest httpRequest;

    @Override
    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody @Valid final UserRegistrationRequest request) {
        log.debug("auth.register.processing");
        emailTokenSender.sendEmailVerificationCode(request);
        log.debug("auth.register.email_sent");
        return ResponseEntity.ok("Email verification token sent");
    }

    @Override
    @PostMapping("/confirm")
    public ResponseEntity<UserAuthenticationResponse> confirmEmail(@Validated @Valid @RequestBody final ConfirmEmailRequest confirmEmailRequest) {
        log.debug("auth.email.confirming");
        var response = emailTokenConformer.confirmEmailByCode(confirmEmailRequest, httpRequest);
        log.info("auth.email.confirmed");
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    @PostMapping("/authenticate")
    public ResponseEntity<UserAuthenticationResponse> authenticate(@Valid @RequestBody final UserAuthenticationRequest request) {
        UserDetails userDetails = userAuthenticationService.verifyCredentials(request);
        UUID sessionId = UUID.randomUUID();
        String refreshToken = jwtTokenProvider.generateRefreshToken(userDetails, sessionId);
        AuthSessionEntity session =
                authSessionService.createSession(sessionId, ((UserEntity) userDetails).getId(),
                        jwtBlacklistService.sha256(refreshToken), httpRequest);
        MDC.put(MDC_USER_ID, session.getUserId().toString());
        MDC.put(MDC_SESSION_ID, session.getId().toString());
        var response = userAuthenticationService.buildTokenPair(userDetails, request.getEmail(), sessionId, refreshToken);
        return ResponseEntity.ok(response);
    }

    @Override
    @PostMapping("/refresh")
    public ResponseEntity<UserAuthenticationResponse> refreshToken() {
        return refreshTokenService.refresh(httpRequest);
    }

    @Override
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader(name = "X-Refresh-Token", required = false)
                                       String xRefreshToken) {
        logoutService.logout(xRefreshToken, httpRequest);
        return ResponseEntity.ok().build();
    }

    @Override
    @PostMapping("/logout-all")
    public ResponseEntity<Void> logoutAll() {
        log.debug("auth.logout_all.processing");
        logoutService.logoutAll(securityPrincipalProvider.getUserId());
        return ResponseEntity.ok().build();
    }

    @Override
    @GetMapping("/sessions")
    public ResponseEntity<List<SessionInfo>> getSessions() {
        UUID userId = securityPrincipalProvider.getUserId();
        List<SessionInfo> sessions = authSessionService.listActiveSessions(userId).stream()
                .map(s -> new SessionInfo()
                        .sessionId(s.getId())
                        .createdAt(s.getCreatedAt())
                        .expiresAt(s.getExpiresAt())
                        .lastUsedAt(s.getLastUsedAt())
                        .userAgent(s.getUserAgent())
                        .ipAddress(s.getIpAddress())
                )
                .toList();
        return ResponseEntity.ok(sessions);
    }

    @Override
    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<Void> revokeSession(@PathVariable UUID sessionId) {
        UUID userId = securityPrincipalProvider.getUserId();
        authSessionService.revokeById(sessionId, userId);
        log.info("auth.session.revoked_remote: sessionId={}, userId={}", sessionId, userId);
        return ResponseEntity.noContent().build();
    }

    @Override
    @PostMapping("/password/forgot")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody final ForgotPasswordRequest request) {
        log.debug("auth.password.forgot.processing");
        passwordResetService.requestReset(request.getEmail());
        return ResponseEntity.ok().build();
    }

    @Override
    // amazonq-ignore-next-line
    @PostMapping("/password/change")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody final ChangePasswordRequest request) {
        passwordResetService.confirmReset(request.getCode(), request.getPassword());
        return ResponseEntity.ok().build();
    }
}
