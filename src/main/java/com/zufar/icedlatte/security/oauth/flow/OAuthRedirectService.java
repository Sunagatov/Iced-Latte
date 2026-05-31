package com.zufar.icedlatte.security.oauth.flow;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import com.zufar.icedlatte.security.oauth.config.OAuthProvider;

@Service
public class OAuthRedirectService {

    private static final String SIGN_IN_PATH = "/signin";
    private static final String ERROR_QUERY_PARAM = "error";
    private static final String NEXT_QUERY_PARAM = "next";
    private static final String AUTH_FAILED_ERROR = "auth_failed";

    private final URI frontendUri;

    public OAuthRedirectService(@Value("${frontend.url}") String frontendUrl) {
        this.frontendUri = parseFrontendUri(frontendUrl);
    }

    String resolveCallbackBase(OAuthProvider provider, String redirectUrl) {
        if (redirectUrl == null || redirectUrl.isBlank()) {
            return defaultCallbackBase(provider);
        }
        try {
            URI incoming = new URI(redirectUrl);
            boolean sameOrigin = frontendUri.getScheme().equalsIgnoreCase(incoming.getScheme())
                    && frontendUri.getHost().equalsIgnoreCase(incoming.getHost())
                    && effectivePort(frontendUri) == effectivePort(incoming);
            boolean expectedPath = provider.callbackPath().equals(incoming.getPath());
            if (!sameOrigin || !expectedPath) {
                return defaultCallbackBase(provider);
            }
            return allowedCallbackBase(incoming);
        } catch (URISyntaxException _) {
            return defaultCallbackBase(provider);
        }
    }

    URI signInErrorRedirect(String errorCode) {
        return URI.create(UriComponentsBuilder.fromUri(frontendUri)
                .path(SIGN_IN_PATH)
                .queryParam(ERROR_QUERY_PARAM, errorCode)
                .build(true)
                .toUriString());
    }

    URI frontendErrorRedirect(String callbackBase) {
        try {
            URI callbackUri = new URI(callbackBase);
            UriComponentsBuilder redirectBuilder = UriComponentsBuilder.fromUri(frontendUri)
                    .path(SIGN_IN_PATH)
                    .queryParam(ERROR_QUERY_PARAM, AUTH_FAILED_ERROR);
            String next = UriComponentsBuilder.fromUri(callbackUri)
                    .build()
                    .getQueryParams()
                    .getFirst(NEXT_QUERY_PARAM);
            if (isSafeRelativePath(next)) {
                redirectBuilder.queryParam(NEXT_QUERY_PARAM, next);
            }
            return URI.create(redirectBuilder.build(true).toUriString());
        } catch (URISyntaxException _) {
            return signInErrorRedirect(AUTH_FAILED_ERROR);
        }
    }

    String callbackUrlWithHandoffCode(String callbackBase, String handoffCode) {
        String encodedCode = URLEncoder.encode(handoffCode, StandardCharsets.UTF_8);
        try {
            return UriComponentsBuilder.fromUri(new URI(callbackBase))
                    .fragment("oauthCode=" + encodedCode)
                    .build(true)
                    .toUriString();
        } catch (URISyntaxException _) {
            return callbackBase.split("#", 2)[0] + "#oauthCode=" + encodedCode;
        }
    }

    private String defaultCallbackBase(OAuthProvider provider) {
        return UriComponentsBuilder.fromUri(frontendUri)
                .path(provider.callbackPath())
                .build()
                .toUriString();
    }

    private static URI parseFrontendUri(String frontendUrl) {
        if (!StringUtils.hasText(frontendUrl)) {
            throw new IllegalArgumentException("frontend.url must not be blank");
        }
        URI uri;
        try {
            uri = URI.create(frontendUrl);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("frontend.url must be a valid URI", e);
        }
        if (!StringUtils.hasText(uri.getScheme()) || !StringUtils.hasText(uri.getHost())) {
            throw new IllegalArgumentException("frontend.url must be an absolute URI with scheme and host");
        }
        String scheme = uri.getScheme().toLowerCase();
        if (!"http".equals(scheme) && !"https".equals(scheme)) {
            throw new IllegalArgumentException("frontend.url must use http or https");
        }
        return uri;
    }

    private String allowedCallbackBase(URI incoming) {
        UriComponentsBuilder builder = UriComponentsBuilder.newInstance()
                .scheme(incoming.getScheme())
                .host(incoming.getHost())
                .path(incoming.getPath());
        if (incoming.getPort() != -1) {
            builder.port(incoming.getPort());
        }

        String next =
                UriComponentsBuilder.fromUri(incoming).build().getQueryParams().getFirst(NEXT_QUERY_PARAM);
        if (isSafeRelativePath(next)) {
            builder.queryParam(NEXT_QUERY_PARAM, next);
        }
        return builder.build(true).toUriString();
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

    private static boolean isSafeRelativePath(String next) {
        if (!StringUtils.hasText(next) || next.contains("\\")) {
            return false;
        }
        try {
            URI uri = new URI(next);
            String path = uri.getPath();
            return uri.getScheme() == null
                    && uri.getRawAuthority() == null
                    && path != null
                    && path.startsWith("/")
                    && !path.startsWith("//")
                    && !path.contains("\\");
        } catch (URISyntaxException _) {
            return false;
        }
    }
}
