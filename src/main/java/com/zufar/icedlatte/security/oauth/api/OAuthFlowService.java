package com.zufar.icedlatte.security.oauth.api;

import com.zufar.icedlatte.openapi.dto.UserAuthenticationResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OAuthFlowService {

    private static final String SIGN_IN_PATH = "/signin";
    private static final String ERROR_QUERY_PARAM = "error";
    private static final String NEXT_QUERY_PARAM = "next";
    private static final String PROVIDER_DISABLED_ERROR = "oauth_disabled";
    private static final String MISSING_CODE_ERROR = "missing_code";
    private static final String INVALID_STATE_ERROR = "invalid_state";
    private static final String AUTH_FAILED_ERROR = "auth_failed";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Value("${frontend.url}")
    private String frontendUrl;

    private final OAuthLoginService oAuthLoginService;
    private final OAuthStateStore oAuthStateStore;

    public Optional<URI> initiate(OAuthProvider provider,
                                  String redirectUrl) {
        var client = oAuthLoginService.findClient(provider);
        if (client.isEmpty()) {
            log.warn("auth.oauth.disabled: provider={}", provider.id());
            return Optional.empty();
        }
        log.info("auth.oauth.initiate: provider={}", provider.id());
        String callbackBase = resolveCallbackBase(provider, redirectUrl);
        String nonce = generateStateNonce();
        oAuthStateStore.store(provider, nonce, callbackBase);
        return Optional.of(client.get().buildAuthorizationUri(nonce));
    }

    public URI completeCallback(OAuthProvider provider,
                                String code,
                                String state,
                                HttpServletRequest request) {
        if (oAuthLoginService.findClient(provider).isEmpty()) {
            return buildSignInErrorRedirect(PROVIDER_DISABLED_ERROR);
        }
        if (code == null || code.isBlank()) {
            log.debug("auth.oauth.callback.missing-code: provider={}", provider.id());
            return buildSignInErrorRedirect(MISSING_CODE_ERROR);
        }
        if (state == null || state.isBlank()) {
            log.debug("auth.oauth.callback.missing-state: provider={}", provider.id());
            return buildSignInErrorRedirect(INVALID_STATE_ERROR);
        }
        String callbackBase = oAuthStateStore.consume(provider, state);
        if (callbackBase == null) {
            log.info("auth.oauth.callback.invalid-state: provider={}", provider.id());
            return buildSignInErrorRedirect(INVALID_STATE_ERROR);
        }
        try {
            UserAuthenticationResponse tokens = oAuthLoginService.handle(provider, code, request);
            return URI.create(buildCallbackUrlWithFragmentTokens(callbackBase, tokens));
        } catch (Exception e) {
            log.error("auth.oauth.callback.failed: provider={}, exceptionClass={}, reasonCode=CALLBACK_FAILURE",
                    provider.id(), e.getClass().getSimpleName(), e);
            return buildFrontendErrorRedirect(callbackBase);
        }
    }

    private String resolveCallbackBase(OAuthProvider provider,
                                       String redirectUrl) {
        if (redirectUrl == null || redirectUrl.isBlank()) {
            return defaultCallbackBase(provider);
        }
        try {
            URI incoming = new URI(redirectUrl);
            URI allowed = new URI(frontendUrl);
            boolean sameOrigin = allowed.getScheme().equalsIgnoreCase(incoming.getScheme())
                    && allowed.getHost().equalsIgnoreCase(incoming.getHost())
                    && effectivePort(allowed) == effectivePort(incoming);
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

    private static int effectivePort(URI uri) {
        if (uri.getPort() != -1) {
            return uri.getPort();
        }
        return switch (uri.getScheme().toLowerCase()) {
            case "http" -> 80;
            case "https" -> 443;
            default -> -1;
        };
    }

    private String defaultCallbackBase(OAuthProvider provider) {
        return UriComponentsBuilder.fromUriString(frontendUrl)
                .path(provider.callbackPath())
                .build()
                .toUriString();
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

    private static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private URI buildSignInErrorRedirect(String errorCode) {
        return URI.create(UriComponentsBuilder.fromUriString(frontendUrl)
                .path(SIGN_IN_PATH)
                .queryParam(ERROR_QUERY_PARAM, errorCode)
                .build(true)
                .toUriString());
    }

    private URI buildFrontendErrorRedirect(String callbackBase) {
        try {
            URI callbackUri = new URI(callbackBase);
            URI frontendUri = new URI(frontendUrl);
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
            return URI.create(redirectBuilder.build(true).toUriString());
        } catch (URISyntaxException _) {
            return buildSignInErrorRedirect(AUTH_FAILED_ERROR);
        }
    }
}
