package com.zufar.icedlatte.security.oauth.flow;

import java.time.Duration;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.WebUtils;

import com.zufar.icedlatte.common.http.ApiPaths;
import com.zufar.icedlatte.security.oauth.config.OAuthProvider;

@Service
class OAuthStateCookieService {

    private static final String COOKIE_PREFIX = "iced_latte_oauth_state_";
    private static final String SAME_SITE_LAX = "Lax";

    void bind(
            HttpServletRequest request,
            HttpServletResponse response,
            OAuthProvider provider,
            String state,
            Duration ttl) {
        response.addHeader(
                HttpHeaders.SET_COOKIE,
                cookie(provider, state, ttl, isSecure(request)).toString());
    }

    boolean matches(HttpServletRequest request, OAuthProvider provider, String state) {
        if (!StringUtils.hasText(state)) {
            return false;
        }
        var cookie = WebUtils.getCookie(request, cookieName(provider));
        return cookie != null && state.equals(cookie.getValue());
    }

    void clear(HttpServletRequest request, HttpServletResponse response, OAuthProvider provider) {
        response.addHeader(
                HttpHeaders.SET_COOKIE,
                cookie(provider, "", Duration.ZERO, isSecure(request)).toString());
    }

    static String cookieName(OAuthProvider provider) {
        return COOKIE_PREFIX + provider.id();
    }

    private static ResponseCookie cookie(OAuthProvider provider, String value, Duration maxAge, boolean secure) {
        return ResponseCookie.from(cookieName(provider), value)
                .httpOnly(true)
                .secure(secure)
                .sameSite(SAME_SITE_LAX)
                .path(ApiPaths.AUTH_OAUTH)
                .maxAge(maxAge)
                .build();
    }

    private static boolean isSecure(HttpServletRequest request) {
        return request.isSecure() || "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto"));
    }
}
