package com.zufar.icedlatte.security.ratelimit;

import com.zufar.icedlatte.common.http.ApiPaths;
import com.zufar.icedlatte.security.configuration.AuthPaths;
import jakarta.servlet.http.HttpServletRequest;

public final class RateLimitRequestClassifier {

    private static final String AUTHENTICATE_PATH = AuthPaths.AUTHENTICATE;
    private static final String REGISTER_PATH = AuthPaths.ROOT + "/register";
    private static final String GOOGLE_AUTH_PATH_PREFIX = AuthPaths.GOOGLE;
    private static final String TELEMETRY_ROOT = "/api/v1/telemetry/";

    private RateLimitRequestClassifier() {
    }

    public static boolean shouldSkip(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI();
        return "OPTIONS".equalsIgnoreCase(method)
                || path.startsWith(ApiPaths.ACTUATOR_ROOT)
                || path.startsWith(ApiPaths.DOCS_ROOT);
    }

    public static boolean isStrictPreAuthPath(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.equals(AUTHENTICATE_PATH) || path.equals(REGISTER_PATH);
    }

    public static RateLimitCategory resolvePostAuthCategory(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (isGlobalAuthPath(path)) {
            return RateLimitCategory.GLOBAL;
        }
        if (path.startsWith(AuthPaths.ROOT_PREFIX)) {
            return RateLimitCategory.AUTH;
        }
        if (isPaymentPath(path)) {
            return RateLimitCategory.PAYMENT;
        }
        if (isSearchPath(path, request)) {
            return RateLimitCategory.SEARCH;
        }
        if (isTelemetryPath(path)) {
            return RateLimitCategory.TELEMETRY;
        }
        return RateLimitCategory.GLOBAL;
    }

    private static boolean isGlobalAuthPath(String path) {
        return path.startsWith(GOOGLE_AUTH_PATH_PREFIX)
                || path.equals(AUTHENTICATE_PATH)
                || path.equals(REGISTER_PATH);
    }

    private static boolean isPaymentPath(String path) {
        return path.equals(ApiPaths.PAYMENT) || path.startsWith(ApiPaths.PAYMENT + "/");
    }

    private static boolean isSearchPath(String path, HttpServletRequest request) {
        return path.equals(ApiPaths.PRODUCTS) && request.getParameter("keyword") != null;
    }

    private static boolean isTelemetryPath(String path) {
        return path.startsWith(TELEMETRY_ROOT);
    }
}
