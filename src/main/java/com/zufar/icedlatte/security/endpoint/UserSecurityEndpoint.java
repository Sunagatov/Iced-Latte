package com.zufar.icedlatte.security.endpoint;

import com.zufar.icedlatte.openapi.dto.ConfirmEmailRequest;
import com.zufar.icedlatte.openapi.dto.ForgotPasswordRequest;
import com.zufar.icedlatte.openapi.dto.ChangePasswordRequest;
import com.zufar.icedlatte.openapi.dto.SessionInfo;
import com.zufar.icedlatte.openapi.dto.UserAuthenticationRequest;
import com.zufar.icedlatte.openapi.dto.UserAuthenticationResponse;
import com.zufar.icedlatte.openapi.dto.UserRegistrationRequest;
import com.zufar.icedlatte.openapi.security.api.SecurityApi;
import com.zufar.icedlatte.email.api.EmailVerificationService;
import com.zufar.icedlatte.security.api.AuthSessionService;
import com.zufar.icedlatte.security.api.LogoutService;
import com.zufar.icedlatte.security.api.PasswordResetService;
import com.zufar.icedlatte.security.api.RefreshTokenService;
import com.zufar.icedlatte.security.api.SecurityPrincipalProvider;
import com.zufar.icedlatte.security.api.SessionTokenService;
import com.zufar.icedlatte.security.api.UserAuthenticationService;
import com.zufar.icedlatte.security.configuration.AuthPaths;
import com.zufar.icedlatte.security.entity.AuthSessionEntity;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    public static final String USER_SECURITY_API_URL = AuthPaths.ROOT;

    private final UserAuthenticationService userAuthenticationService;
    private final SessionTokenService sessionTokenService;
    private final EmailVerificationService emailVerificationService;
    private final AuthSessionService authSessionService;
    private final RefreshTokenService refreshTokenService;
    private final LogoutService logoutService;
    private final PasswordResetService passwordResetService;
    private final SecurityPrincipalProvider securityPrincipalProvider;
    private final HttpServletRequest httpRequest;

    @Override
    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody @Valid final UserRegistrationRequest request) {
        emailVerificationService.sendEmailVerificationCode(request);
        return ResponseEntity.ok("Email verification token sent");
    }

    @Override
    @PostMapping("/confirm")
    public ResponseEntity<UserAuthenticationResponse> confirmEmail(@Validated @Valid @RequestBody final ConfirmEmailRequest confirmEmailRequest) {
        var response = emailVerificationService.confirmEmailByCode(confirmEmailRequest, httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    @PostMapping("/authenticate")
    public ResponseEntity<UserAuthenticationResponse> authenticate(@Valid @RequestBody final UserAuthenticationRequest request) {
        var userDetails = userAuthenticationService.verifyCredentials(request);
        return ResponseEntity.ok(sessionTokenService.issueForNewSession(userDetails, httpRequest));
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
        logoutService.logoutAll(securityPrincipalProvider.getUserId());
        return ResponseEntity.ok().build();
    }

    @Override
    @GetMapping("/sessions")
    public ResponseEntity<List<SessionInfo>> getSessions() {
        return ResponseEntity.ok(authSessionService.listActiveSessions(securityPrincipalProvider.getUserId()).stream()
                .map(this::toSessionInfo)
                .toList());
    }

    @Override
    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<Void> revokeSession(@PathVariable UUID sessionId) {
        authSessionService.revokeById(sessionId, securityPrincipalProvider.getUserId());
        return ResponseEntity.noContent().build();
    }

    @Override
    @PostMapping("/password/forgot")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody final ForgotPasswordRequest request) {
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
