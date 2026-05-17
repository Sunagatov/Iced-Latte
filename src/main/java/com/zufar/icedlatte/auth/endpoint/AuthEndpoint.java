package com.zufar.icedlatte.auth.endpoint;

import com.zufar.icedlatte.auth.api.OAuthLoginService;
import com.zufar.icedlatte.auth.api.OAuthProvider;
import com.zufar.icedlatte.auth.api.OAuthStateStore;
import com.zufar.icedlatte.openapi.dto.UserAuthenticationResponse;
import com.zufar.icedlatte.security.configuration.AuthPaths;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping(AuthPaths.ROOT)
public class AuthEndpoint {

    private static final String SIGN_IN_PATH = "/signin";
    private static final String ERROR_QUERY_PARAM = "error";
    private static final String NEXT_QUERY_PARAM = "next";
    private static final String PROVIDER_DISABLED_ERROR = "oauth_disabled";
    private static final String MISSING_CODE_ERROR = "missing_code";
    private static final String INVALID_STATE_ERROR = "invalid_state";
    private static final String AUTH_FAILED_ERROR = "auth_failed";

    @Value("${frontend.url}")
    private String frontendUrl;

    private final OAuthLoginService oAuthLoginService;
    private final OAuthStateStore oAuthStateCache;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public AuthEndpoint(OAuthLoginService oAuthLoginService,
                        OAuthStateStore oAuthStateCache) {
        this.oAuthLoginService = oAuthLoginService;
        this.oAuthStateCache = oAuthStateCache;
    }

    @GetMapping("/google")
    public ResponseEntity<?> initiateGoogleAuth(@RequestParam(required = false) String redirectUrl) {
        return initiateOAuth("google", redirectUrl);
    }

    @GetMapping("/oauth/{provider}")
    public ResponseEntity<?> initiateOAuth(@PathVariable String provider,
                                           @RequestParam(required = false) String redirectUrl) {
        OAuthProvider oAuthProvider = parseProvider(provider);
        var client = oAuthLoginService.findClient(oAuthProvider);
        if (client.isEmpty()) {
            log.warn("auth.oauth.disabled: provider={}", oAuthProvider.id());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("message", "OAuth provider is not available.", "status", 503));
        }
        log.info("auth.oauth.initiate: provider={}", oAuthProvider.id());
        String callbackBase = resolveCallbackBase(oAuthProvider, redirectUrl);
        String nonce = generateStateNonce();
        oAuthStateCache.store(nonce, callbackBase);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(client.get().buildAuthorizationUri(nonce))
                .build();
    }

    private String resolveCallbackBase(OAuthProvider provider,
                                       String redirectUrl) {
        if (redirectUrl == null || redirectUrl.isBlank()) {
            return defaultCallbackBase(provider);
        }
        try {
            URI incoming = new URI(redirectUrl);
            URI allowed = new URI(frontendUrl);
            boolean sameOrigin = allowed.getScheme().equals(incoming.getScheme())
                    && allowed.getHost().equals(incoming.getHost())
                    && allowed.getPort() == incoming.getPort();
            boolean expectedPath = provider.callbackPath().equals(incoming.getPath());
            if (!sameOrigin || !expectedPath) {
                log.info("auth.oauth.redirect.rejected: provider={}, reasonCode={}", provider.id(),
                        sameOrigin ? "PATH_MISMATCH" : "ORIGIN_MISMATCH");
                return defaultCallbackBase(provider);
            }
        } catch (URISyntaxException _) {
            log.debug("auth.oauth.redirect.invalid: provider={}, reasonCode=INVALID_URI", provider.id());
            return defaultCallbackBase(provider);
        }
        return redirectUrl;
    }

    private String defaultCallbackBase(OAuthProvider provider) {
        return UriComponentsBuilder.fromUriString(frontendUrl)
                .path(provider.callbackPath())
                .build()
                .toUriString();
    }

    @GetMapping("/google/callback")
    public void googleCallback(@RequestParam(required = false) String code,
                               @RequestParam(required = false) String state,
                               HttpServletRequest request,
                               HttpServletResponse response) throws IOException {
        oAuthCallback("google", code, state, request, response);
    }

    @GetMapping("/oauth/{provider}/callback")
    public void oAuthCallback(@PathVariable String provider,
                              @RequestParam(required = false) String code,
                              @RequestParam(required = false) String state,
                              HttpServletRequest request,
                              HttpServletResponse response) throws IOException {
        OAuthProvider oAuthProvider = parseProvider(provider);
        if (oAuthLoginService.findClient(oAuthProvider).isEmpty()) {
            redirectToSignInError(response, PROVIDER_DISABLED_ERROR);
            return;
        }
        if (code == null || code.isBlank()) {
            log.debug("auth.oauth.callback.missing-code: provider={}", oAuthProvider.id());
            redirectToSignInError(response, MISSING_CODE_ERROR);
            return;
        }
        if (state == null || state.isBlank()) {
            log.debug("auth.oauth.callback.missing-state: provider={}", oAuthProvider.id());
            redirectToSignInError(response, INVALID_STATE_ERROR);
            return;
        }
        String stored = oAuthStateCache.consume(state);
        if (stored == null) {
            log.info("auth.oauth.callback.invalid-state: provider={}", oAuthProvider.id());
            redirectToSignInError(response, INVALID_STATE_ERROR);
            return;
        }
        try {
            var tokens = oAuthLoginService.handle(oAuthProvider, code, request);
            response.sendRedirect(buildCallbackUrlWithFragmentTokens(stored, tokens));
        } catch (Exception e) {
            log.error("auth.oauth.callback.failed: provider={}, exceptionClass={}, reasonCode=CALLBACK_FAILURE",
                    oAuthProvider.id(), e.getClass().getSimpleName(), e);
            response.sendRedirect(buildFrontendErrorRedirect(stored, frontendUrl));
        }
    }

    private OAuthProvider parseProvider(String provider) {
        return OAuthProvider.fromId(provider)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "OAuth provider is not supported."));
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
