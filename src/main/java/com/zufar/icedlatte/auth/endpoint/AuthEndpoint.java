package com.zufar.icedlatte.auth.endpoint;

import com.zufar.icedlatte.auth.api.GoogleAuthCallbackHandler;
import com.zufar.icedlatte.auth.api.OAuthStateStore;
import com.zufar.icedlatte.openapi.dto.UserAuthenticationResponse;
import com.zufar.icedlatte.security.configuration.AuthPaths;
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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Slf4j
@RestController
@RequestMapping(AuthPaths.ROOT)
public class AuthEndpoint {

    private static final String GOOGLE_CALLBACK_PATH = "/auth/google/callback";
    private static final String SIGN_IN_PATH = "/signin";
    private static final String ERROR_QUERY_PARAM = "error";
    private static final String NEXT_QUERY_PARAM = "next";
    private static final String GOOGLE_DISABLED_ERROR = "google_disabled";
    private static final String MISSING_CODE_ERROR = "missing_code";
    private static final String INVALID_STATE_ERROR = "invalid_state";
    private static final String AUTH_FAILED_ERROR = "auth_failed";

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

    private final GoogleAuthCallbackHandler googleAuthCallbackHandler;
    private final OAuthStateStore oAuthStateCache;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public AuthEndpoint(@Autowired(required = false) GoogleAuthCallbackHandler googleAuthCallbackHandler,
                        OAuthStateStore oAuthStateCache) {
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
        String nonce = generateStateNonce();
        oAuthStateCache.store(nonce, callbackBase);
        URI authUri = UriComponentsBuilder.fromUriString(googleAuthServerUrl)
                .queryParam("scope", scope)
                .queryParam("access_type", "offline")
                .queryParam("response_type", "code")
                .queryParam("redirect_uri", redirectUri)
                .queryParam("client_id", clientId)
                .queryParam("state", nonce)
                .build()
                .toUri();
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(authUri)
                .build();
    }

    private String resolveCallbackBase(String redirectUrl) {
        if (redirectUrl == null || redirectUrl.isBlank()) {
            return defaultCallbackBase();
        }
        try {
            URI incoming = new URI(redirectUrl);
            URI allowed = new URI(frontendUrl);
            boolean sameOrigin = allowed.getScheme().equals(incoming.getScheme())
                    && allowed.getHost().equals(incoming.getHost())
                    && allowed.getPort() == incoming.getPort();
            boolean expectedPath = GOOGLE_CALLBACK_PATH.equals(incoming.getPath());
            if (!sameOrigin || !expectedPath) {
            log.info("auth.google.redirect.rejected: reasonCode={}",
                        sameOrigin ? "PATH_MISMATCH" : "ORIGIN_MISMATCH");
                return defaultCallbackBase();
            }
        } catch (URISyntaxException _) {
            log.debug("auth.google.redirect.invalid: reasonCode=INVALID_URI");
            return defaultCallbackBase();
        }
        return redirectUrl;
    }

    private String defaultCallbackBase() {
        return UriComponentsBuilder.fromUriString(frontendUrl)
                .path(GOOGLE_CALLBACK_PATH)
                .build()
                .toUriString();
    }

    @GetMapping("/google/callback")
    public void googleCallback(@RequestParam(required = false) String code,
                               @RequestParam(required = false) String state,
                               HttpServletRequest request,
                               HttpServletResponse response) throws IOException {
        if (googleAuthCallbackHandler == null) {
            redirectToSignInError(response, GOOGLE_DISABLED_ERROR);
            return;
        }
        if (code == null || code.isBlank()) {
            log.debug("auth.google.callback.missing-code");
            redirectToSignInError(response, MISSING_CODE_ERROR);
            return;
        }
        if (state == null || state.isBlank()) {
            log.debug("auth.google.callback.missing-state");
            redirectToSignInError(response, INVALID_STATE_ERROR);
            return;
        }
        String stored = oAuthStateCache.consume(state);
        if (stored == null) {
            log.info("auth.google.callback.invalid-state");
            redirectToSignInError(response, INVALID_STATE_ERROR);
            return;
        }
        try {
            var tokens = googleAuthCallbackHandler.handle(code, request);
            response.sendRedirect(buildCallbackUrlWithFragmentTokens(stored, tokens));
        } catch (Exception e) {
            log.error("auth.google.callback.failed: exceptionClass={}, reasonCode=CALLBACK_FAILURE",
                    e.getClass().getSimpleName(), e);
            response.sendRedirect(buildFrontendErrorRedirect(stored, frontendUrl));
        }
    }

    private static String buildCallbackUrlWithFragmentTokens(String callbackBase,
                                                             UserAuthenticationResponse tokens) {
        return callbackBase
                + "#token=" + urlEncode(tokens.getToken())
                + "&refreshToken=" + urlEncode(tokens.getRefreshToken());
    }

    private static String generateStateNonce() {
        byte[] nonceBytes = new byte[16];
        SECURE_RANDOM.nextBytes(nonceBytes);
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(nonceBytes);
    }

    private void redirectToSignInError(HttpServletResponse response,
                                       String errorCode) throws IOException {
        response.sendRedirect(buildSignInErrorRedirect(frontendUrl, errorCode));
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String buildSignInErrorRedirect(String frontendBaseUrl,
                                                   String errorCode) {
        return UriComponentsBuilder.fromUriString(frontendBaseUrl)
                .path(SIGN_IN_PATH)
                .queryParam(ERROR_QUERY_PARAM, errorCode)
                .build(true)
                .toUriString();
    }

    private static String buildFrontendErrorRedirect(String callbackBase,
                                                     String frontendBaseUrl) {
        try {
            URI callbackUri = new URI(callbackBase);
            URI frontendUri = new URI(frontendBaseUrl);
            UriComponentsBuilder redirectBuilder = UriComponentsBuilder
                    .fromUri(frontendUri)
                    .path(SIGN_IN_PATH)
                    .queryParam(ERROR_QUERY_PARAM, AUTH_FAILED_ERROR);
            String next = UriComponentsBuilder.fromUri(callbackUri)
                    .build()
                    .getQueryParams()
                    .getFirst(NEXT_QUERY_PARAM);
            if (next != null && !next.isBlank()) {
                redirectBuilder.queryParam(NEXT_QUERY_PARAM, next);
            }
            return redirectBuilder.build(true).toUriString();
        } catch (URISyntaxException e) {
            return buildSignInErrorRedirect(frontendBaseUrl, AUTH_FAILED_ERROR);
        }
    }
}
