package com.zufar.icedlatte.security.endpoint;

import com.zufar.icedlatte.security.oauth.api.OAuthFlowService;
import com.zufar.icedlatte.security.oauth.api.OAuthProvider;
import com.zufar.icedlatte.openapi.dto.ConfirmEmailRequest;
import com.zufar.icedlatte.openapi.dto.ForgotPasswordRequest;
import com.zufar.icedlatte.openapi.dto.ChangePasswordRequest;
import com.zufar.icedlatte.openapi.dto.SessionInfo;
import com.zufar.icedlatte.openapi.dto.UserAuthenticationRequest;
import com.zufar.icedlatte.openapi.dto.UserAuthenticationResponse;
import com.zufar.icedlatte.openapi.dto.UserRegistrationRequest;
import com.zufar.icedlatte.openapi.security.api.SecurityApi;
import com.zufar.icedlatte.security.api.EmailVerificationService;
import com.zufar.icedlatte.security.api.AuthSessionService;
import com.zufar.icedlatte.security.api.LogoutService;
import com.zufar.icedlatte.security.api.PasswordResetService;
import com.zufar.icedlatte.security.api.RefreshTokenService;
import com.zufar.icedlatte.security.api.SecurityPrincipalProvider;
import com.zufar.icedlatte.security.api.SessionTokenService;
import com.zufar.icedlatte.security.api.UserAuthenticationService;
import com.zufar.icedlatte.security.api.UserRegistrationService;
import com.zufar.icedlatte.security.configuration.AuthPaths;
import com.zufar.icedlatte.security.turnstile.TurnstileVerifier;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.List;
import java.util.UUID;

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
    private final UserRegistrationService userRegistrationService;
    private final TurnstileVerifier turnstileVerifier;
    private final HttpServletRequest httpRequest;
    private final OAuthFlowService oAuthFlowService;

    @Value("${email.enabled:false}")
    private boolean emailEnabled;

    @Override
    @GetMapping("/oauth/{provider}")
    public ResponseEntity<Void> initiateOAuth(@PathVariable String provider,
                                              @Valid @RequestParam(required = false) URI redirectUrl) {
        OAuthProvider oAuthProvider = parseProvider(provider);
        return oAuthFlowService.initiate(oAuthProvider, redirectUrl == null ? null : redirectUrl.toString())
                .map(authUri -> ResponseEntity.status(HttpStatus.FOUND).location(authUri).<Void>build())
                .orElseGet(() -> ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build());
    }

    @Override
    @GetMapping("/oauth/{provider}/callback")
    public ResponseEntity<Void> completeOAuthCallback(@PathVariable String provider,
                                                      @Valid @RequestParam(required = false) String code,
                                                      @Valid @RequestParam(required = false) String state) {
        OAuthProvider oAuthProvider = parseProvider(provider);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(oAuthFlowService.completeCallback(oAuthProvider, code, state, httpRequest))
                .build();
    }

    @Override
    @PostMapping("/register")
    public ResponseEntity<UserAuthenticationResponse> register(@RequestBody @Valid final UserRegistrationRequest request) {
        turnstileVerifier.verify(request.getTurnstileToken());
        if (emailEnabled) {
            emailVerificationService.sendEmailVerificationCode(request);
            return ResponseEntity.ok().build();
        }
        userRegistrationService.ensureEmailAvailable(request);
        return ResponseEntity.ok(userRegistrationService.register(request, httpRequest));
    }

    @Override
    @PostMapping("/confirm")
    public ResponseEntity<UserAuthenticationResponse> confirmEmail(@Valid @RequestBody final ConfirmEmailRequest confirmEmailRequest) {
        var response = emailVerificationService.confirmEmailByCode(confirmEmailRequest, httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    @PostMapping("/authenticate")
    public ResponseEntity<UserAuthenticationResponse> authenticate(@Valid @RequestBody final UserAuthenticationRequest request) {
        turnstileVerifier.verify(request.getTurnstileToken());
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
        return ResponseEntity.ok(authSessionService.listActiveSessionInfos(securityPrincipalProvider.getUserId()));
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

    private OAuthProvider parseProvider(String provider) {
        return OAuthProvider.fromId(provider)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "OAuth provider is not supported."));
    }
}
