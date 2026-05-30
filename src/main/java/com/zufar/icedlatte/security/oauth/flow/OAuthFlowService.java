package com.zufar.icedlatte.security.oauth.flow;

import java.net.URI;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.zufar.icedlatte.common.exception.BadRequestException;
import com.zufar.icedlatte.common.exception.UnauthorizedException;
import com.zufar.icedlatte.security.oauth.config.OAuthProvider;
import com.zufar.icedlatte.security.oauth.login.OAuthLoginService;
import com.zufar.icedlatte.security.session.token.AuthenticationTokens;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class OAuthFlowService {

    private static final String PROVIDER_DISABLED_ERROR = "oauth_disabled";
    private static final String MISSING_CODE_ERROR = "missing_code";
    private static final String INVALID_STATE_ERROR = "invalid_state";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final OAuthLoginService oAuthLoginService;
    private final OAuthStateStore oAuthStateStore;
    private final OAuthTokenHandoffStore oAuthTokenHandoffStore;
    private final OAuthRedirectService oAuthRedirectService;

    public Optional<URI> initiate(OAuthProvider provider, String redirectUrl) {
        var client = oAuthLoginService.findClient(provider);
        if (client.isEmpty()) {
            log.warn("auth.oauth.disabled: provider={}", provider.id());
            return Optional.empty();
        }
        log.info("auth.oauth.initiate: provider={}", provider.id());
        String callbackBase = oAuthRedirectService.resolveCallbackBase(provider, redirectUrl);
        String nonce = generateStateNonce();
        oAuthStateStore.store(provider, nonce, callbackBase);
        return Optional.of(client.get().buildAuthorizationUri(nonce));
    }

    public URI completeCallback(OAuthProvider provider, String code, String state, HttpServletRequest request) {
        if (oAuthLoginService.findClient(provider).isEmpty()) {
            return oAuthRedirectService.signInErrorRedirect(PROVIDER_DISABLED_ERROR);
        }
        if (code == null || code.isBlank()) {
            log.debug("auth.oauth.callback.missing-code: provider={}", provider.id());
            return oAuthRedirectService.signInErrorRedirect(MISSING_CODE_ERROR);
        }
        if (state == null || state.isBlank()) {
            log.debug("auth.oauth.callback.missing-state: provider={}", provider.id());
            return oAuthRedirectService.signInErrorRedirect(INVALID_STATE_ERROR);
        }
        String callbackBase = oAuthStateStore.consume(provider, state);
        if (callbackBase == null) {
            log.info("auth.oauth.callback.invalid-state: provider={}", provider.id());
            return oAuthRedirectService.signInErrorRedirect(INVALID_STATE_ERROR);
        }
        try {
            AuthenticationTokens tokens = oAuthLoginService.handle(provider, code, request);
            String handoffCode = oAuthTokenHandoffStore.store(tokens);
            return URI.create(oAuthRedirectService.callbackUrlWithHandoffCode(callbackBase, handoffCode));
        } catch (BadRequestException | UnauthorizedException e) {
            log.error(
                    "auth.oauth.callback.failed: provider={}, exceptionClass={}, reasonCode=CALLBACK_FAILURE",
                    provider.id(),
                    e.getClass().getSimpleName(),
                    e);
            return oAuthRedirectService.frontendErrorRedirect(callbackBase);
        }
    }

    public AuthenticationTokens completeTokenHandoff(String code) {
        if (!StringUtils.hasText(code)) {
            throw new BadRequestException("OAuth token code is required.");
        }
        return oAuthTokenHandoffStore
                .consume(code)
                .orElseThrow(() -> new BadRequestException("OAuth token code is invalid or expired."));
    }

    private static String generateStateNonce() {
        byte[] nonceBytes = new byte[16];
        SECURE_RANDOM.nextBytes(nonceBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(nonceBytes);
    }
}
