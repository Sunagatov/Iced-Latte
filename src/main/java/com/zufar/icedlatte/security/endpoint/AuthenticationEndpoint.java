package com.zufar.icedlatte.security.endpoint;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.zufar.icedlatte.common.util.ClientIpExtractor;
import com.zufar.icedlatte.openapi.dto.ConfirmEmailRequest;
import com.zufar.icedlatte.openapi.dto.UserAuthenticationRequest;
import com.zufar.icedlatte.openapi.dto.UserAuthenticationResponse;
import com.zufar.icedlatte.openapi.dto.UserRegistrationRequest;
import com.zufar.icedlatte.openapi.security.api.AuthenticationApi;
import com.zufar.icedlatte.security.session.management.AuthSessionRequestMetadata;
import com.zufar.icedlatte.security.session.token.AuthenticationTokens;
import com.zufar.icedlatte.security.signin.auth.UserAuthenticationService;
import com.zufar.icedlatte.security.signup.registration.UserRegistrationService;
import com.zufar.icedlatte.security.signup.verification.EmailVerificationService;

import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequiredArgsConstructor
public class AuthenticationEndpoint implements AuthenticationApi {

    private final UserAuthenticationService userAuthenticationService;
    private final EmailVerificationService emailVerificationService;
    private final UserRegistrationService userRegistrationService;
    private final HttpServletRequest httpRequest;
    private final ClientIpExtractor clientIpExtractor;

    @Value("${email.enabled:false}")
    private boolean emailEnabled;

    @Override
    @PostMapping("/api/v1/auth/register")
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
    @PostMapping("/api/v1/auth/confirm")
    public ResponseEntity<UserAuthenticationResponse> confirmEmail(
            @Valid @RequestBody final ConfirmEmailRequest confirmEmailRequest) {
        var response = emailVerificationService.confirmEmailByCode(confirmEmailRequest.getToken(), requestMetadata());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(response));
    }

    @Override
    @PostMapping("/api/v1/auth/authenticate")
    public ResponseEntity<UserAuthenticationResponse> authenticate(
            @Valid @RequestBody final UserAuthenticationRequest request) {
        AuthenticationTokens authenticationTokens = userAuthenticationService.authenticate(request, requestMetadata());
        return ResponseEntity.ok(toResponse(authenticationTokens));
    }

    private AuthSessionRequestMetadata requestMetadata() {
        String userAgent = httpRequest.getHeader(HttpHeaders.USER_AGENT);
        String ipAddress = clientIpExtractor.extract(httpRequest);
        return new AuthSessionRequestMetadata(userAgent, ipAddress);
    }

    private static UserAuthenticationResponse toResponse(AuthenticationTokens tokens) {
        UserAuthenticationResponse response = new UserAuthenticationResponse();
        response.setToken(tokens.accessToken());
        response.setRefreshToken(tokens.refreshToken());
        return response;
    }
}
