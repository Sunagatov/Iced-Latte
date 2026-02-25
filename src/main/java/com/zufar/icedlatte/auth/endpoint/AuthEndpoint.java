package com.zufar.icedlatte.auth.endpoint;

import com.zufar.icedlatte.auth.api.GoogleAuthCallbackHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthEndpoint {

    @Value("${google.auth.server.url}")
    private String googleAuthServerUrl;

    @Value("${google.client-id}")
    private String clientId;

    @Value("${google.scope}")
    private String scope;

    @Value("${google.redirect-uri}")
    private String redirectUri;

    @Value("${frontend.url}")
    private String frontendUrl;

    private final GoogleAuthCallbackHandler googleAuthCallbackHandler;

    @GetMapping("/google")
    public ResponseEntity<Void> initiateGoogleAuth() {
        log.info("auth.google.initiate");
        URI authUri = UriComponentsBuilder.fromUriString(googleAuthServerUrl)
                .queryParam("scope", scope)
                .queryParam("access_type", "offline")
                .queryParam("response_type", "code")
                .queryParam("redirect_uri", redirectUri)
                .queryParam("client_id", clientId)
                .build().toUri();
        return ResponseEntity.status(HttpStatus.FOUND).location(authUri).build();
    }

    @GetMapping("/google/callback")
    public ResponseEntity<Void> googleCallback(@RequestParam("code") String code) {
        try {
            var tokens = googleAuthCallbackHandler.handle(code);
            URI destination = UriComponentsBuilder.fromUriString(frontendUrl + "/auth/google/callback")
                    .queryParam("token", tokens.getToken())
                    .queryParam("refreshToken", tokens.getRefreshToken())
                    .build().toUri();
            return ResponseEntity.status(HttpStatus.FOUND).location(destination).build();
        } catch (Exception e) {
            log.error("auth.google.callback.failed: message={}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(frontendUrl + "/signin?error=auth_failed"))
                    .build();
        }
    }
}
