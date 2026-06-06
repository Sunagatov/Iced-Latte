package com.zufar.icedlatte.security.endpoint;

import java.net.URI;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.zufar.icedlatte.openapi.dto.UserAuthenticationResponse;
import com.zufar.icedlatte.openapi.security.api.OAuthApi;
import com.zufar.icedlatte.security.oauth.config.OAuthProvider;
import com.zufar.icedlatte.security.oauth.flow.OAuthFlowService;
import com.zufar.icedlatte.security.session.token.AuthenticationTokens;

import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequiredArgsConstructor
public class OAuthEndpoint implements OAuthApi {

    private static final String UNSUPPORTED_OAUTH_PROVIDER_REASON = "OAuth provider is not supported.";

    private final OAuthFlowService oAuthFlowService;
    private final HttpServletRequest httpRequest;
    private final HttpServletResponse httpResponse;

    @Override
    @GetMapping("/api/v1/auth/oauth/{provider}")
    public ResponseEntity<Void> initiateOAuth(
            @PathVariable String provider,
            @Valid @RequestParam(required = false) URI redirectUrl) {
        OAuthProvider oAuthProvider = parseProvider(provider);
        String redirectUrlAsString = redirectUrl == null ? null : redirectUrl.toString();
        return oAuthFlowService
                .initiate(oAuthProvider, redirectUrlAsString, httpRequest, httpResponse)
                .map(OAuthEndpoint::redirect)
                .orElseGet(() -> ResponseEntity
                        .status(HttpStatus.SERVICE_UNAVAILABLE)
                        .build());
    }

    @Override
    @GetMapping("/api/v1/auth/oauth/{provider}/callback")
    public ResponseEntity<Void> completeOAuthCallback(
            @PathVariable String provider,
            @Valid @RequestParam(required = false) String code,
            @Valid @RequestParam(required = false) String state) {
        OAuthProvider oAuthProvider = parseProvider(provider);
        URI location = oAuthFlowService.completeCallback(oAuthProvider, code, state, httpRequest, httpResponse);
        return redirect(location);
    }

    @Override
    @PostMapping("/api/v1/auth/oauth/token")
    public ResponseEntity<UserAuthenticationResponse> completeOAuthTokenHandoff(@RequestParam String code) {
        AuthenticationTokens authenticationTokens = oAuthFlowService.completeTokenHandoff(code);
        return ResponseEntity.ok(toResponse(authenticationTokens));
    }

    private OAuthProvider parseProvider(String provider) {
        return OAuthProvider
                .fromId(provider)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, UNSUPPORTED_OAUTH_PROVIDER_REASON));
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
