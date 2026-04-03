package com.zufar.icedlatte.auth.endpoint;

import com.zufar.icedlatte.auth.api.GoogleAuthCallbackHandler;
import com.zufar.icedlatte.auth.api.OAuthStateCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.SecureRandom;
import java.util.Base64;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
public class AuthEndpoint {

    @Value("${google.auth.server.url:}")
    private String googleAuthServerUrl;

    @Value("${google.client-id:}")
    private String clientId;

    @Value("${google.scope:}")
    private String scope;

    @Value("${google.redirect-uri:}")
    private String redirectUri;

    @Value("${frontend.url}")
    private String frontendUrl;

    @Autowired(required = false)
    private GoogleAuthCallbackHandler googleAuthCallbackHandler;

    @Autowired
    private OAuthStateCache oAuthStateCache;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @GetMapping("/google")
    public ResponseEntity<Void> initiateGoogleAuth(@RequestParam(required = false) String redirectUrl) {
        if (googleAuthCallbackHandler == null) {
            log.warn("auth.google.disabled");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
        log.info("auth.google.initiate");
        String callbackBase = resolveCallbackBase(redirectUrl);
        byte[] nonceBytes = new byte[16];
        SECURE_RANDOM.nextBytes(nonceBytes);
        String nonce = Base64.getUrlEncoder().withoutPadding().encodeToString(nonceBytes);
        oAuthStateCache.store(nonce, callbackBase);
        URI authUri = UriComponentsBuilder.fromUriString(googleAuthServerUrl)
                .queryParam("scope", scope)
                .queryParam("access_type", "offline")
                .queryParam("response_type", "code")
                .queryParam("redirect_uri", redirectUri)
                .queryParam("client_id", clientId)
                .queryParam("state", nonce)
                .build().toUri();
        return ResponseEntity.status(HttpStatus.FOUND).location(authUri).build();
    }

    private String resolveCallbackBase(String redirectUrl) {
        if (redirectUrl == null || redirectUrl.isBlank()) {
            return frontendUrl;
        }
        try {
            URI incoming = new URI(redirectUrl);
            URI allowed = new URI(frontendUrl);
            boolean sameOrigin = incoming.getScheme().equals(allowed.getScheme())
                    && incoming.getHost().equals(allowed.getHost())
                    && incoming.getPort() == allowed.getPort();
            if (!sameOrigin) {
                log.warn("auth.google.redirect.rejected: redirectUrl={}", redirectUrl);
                return frontendUrl;
            }
        } catch (URISyntaxException e) {
            log.warn("auth.google.redirect.invalid: redirectUrl={}", redirectUrl);
            return frontendUrl;
        }
        return redirectUrl;
    }

    @GetMapping("/google/callback")
    public ResponseEntity<Void> googleCallback(@RequestParam("code") String code,
                                               @RequestParam(required = false) String state) {
        if (googleAuthCallbackHandler == null) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
        if (state == null || state.isBlank()) {
            log.warn("auth.google.callback.missing-state");
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(frontendUrl + "/signin?error=invalid_state"))
                    .build();
        }
        String stored = oAuthStateCache.consume(state);
        if (stored == null) {
            log.warn("auth.google.callback.invalid-state");
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(frontendUrl + "/signin?error=invalid_state"))
                    .build();
        }
        try {
            var tokens = googleAuthCallbackHandler.handle(code);
            URI destination = UriComponentsBuilder.fromUriString(stored + "/auth/google/callback")
                    .queryParam("token", tokens.getToken())
                    .queryParam("refreshToken", tokens.getRefreshToken())
                    .build().toUri();
            return ResponseEntity.status(HttpStatus.FOUND).location(destination).build();
        } catch (Exception e) {
            log.error("auth.google.callback.failed: message={}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(stored + "/signin?error=auth_failed"))
                    .build();
        }
    }
}
