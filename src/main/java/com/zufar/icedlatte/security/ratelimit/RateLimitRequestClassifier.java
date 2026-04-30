package com.zufar.icedlatte.security.ratelimit;

import jakarta.servlet.http.HttpServletRequest;

public final class RateLimitRequestClassifier {

    private static final String AUTH_ROOT = "/api/v1/auth/";
    private static final String AUTHENTICATE_PATH = AUTH_ROOT + "authenticate";
    private static final String REGISTER_PATH = AUTH_ROOT + "register";
    private static final String GOOGLE_AUTH_PATH_PREFIX = AUTH_ROOT + "google";
    private static final String PAYMENT_ROOT = "/api/v1/payment";
    private static final String PRODUCTS_PATH = "/api/v1/products";
    private static final String TELEMETRY_ROOT = "/api/v1/telemetry/";
    private static final String ACTUATOR_ROOT = "/actuator/";
    private static final String API_DOCS_ROOT = "/api/docs/";

    private RateLimitRequestClassifier() {
    }

    public static boolean shouldSkip(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI();
        return "OPTIONS".equalsIgnoreCase(method)
                || path.startsWith(ACTUATOR_ROOT)
                || path.startsWith(API_DOCS_ROOT);
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
        if (path.startsWith(AUTH_ROOT)) {
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
        return path.equals(PAYMENT_ROOT) || path.startsWith(PAYMENT_ROOT + "/");
    }

    private static boolean isSearchPath(String path, HttpServletRequest request) {
        return path.equals(PRODUCTS_PATH) && request.getParameter("keyword") != null;
    }

    private static boolean isTelemetryPath(String path) {
        return path.startsWith(TELEMETRY_ROOT);
    }
}
