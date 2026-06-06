package com.zufar.icedlatte.security.endpoint;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
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

import com.zufar.icedlatte.common.http.ApiPaths;
import com.zufar.icedlatte.common.util.ClientIpExtractor;
import com.zufar.icedlatte.openapi.dto.ChangePasswordRequest;
import com.zufar.icedlatte.openapi.dto.ConfirmEmailRequest;
import com.zufar.icedlatte.openapi.dto.ForgotPasswordRequest;
import com.zufar.icedlatte.openapi.dto.SessionInfo;
import com.zufar.icedlatte.openapi.dto.UserAuthenticationRequest;
import com.zufar.icedlatte.openapi.dto.UserAuthenticationResponse;
import com.zufar.icedlatte.openapi.dto.UserRegistrationRequest;
import com.zufar.icedlatte.openapi.security.api.SecurityApi;
import com.zufar.icedlatte.security.api.CurrentUserProvider;
import com.zufar.icedlatte.security.oauth.config.OAuthProvider;
import com.zufar.icedlatte.security.oauth.flow.OAuthFlowService;
import com.zufar.icedlatte.security.session.management.AuthSessionRequestMetadata;
import com.zufar.icedlatte.security.session.management.AuthSessionService;
import com.zufar.icedlatte.security.session.revocation.TokenRevocationService;
import com.zufar.icedlatte.security.session.token.AuthenticationTokens;
import com.zufar.icedlatte.security.session.token.RefreshTokenResult;
import com.zufar.icedlatte.security.session.token.RefreshTokenService;
import com.zufar.icedlatte.security.signin.auth.UserAuthenticationService;
import com.zufar.icedlatte.security.signup.password.PasswordResetService;
import com.zufar.icedlatte.security.signup.registration.UserRegistrationService;
import com.zufar.icedlatte.security.signup.verification.EmailVerificationService;

import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequestMapping(UserSecurityEndpoint.USER_SECURITY_API_URL)
@RequiredArgsConstructor
public class UserSecurityEndpoint implements SecurityApi {

    public static final String USER_SECURITY_API_URL = ApiPaths.AUTH;

    private static final String X_REFRESH_TOKEN_HEADER = "X-Refresh-Token";
    private static final String OAUTH_HANDOFF_CODE_PATTERN = "^[A-Za-z0-9_-]{43}$";
    private static final String UNSUPPORTED_OAUTH_PROVIDER_REASON = "OAuth provider is not supported.";

    private final UserAuthenticationService userAuthenticationService;
    private final EmailVerificationService emailVerificationService;
    private final AuthSessionService authSessionService;
    private final RefreshTokenService refreshTokenService;
    private final TokenRevocationService tokenRevocationService;
    private final PasswordResetService passwordResetService;
    private final CurrentUserProvider currentUserProvider;
    private final UserRegistrationService userRegistrationService;
    private final HttpServletRequest httpRequest;
    private final HttpServletResponse httpResponse;
    private final OAuthFlowService oAuthFlowService;
    private final ClientIpExtractor clientIpExtractor;

    @Value("${email.enabled:false}")
    private boolean emailEnabled;

    @Override
    @GetMapping("/oauth/{provider}")
    public ResponseEntity<Void> initiateOAuth(
            @PathVariable String provider,
            @Valid @RequestParam(required = false) URI redirectUrl) {
        OAuthProvider oAuthProvider = parseProvider(provider);
        String redirectUrlAsString = redirectUrl == null ? null : redirectUrl.toString();
        return oAuthFlowService
                .initiate(oAuthProvider, redirectUrlAsString, httpRequest, httpResponse)
                .map(UserSecurityEndpoint::redirect)
                .orElseGet(() -> ResponseEntity
                        .status(HttpStatus.SERVICE_UNAVAILABLE)
                        .build());
    }

    @Override
    @GetMapping("/oauth/{provider}/callback")
    public ResponseEntity<Void> completeOAuthCallback(
            @PathVariable String provider,
            @Valid @RequestParam(required = false) String code,
            @Valid @RequestParam(required = false) String state) {
        OAuthProvider oAuthProvider = parseProvider(provider);
        URI location = oAuthFlowService.completeCallback(oAuthProvider, code, state, httpRequest, httpResponse);
        return redirect(location);
    }

