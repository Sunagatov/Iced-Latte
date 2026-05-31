package com.zufar.icedlatte.common.correlation;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;

import com.zufar.icedlatte.common.http.ApiPaths;
import com.zufar.icedlatte.common.http.RequestPathUtils;
import com.zufar.icedlatte.common.util.ClientIpExtractor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Order(2)
@RequiredArgsConstructor
@Slf4j(topic = "http.access")
public class RequestCompletionLoggingFilter extends OncePerRequestFilter {

    private static final String ANONYMOUS_PRINCIPAL = "anonymousUser";

    public static final String OUTCOME = "http.request.completed: method={}, path={}, status={}, "
            + "duration_ms={}, client_ip={}, authenticated={}, outcome={}";

    private final ClientIpExtractor clientIpExtractor;

    @Value("${logging.slow-request-threshold-ms:1000}")
    private long slowRequestThresholdMs;

    @Value("${logging.access.debug-success-paths:}")
    private String debugSuccessPaths;

    @Value("${logging.access.expected-anonymous-unauthorized-paths:}")
    private String expectedAnonymousUnauthorizedPaths;

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();
        return "OPTIONS".equalsIgnoreCase(method)
                || path.startsWith(ApiPaths.ACTUATOR_ROOT)
                || path.startsWith(ApiPaths.DOCS_ROOT);
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {
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
            } else if (status == 404 && RequestPathUtils.isPublicInternetNoise(path)) {
                log.debug(OUTCOME, args);
            } else if (!authenticated
                    && status == HttpServletResponse.SC_UNAUTHORIZED
                    && isExpectedAnonymousAuthProbe(path)) {
                log.debug(OUTCOME, args);
            } else if (slow) {
                log.warn(OUTCOME, args);
            } else if (status >= 400) {
                log.debug(OUTCOME, args);
            } else if (isDebugSuccessPath(path)) {
                log.debug(OUTCOME, args);
            } else {
                log.info(OUTCOME, args);
            }
        }
    }

    private boolean isDebugSuccessPath(String path) {
        return configuredPaths(debugSuccessPaths).contains(path);
    }

    private boolean isExpectedAnonymousAuthProbe(String path) {
        return configuredPaths(expectedAnonymousUnauthorizedPaths).contains(path);
    }

    private static Set<String> configuredPaths(String rawPaths) {
        if (rawPaths == null || rawPaths.isBlank()) {
            return Set.of();
        }
        return Stream.of(rawPaths.split(","))
                .map(String::trim)
                .filter(path -> !path.isBlank())
                .collect(Collectors.toUnmodifiableSet());
    }

    private static String resolvePathTemplate(HttpServletRequest request) {
        Object pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        String resolved = pattern != null ? pattern.toString() : null;
        if (resolved == null || "/**".equals(resolved)) {
            resolved = request.getRequestURI();
        }
        return RequestPathUtils.sanitize(RequestPathUtils.normalizePath(resolved));
    }

    private static boolean isAuthenticated() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated() && !ANONYMOUS_PRINCIPAL.equals(auth.getPrincipal());
    }
}
