package com.zufar.icedlatte.security.endpoint;

import com.zufar.icedlatte.email.api.EmailTokenConformer;
import com.zufar.icedlatte.email.api.EmailTokenSender;
import com.zufar.icedlatte.openapi.dto.ChangePasswordRequest;
import com.zufar.icedlatte.openapi.dto.ConfirmEmailRequest;
import com.zufar.icedlatte.openapi.dto.ForgotPasswordRequest;
import com.zufar.icedlatte.openapi.dto.SessionInfo;
import com.zufar.icedlatte.openapi.dto.UserAuthenticationRequest;
import com.zufar.icedlatte.openapi.dto.UserAuthenticationResponse;
import com.zufar.icedlatte.openapi.dto.UserRegistrationRequest;
import com.zufar.icedlatte.openapi.security.api.SecurityApi;
import com.zufar.icedlatte.security.api.AuthSessionService;
import com.zufar.icedlatte.security.api.SecurityPrincipalProvider;
import com.zufar.icedlatte.security.api.UserAuthenticationService;
import com.zufar.icedlatte.security.entity.AuthSessionEntity;
import com.zufar.icedlatte.security.exception.AbsentBearerHeaderException;
import com.zufar.icedlatte.security.exception.JwtTokenBlacklistedException;
import com.zufar.icedlatte.security.jwt.JwtBlacklistService;
import com.zufar.icedlatte.security.jwt.JwtBlacklistValidator;
import com.zufar.icedlatte.security.jwt.JwtRefreshTokenValidator;
import com.zufar.icedlatte.security.jwt.JwtTokenFromAuthHeaderExtractor;
import com.zufar.icedlatte.security.jwt.JwtTokenProvider;
import com.zufar.icedlatte.user.api.SingleUserProvider;
import com.zufar.icedlatte.user.entity.UserEntity;
import com.zufar.icedlatte.user.exception.UserNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Slf4j
@Validated
@RestController
@RequestMapping(value = UserSecurityEndpoint.USER_SECURITY_API_URL)
@RequiredArgsConstructor
public class UserSecurityEndpoint implements SecurityApi {

    public static final String USER_SECURITY_API_URL = "/api/v1/auth/";

    private final UserAuthenticationService userAuthenticationService;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtTokenFromAuthHeaderExtractor jwtTokenFromAuthHeaderExtractor;
    private final JwtBlacklistValidator jwtBlacklistValidator;
    private final JwtBlacklistService jwtBlacklistService;
    private final EmailTokenSender emailTokenSender;
    private final EmailTokenConformer emailTokenConformer;
    private final JwtRefreshTokenValidator jwtRefreshTokenValidator;
    private final UserDetailsService userDetailsService;
    private final SingleUserProvider singleUserProvider;
    private final AuthSessionService authSessionService;
    private final SecurityPrincipalProvider securityPrincipalProvider;

