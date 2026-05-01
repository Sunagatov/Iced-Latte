package com.zufar.icedlatte.common.correlation;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
public class CorrelationFilter extends OncePerRequestFilter {

    private static final int MAX_HEADER_LENGTH = 64;
    private static final Pattern UNSAFE_HEADER_CHARS = Pattern.compile("[^A-Za-z0-9._\\-]");

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        String correlationId = request.getHeader(RequestContextConstants.CORRELATION_ID_HEADER) != null
                ? sanitizeHeader(request.getHeader(RequestContextConstants.CORRELATION_ID_HEADER))
                : UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        String requestId = UUID.randomUUID().toString();
        String sessionId = sanitizeHeader(request.getHeader(RequestContextConstants.SESSION_ID_HEADER));
        String clientTraceId = sanitizeHeader(request.getHeader(RequestContextConstants.TRACE_ID_HEADER));

        response.setHeader(RequestContextConstants.CORRELATION_ID_HEADER, correlationId);
        response.setHeader(RequestContextConstants.REQUEST_ID_HEADER, requestId);
        MDC.put(RequestContextConstants.CORRELATION_ID_MDC_KEY, correlationId);
        MDC.put(RequestContextConstants.REQUEST_ID_MDC_KEY, requestId);
        if (sessionId != null) MDC.put(RequestContextConstants.SESSION_ID_MDC_KEY, sessionId);
        if (clientTraceId != null) MDC.put(RequestContextConstants.CLIENT_TRACE_ID_MDC_KEY, clientTraceId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(RequestContextConstants.CORRELATION_ID_MDC_KEY);
            MDC.remove(RequestContextConstants.REQUEST_ID_MDC_KEY);
            MDC.remove(RequestContextConstants.SESSION_ID_MDC_KEY);
            MDC.remove(RequestContextConstants.CLIENT_TRACE_ID_MDC_KEY);
            MDC.remove(RequestContextConstants.USER_ID_MDC_KEY);
        }
    }

    private static String sanitizeHeader(String value) {
        if (value == null)
            return null;
        String cleaned = UNSAFE_HEADER_CHARS.matcher(value)
                .replaceAll("_");
        return cleaned.length() > MAX_HEADER_LENGTH ?
                cleaned.substring(0, MAX_HEADER_LENGTH) : cleaned;
    }
}
