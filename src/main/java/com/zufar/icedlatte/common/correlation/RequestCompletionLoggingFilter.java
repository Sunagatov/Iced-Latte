package com.zufar.icedlatte.common.correlation;

import com.zufar.icedlatte.common.http.ApiPaths;
import com.zufar.icedlatte.common.util.ClientIpExtractor;
import com.zufar.icedlatte.security.configuration.AuthPaths;
import com.zufar.icedlatte.security.configuration.SecurityConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;

import java.io.IOException;

@Component
@Order(2)
@RequiredArgsConstructor
@Slf4j(topic = "http.access")
public class RequestCompletionLoggingFilter extends OncePerRequestFilter {

    public static final String OUTCOME = "http.request.completed: method={}, path={}, status={}, " +
            "duration_ms={}, client_ip={}, authenticated={}, outcome={}";

    private final ClientIpExtractor clientIpExtractor;

    @Value("${logging.slow-request-threshold-ms:1000}")
    private long slowRequestThresholdMs;

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();
        return "OPTIONS".equalsIgnoreCase(method)
                || path.startsWith(ApiPaths.ACTUATOR_ROOT)
                || path.startsWith(ApiPaths.DOCS_ROOT);
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        long start = System.currentTimeMillis();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = System.currentTimeMillis() - start;
            int status = response.getStatus();

            String outcome;
            if (status < 400) {
                outcome = "SUCCESS";
            } else if (status < 500) {
                outcome = "CLIENT_ERROR";
            } else {
                outcome = "SERVER_ERROR";
            }

            String clientIp = clientIpExtractor.extract(request);
            String method = request.getMethod();
            String path = resolvePathTemplate(request);
            boolean slow = durationMs >= slowRequestThresholdMs;
            boolean authenticated = isAuthenticated();

            Object[] args = {method, path, status, durationMs, clientIp, authenticated, outcome};

            if (status >= 500) {
                log.error(OUTCOME, args);
            } else if (status == 404 && isPublicInternetNoise(path)) {
                log.debug(OUTCOME, args);
            } else if (!authenticated && status == HttpServletResponse.SC_UNAUTHORIZED && isExpectedAnonymousAuthProbe(path)) {
                log.debug(OUTCOME, args);
            } else if (slow) {
                log.warn(OUTCOME, args);
            } else if (status >= 400) {
                log.debug(OUTCOME, args);
            } else if (isPollingEndpoint(path)) {
                log.debug(OUTCOME, args);
            } else {
                log.info(OUTCOME, args);
            }
        }
    }

    // Endpoints that are polled frequently by the frontend and produce repetitive 2xx lines.
    // Logged at DEBUG to reduce noise; operators can promote to INFO via logging.level.http.access=INFO.
    private static boolean isPollingEndpoint(String path) {
        return ApiPaths.PRODUCTS_BRANDS.equals(path)
                || ApiPaths.PRODUCTS_SELLERS.equals(path)
                || ApiPaths.USERS.equals(path)
                || ApiPaths.CART.equals(path)
                || ApiPaths.FAVORITES.equals(path);
    }

    // Expected anonymous bootstrap / probe flow from the frontend.
    // These are not operationally interesting at WARN when the user is simply unauthenticated.
    private static boolean isExpectedAnonymousAuthProbe(String path) {
        return AuthPaths.REFRESH.equals(path)
                || ApiPaths.USERS.equals(path)
                || ApiPaths.CART.equals(path)
                || ApiPaths.FAVORITES.equals(path);
    }

    private static boolean isPublicInternetNoise(String path) {
        String normalized = normalizePath(path);
        return !normalized.startsWith(ApiPaths.API_ROOT + "/")
                && !normalized.startsWith(ApiPaths.ACTUATOR_ROOT)
                && !normalized.startsWith(ApiPaths.DOCS_ROOT);
    }

    private static String resolvePathTemplate(HttpServletRequest request) {
        Object pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        String resolved = pattern != null ? pattern.toString() : null;
        if (resolved == null || "/**".equals(resolved)) {
            resolved = request.getRequestURI();
        }
        return sanitize(normalizePath(resolved));
    }

    private static boolean isAuthenticated() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null
                && auth.isAuthenticated()
                && !SecurityConstants.ANONYMOUS_PRINCIPAL.equals(auth.getPrincipal());
    }

    private static String normalizePath(String value) {
        if (value == null || value.isBlank()) {
            return "/";
        }
        return value.startsWith("/") ? value : "/" + value;
    }

    private static String sanitize(String value) {
        return value == null ? "" : value.replaceAll("[\\r\\n]", "_");
    }
}