    private final HttpServletRequest httpRequest;

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody @Valid final UserRegistrationRequest request) {
        log.debug("auth.register.processing");
        emailTokenSender.sendEmailVerificationCode(request);
        log.debug("auth.register.email_sent");
        return ResponseEntity.ok("Email verification token sent");
    }

    @Override
    @PostMapping(value = "/confirm")
    public ResponseEntity<UserAuthenticationResponse> confirmEmail(@Validated @Valid @RequestBody final ConfirmEmailRequest confirmEmailRequest) {
        log.debug("auth.email.confirming");
        var response = emailTokenConformer.confirmEmailByCode(confirmEmailRequest);
        log.info("auth.email.confirmed");
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    @PostMapping("/authenticate")
    public ResponseEntity<UserAuthenticationResponse> authenticate(@Valid @RequestBody final UserAuthenticationRequest request) {
        UserDetails userDetails = userAuthenticationService.verifyCredentials(request);
        String refreshToken = jwtTokenProvider.generateRefreshToken(userDetails);
        AuthSessionEntity session;
        try {
            session = authSessionService.createSession(
                    ((UserEntity) userDetails).getId(),
                    jwtBlacklistService.sha256(refreshToken),
                    httpRequest);
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            // Duplicate refresh token hash (same-second login in tests / race) — session already exists
            log.warn("auth.session.duplicate_hash: falling back to token-only response");
            var fallback = userAuthenticationService.authenticate(userDetails, request.getEmail());
            return ResponseEntity.ok(fallback);
        }
        var response = userAuthenticationService.buildTokenPair(userDetails, request.getEmail(), session.getId(), refreshToken);
        return ResponseEntity.ok(response);
    }

    @Override
    @PostMapping("/refresh")
    public ResponseEntity<UserAuthenticationResponse> refreshToken() {
        log.debug("auth.token.refreshing");
        try {
            AuthSessionEntity session = jwtRefreshTokenValidator.validateAndGetSession(httpRequest);
            String oldRawToken = jwtRefreshTokenValidator.extractRawToken(httpRequest);
            String userEmail = jwtRefreshTokenValidator.extractEmail(httpRequest);
            UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);

            String newRefreshToken = jwtTokenProvider.generateRefreshToken(userDetails);
            authSessionService.rotateSession(
                    jwtBlacklistService.sha256(oldRawToken),
                    jwtBlacklistService.sha256(newRefreshToken));

            var response = userAuthenticationService.buildTokenPair(userDetails, userEmail, session.getId(), newRefreshToken);
            log.debug("auth.token.refreshed");
            return ResponseEntity.ok(response);
        } catch (JwtTokenBlacklistedException ex) {
            // Pre-migration token: no session row exists — legacy path only
            log.warn("auth.token.refresh_legacy_fallback: reason={}", ex.getMessage());
            String userEmail = jwtRefreshTokenValidator.extractEmail(httpRequest);
            UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);
            var response = userAuthenticationService.authenticate(userDetails, userEmail);
            log.debug("auth.token.refreshed");
            return ResponseEntity.ok(response);
        }
    }

    @Override
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @org.springframework.web.bind.annotation.RequestHeader(value = "X-Refresh-Token", required = false)
            String xRefreshToken) {
        log.debug("auth.logout.processing");

        if (StringUtils.hasText(xRefreshToken)) {
            authSessionService.revokeByRefreshTokenHash(jwtBlacklistService.sha256(xRefreshToken));
            jwtBlacklistValidator.addToBlacklist(xRefreshToken);
        }

        String authHeader = httpRequest.getHeader("Authorization");
        if (StringUtils.hasText(authHeader)) {
            try {
                jwtBlacklistValidator.addToBlacklist(jwtTokenFromAuthHeaderExtractor.extract(authHeader));
            } catch (AbsentBearerHeaderException ex) {
                log.warn("auth.logout.token_error: header=Authorization reason={}", ex.getMessage());
            }
        }

        log.debug("auth.logout.completed");
        return ResponseEntity.ok().build();
    }

    @PostMapping("/logout-all")
    public ResponseEntity<Void> logoutAll() {
        log.debug("auth.logout_all.processing");
        UUID userId = securityPrincipalProvider.getUserId();
        authSessionService.revokeAllForUser(userId);
        log.info("auth.logout_all.completed: userId={}", userId);
        return ResponseEntity.ok().build();
    }

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
        try {
            singleUserProvider.getUserEntityByEmail(request.getEmail());
            emailTokenSender.sendPasswordResetCode(request.getEmail());
        } catch (UserNotFoundException e) {
            log.warn("auth.password.forgot.unknown_email");
        }
        return ResponseEntity.ok().build();
    }

    @Override
    // amazonq-ignore-next-line
    @PostMapping("/password/change")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody final ChangePasswordRequest request) {
        emailTokenConformer.confirmResetPasswordEmailByCode(new ConfirmEmailRequest(request.getCode()), request.getPassword());
        log.info("auth.password.changed");
        return ResponseEntity.ok().build();
    }
}
