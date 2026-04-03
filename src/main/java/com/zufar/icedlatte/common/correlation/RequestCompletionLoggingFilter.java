package com.zufar.icedlatte.common.correlation;

import com.zufar.icedlatte.common.util.ClientIpExtractor;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class RequestCompletionLoggingFilter extends OncePerRequestFilter {

    private final ClientIpExtractor clientIpExtractor;

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();
        return "OPTIONS".equalsIgnoreCase(method) || path.startsWith("/actuator/") || path.startsWith("/api/docs/");
    }

    private static final long SLOW_REQUEST_THRESHOLD_MS = 1000L;

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
            String outcome = status < 400 ? "SUCCESS" : status < 500 ? "CLIENT_ERROR" : "SERVER_ERROR";
            String clientIp = clientIpExtractor.extract(request);
            String method = request.getMethod();
            String path = sanitize(request.getRequestURI());
            boolean slow = durationMs >= SLOW_REQUEST_THRESHOLD_MS;
            boolean authenticated = isAuthenticated();

            String msg = "http.request.completed: method={}, path={}, status={}, durationMs={}, clientIp={}, authenticated={}, slow={}, outcome={}";
            Object[] args = {method, path, status, durationMs, clientIp, authenticated, slow, outcome};

            if (status >= 500) {
                log.error(msg, args);
            } else if (status >= 400 || slow) {
                log.warn(msg, args);
            } else {
                log.info(msg, args);
            }
        }
    }

    private static boolean isAuthenticated() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal());
    }

    private static String sanitize(String value) {
        return value == null ? "" : value.replaceAll("[\r\n]", "_");
    }
}
