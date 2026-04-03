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

            log.info("http.request.completed: method={}, path={}, status={}, durationMs={}, clientIp={}, outcome={}",
                    request.getMethod(),
                    sanitize(request.getRequestURI()),
                    status,
                    durationMs,
                    clientIp,
                    outcome);
        }
    }

    private static String sanitize(String value) {
        return value == null ? "" : value.replaceAll("[\r\n]", "_");
    }
}
