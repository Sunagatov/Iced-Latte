package com.zufar.icedlatte.auth.endpoint;

import com.zufar.icedlatte.auth.api.AuthorizationServerUrlCreator;
import com.zufar.icedlatte.auth.api.GoogleAuthCallbackHandler;
import com.zufar.icedlatte.openapi.dto.UserAuthenticationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
@Slf4j
@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/api/v1/auth")
public class AuthEndpoint {

    private static final String AUTH_CODE_PATTERN = "^[a-zA-Z0-9_-]+$";
    private static final int MIN_CODE_LENGTH = 10;
    private static final int MAX_CODE_LENGTH = 512;

    private final GoogleAuthCallbackHandler googleAuthCallbackHandler;
    private final AuthorizationServerUrlCreator authorizationServerUrlCreator;

    @GetMapping("/google")
    public ResponseEntity<String> getGoogleAuthorizationServerUrl() {
        log.info("Initiating Google authentication");
        var authorizationUrl = authorizationServerUrlCreator.create();
        log.info("Google authentication URL created successfully");
        
        return ResponseEntity.status(HttpStatus.TEMPORARY_REDIRECT)
                .header(HttpHeaders.LOCATION, authorizationUrl)
                .build();
    }

    @GetMapping("/google/callback")
    public CompletableFuture<ResponseEntity<UserAuthenticationResponse>> googleAuthCallback(
            @RequestParam("code") String authorizationCode) {
        
        log.info("Processing Google authentication callback");
        
        return validateAuthorizationCode(authorizationCode)
                .map(this::processValidCode)
                .orElseGet(() -> CompletableFuture.completedFuture(
                    ResponseEntity.badRequest().build()));
    }

    private Optional<String> validateAuthorizationCode(String code) {
        return Optional.ofNullable(code)
                .map(String::trim)
                .filter(Predicate.not(String::isEmpty))
                .filter(c -> c.matches(AUTH_CODE_PATTERN))
                .filter(c -> c.length() >= MIN_CODE_LENGTH && c.length() <= MAX_CODE_LENGTH)
                .or(() -> {
                    log.warn("Invalid authorization code format");
                    return Optional.empty();
                });
    }

    private CompletableFuture<ResponseEntity<UserAuthenticationResponse>> processValidCode(String code) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                var response = googleAuthCallbackHandler.googleAuthCallback(code);
                log.info("Google authentication completed successfully");
                
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(response);
                        
            } catch (Exception e) {
                log.error("Google authentication failed", e);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
        });
    }
}
