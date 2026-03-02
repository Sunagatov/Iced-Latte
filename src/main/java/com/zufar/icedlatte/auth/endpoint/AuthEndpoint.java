package com.zufar.icedlatte.auth.endpoint;

import com.zufar.icedlatte.auth.api.GoogleAuthCallbackHandler;
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
import java.util.Base64;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

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

    private final Optional<GoogleAuthCallbackHandler> googleAuthCallbackHandler;

    @Autowired
    public AuthEndpoint(Optional<GoogleAuthCallbackHandler> googleAuthCallbackHandler) {
        this.googleAuthCallbackHandler = googleAuthCallbackHandler;
    }

    @GetMapping("/google")
    public ResponseEntity<Void> initiateGoogleAuth(@RequestParam(required = false) String redirectUrl) {
        if (googleAuthCallbackHandler.isEmpty()) {
            log.warn("auth.google.disabled");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
        log.info("auth.google.initiate");
        String callbackBase = redirectUrl != null && !redirectUrl.isBlank() ? redirectUrl : frontendUrl;
        String state = Base64.getUrlEncoder().encodeToString(callbackBase.getBytes(StandardCharsets.UTF_8));
        URI authUri = UriComponentsBuilder.fromUriString(googleAuthServerUrl)
                .queryParam("scope", scope)
                .queryParam("access_type", "offline")
                .queryParam("response_type", "code")
                .queryParam("redirect_uri", redirectUri)
                .queryParam("client_id", clientId)
                .queryParam("state", state)
                .build().toUri();
        return ResponseEntity.status(HttpStatus.FOUND).location(authUri).build();
    }

    @GetMapping("/google/callback")
    public ResponseEntity<Void> googleCallback(@RequestParam("code") String code,
                                               @RequestParam(required = false) String state) {
        if (googleAuthCallbackHandler.isEmpty()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
        String callbackBase = frontendUrl;
        if (state != null && !state.isBlank()) {
            try {
                callbackBase = new String(Base64.getUrlDecoder().decode(state), StandardCharsets.UTF_8);
            } catch (Exception e) {
                log.warn("auth.google.callback.invalid-state");
            }
        }
        try {
            var tokens = googleAuthCallbackHandler.get().handle(code);
            URI destination = UriComponentsBuilder.fromUriString(callbackBase + "/auth/google/callback")
                    .queryParam("token", tokens.getToken())
                    .queryParam("refreshToken", tokens.getRefreshToken())
                    .build().toUri();
            return ResponseEntity.status(HttpStatus.FOUND).location(destination).build();
        } catch (Exception e) {
            log.error("auth.google.callback.failed: message={}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(callbackBase + "/signin?error=auth_failed"))
                    .build();
        }
    }
}
