package com.zufar.icedlatte.ratelimit.filter;

import java.util.Locale;
import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;

import com.zufar.icedlatte.common.http.ApiPaths;
import com.zufar.icedlatte.ratelimit.dto.RateLimitCategory;

import lombok.experimental.UtilityClass;

@UtilityClass
class RateLimitRouteClassifier {

    private static final Set<String> READ_METHODS = Set.of("GET", "HEAD", "OPTIONS");
    private static final String AUTH_REGISTER = ApiPaths.AUTH + "/register";
    private static final String TELEMETRY_ROOT = ApiPaths.API_ROOT + "/v1/telemetry/";

    static boolean shouldSkip(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI();
        return "OPTIONS".equalsIgnoreCase(method) || isActuatorPath(path) || isDocsPath(path);
    }

    static RateLimitCategory classify(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod().toUpperCase(Locale.ROOT);
        return switch (path) {
            case String uri
            when uri.startsWith(ApiPaths.AUTH_ROOT_PREFIX) && !isGlobalAuthPath(uri) -> RateLimitCategory.AUTH;
            case String uri when isPasswordResetPath(uri) -> RateLimitCategory.AUTH;
            case String uri
            when uri.equals(ApiPaths.PAYMENT) || uri.startsWith(ApiPaths.PAYMENT + "/") -> RateLimitCategory.PAYMENT;
            case String uri
            when uri.equals(ApiPaths.PRODUCTS) && request.getParameter("keyword") != null -> RateLimitCategory.SEARCH;
            case String uri when uri.startsWith(TELEMETRY_ROOT) -> RateLimitCategory.TELEMETRY;
            case String uri when isFileUploadRequest(request, uri) -> RateLimitCategory.FILE_UPLOAD;
            case String _ when !READ_METHODS.contains(method) -> RateLimitCategory.WRITE;
            default -> RateLimitCategory.GLOBAL;
        };
    }

    static boolean isStrictPreAuthPath(String path) {
        return path.equals(ApiPaths.AUTH_AUTHENTICATE) || path.equals(AUTH_REGISTER) || isPasswordResetPath(path);
    }

    private static boolean isActuatorPath(String path) {
        return isPathUnder(path, ApiPaths.ACTUATOR_ROOT) || isPathUnder(path, ApiPaths.API_ROOT + ApiPaths.ACTUATOR_ROOT);
    }

    private static boolean isDocsPath(String path) {
        return isPathUnder(path, ApiPaths.DOCS_ROOT);
    }

    private static boolean isFileUploadRequest(HttpServletRequest request, String path) {
        String contentType = request.getContentType();
        return contentType != null
                && contentType.toLowerCase(Locale.ROOT).startsWith("multipart/")
                && (path.endsWith("/avatar") || path.contains("/images"));
    }

    private static boolean isPathUnder(String path, String root) {
        String normalizedRoot = root.endsWith("/") ? root.substring(0, root.length() - 1) : root;
        return path.equals(normalizedRoot) || path.startsWith(normalizedRoot + "/");
    }

    private static boolean isGlobalAuthPath(String path) {
        return path.startsWith(ApiPaths.AUTH_OAUTH + "/")
                || path.equals(ApiPaths.AUTH_AUTHENTICATE)
                || path.equals(AUTH_REGISTER);
    }

    private static boolean isPasswordResetPath(String path) {
        return path.equals(ApiPaths.AUTH_PASSWORD_FORGOT) || path.equals(ApiPaths.AUTH_PASSWORD_CHANGE);
    }
}
