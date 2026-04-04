package com.zufar.icedlatte.auth.endpoint;

import com.zufar.icedlatte.auth.api.GoogleAuthCallbackHandler;
import com.zufar.icedlatte.auth.api.OAuthStateCache;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.SecureRandom;
import java.time.Duration;
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

    @Value("${server.ssl.enabled:false}")
    private boolean sslEnabled;

    private final GoogleAuthCallbackHandler googleAuthCallbackHandler;
    private final OAuthStateCache oAuthStateCache;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public AuthEndpoint(@Autowired(required = false) GoogleAuthCallbackHandler googleAuthCallbackHandler,
                        OAuthStateCache oAuthStateCache) {
        this.googleAuthCallbackHandler = googleAuthCallbackHandler;
        this.oAuthStateCache = oAuthStateCache;
    }

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
            boolean sameOrigin = allowed.getScheme().equals(incoming.getScheme())
                    && allowed.getHost().equals(incoming.getHost())
                    && allowed.getPort() == incoming.getPort();
            if (!sameOrigin) {
                log.warn("auth.google.redirect.rejected: reasonCode=ORIGIN_MISMATCH");
                return frontendUrl;
            }
        } catch (URISyntaxException _) {
            log.warn("auth.google.redirect.invalid: reasonCode=INVALID_URI");
            return frontendUrl;
        }
        return redirectUrl;
    }

    @GetMapping("/google/callback")
    public void googleCallback(@RequestParam("code") String code,
                               @RequestParam(required = false) String state,
                               HttpServletRequest request,
                               HttpServletResponse response) throws IOException {
        if (googleAuthCallbackHandler == null) {
            response.sendRedirect(frontendUrl + "/signin?error=google_disabled");
            return;
        }
        if (state == null || state.isBlank()) {
            log.warn("auth.google.callback.missing-state");
            response.sendRedirect(frontendUrl + "/signin?error=invalid_state");
            return;
        }
        String stored = oAuthStateCache.consume(state);
        if (stored == null) {
            log.warn("auth.google.callback.invalid-state");
            response.sendRedirect(frontendUrl + "/signin?error=invalid_state");
            return;
        }
        try {
            var tokens = googleAuthCallbackHandler.handle(code, request);

            Cookie tokenCookie = new Cookie("token", tokens.getToken());
            tokenCookie.setHttpOnly(true);
            tokenCookie.setSecure(sslEnabled || request.isSecure());
            tokenCookie.setPath("/");
            tokenCookie.setMaxAge((int) Duration.ofDays(1).toSeconds());
            tokenCookie.setAttribute("SameSite", "Lax");
            response.addCookie(tokenCookie);

            response.sendRedirect(stored + "/?auth=success");
        } catch (Exception e) {
            log.warn("auth.google.callback.failed: exceptionClass={}, reasonCode=CALLBACK_FAILURE, message={}",
                    e.getClass().getSimpleName(), e.getMessage());
            response.sendRedirect(stored + "/signin?error=auth_failed");
        }
    }
}
