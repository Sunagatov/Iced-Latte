package com.zufar.icedlatte.common.correlation;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
@Order(1)
public class CorrelationFilter extends OncePerRequestFilter {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String CORRELATION_ID_MDC_KEY = "correlationId";
    private static final String SESSION_ID_HEADER = "X-Session-ID";
    private static final String SESSION_ID_MDC_KEY = "sessionId";
    private static final String TRACE_ID_HEADER = "X-Trace-ID";
    private static final String CLIENT_TRACE_ID_MDC_KEY = "clientTraceId";
    private static final String REQUEST_ID_MDC_KEY = "requestId";
    private static final String REQUEST_ID_HEADER = "X-Request-ID";

    private static final int MAX_HEADER_LENGTH = 64;
    private static final Pattern UNSAFE_HEADER_CHARS = Pattern.compile("[^A-Za-z0-9._\\-]");

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String correlationId = request.getHeader(CORRELATION_ID_HEADER) != null
                ? sanitizeHeader(request.getHeader(CORRELATION_ID_HEADER))
                : UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        String requestId = UUID.randomUUID().toString();
        String sessionId = sanitizeHeader(request.getHeader(SESSION_ID_HEADER));
        String clientTraceId = sanitizeHeader(request.getHeader(TRACE_ID_HEADER));

        response.setHeader(CORRELATION_ID_HEADER, correlationId);
        response.setHeader(REQUEST_ID_HEADER, requestId);
        MDC.put(CORRELATION_ID_MDC_KEY, correlationId);
        MDC.put(REQUEST_ID_MDC_KEY, requestId);
        if (sessionId != null) MDC.put(SESSION_ID_MDC_KEY, sessionId);
        if (clientTraceId != null) MDC.put(CLIENT_TRACE_ID_MDC_KEY, clientTraceId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(CORRELATION_ID_MDC_KEY);
            MDC.remove(REQUEST_ID_MDC_KEY);
            MDC.remove(SESSION_ID_MDC_KEY);
            MDC.remove(CLIENT_TRACE_ID_MDC_KEY);
        }
    }

    private static String sanitizeHeader(String value) {
        if (value == null) return null;
        String cleaned = UNSAFE_HEADER_CHARS.matcher(value).replaceAll("_");
        return cleaned.length() > MAX_HEADER_LENGTH ? cleaned.substring(0, MAX_HEADER_LENGTH) : cleaned;
    }
}