    @Override
    @PostMapping("/oauth/token")
    public ResponseEntity<UserAuthenticationResponse> completeOAuthTokenHandoff(
            @NotNull @Pattern(regexp = OAUTH_HANDOFF_CODE_PATTERN) @Size(min = 43, max = 43) @RequestParam String code) {
        AuthenticationTokens authenticationTokens = oAuthFlowService.completeTokenHandoff(code);
        return ResponseEntity.ok(toResponse(authenticationTokens));
    }

    @Override
    @PostMapping("/register")
    public ResponseEntity<UserAuthenticationResponse> register(
            @RequestBody @Valid final UserRegistrationRequest request) {
        if (emailEnabled) {
            emailVerificationService.sendEmailVerificationCode(request);
            return ResponseEntity.ok().build();
        }
        AuthenticationTokens authenticationTokens = userRegistrationService.register(request, requestMetadata());
        return ResponseEntity.ok(toResponse(authenticationTokens));
    }

    @Override
    @PostMapping("/confirm")
    public ResponseEntity<UserAuthenticationResponse> confirmEmail(
            @Valid @RequestBody final ConfirmEmailRequest confirmEmailRequest) {
        var response = emailVerificationService.confirmEmailByCode(confirmEmailRequest.getToken(), requestMetadata());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(response));
    }

    @Override
    @PostMapping("/authenticate")
    public ResponseEntity<UserAuthenticationResponse> authenticate(
            @Valid @RequestBody final UserAuthenticationRequest request) {
        AuthenticationTokens authenticationTokens = userAuthenticationService.authenticate(request, requestMetadata());
        return ResponseEntity.ok(toResponse(authenticationTokens));
    }

    @Override
    @PostMapping("/refresh")
    public ResponseEntity<UserAuthenticationResponse> refreshToken() {
        RefreshTokenResult result = refreshTokenService.refresh(httpRequest, requestMetadata());
        return ResponseEntity
                .status(result.migratedLegacyToken() ? HttpStatus.CREATED : HttpStatus.OK)
                .body(toResponse(result.tokens()));
    }

    @Override
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestHeader(name = X_REFRESH_TOKEN_HEADER, required = false) String xRefreshToken) {
        tokenRevocationService.revokeTokens(xRefreshToken, httpRequest);
        return ResponseEntity.ok().build();
    }

    @Override
    @PostMapping("/logout-all")
    public ResponseEntity<Void> logoutAll() {
        authSessionService.revokeAllForUser(currentUserProvider.getUserId());
        return ResponseEntity.ok().build();
    }

    @Override
    @GetMapping("/sessions")
    public ResponseEntity<List<SessionInfo>> getSessions() {
        return ResponseEntity.ok(authSessionService.listActiveSessionInfos(currentUserProvider.getUserId()));
    }

    @Override
    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<Void> revokeSession(@PathVariable UUID sessionId) {
        authSessionService.revokeById(sessionId, currentUserProvider.getUserId());
        return ResponseEntity.noContent().build();
    }

    @Override
    @PostMapping("/password/forgot")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody final ForgotPasswordRequest request) {
        passwordResetService.requestReset(request.getEmail(), request.getTurnstileToken());
        return ResponseEntity.ok().build();
    }

    @Override
    // amazonq-ignore-next-line
    @PostMapping("/password/change")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody final ChangePasswordRequest request) {
        passwordResetService.confirmReset(request.getCode(), request.getPassword(), request.getTurnstileToken());
        return ResponseEntity.ok().build();
    }

    private OAuthProvider parseProvider(String provider) {
        return OAuthProvider
                .fromId(provider)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, UNSUPPORTED_OAUTH_PROVIDER_REASON));
    }

    private AuthSessionRequestMetadata requestMetadata() {
        String userAgent = httpRequest.getHeader(HttpHeaders.USER_AGENT);
        String ipAddress = clientIpExtractor.extract(httpRequest);
        return new AuthSessionRequestMetadata(userAgent, ipAddress);
    }

    private static ResponseEntity<Void> redirect(URI location) {
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(location)
                .build();
    }

    private static UserAuthenticationResponse toResponse(AuthenticationTokens tokens) {
        UserAuthenticationResponse response = new UserAuthenticationResponse();
        response.setToken(tokens.accessToken());
        response.setRefreshToken(tokens.refreshToken());
        return response;
    }
}
