package com.zufar.icedlatte.common.correlation;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Filter to handle correlation ID for each HTTP request.
 * Uses Java 21 ScopedValue for better performance.
 */
@Slf4j
@Component
@Order(1)
public class CorrelationFilter extends OncePerRequestFilter {
    
    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String CORRELATION_ID_MDC_KEY = "correlationId";
    
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) 
            throws ServletException, IOException {
        
        String correlationId = getOrGenerateCorrelationId(request);
        response.setHeader(CORRELATION_ID_HEADER, correlationId);
        MDC.put(CORRELATION_ID_MDC_KEY, correlationId);
        try {
            CorrelationContext.runWithCorrelationId(correlationId, () -> {
                filterChain.doFilter(request, response);
                return null;
            });
        } catch (ServletException | IOException e) {
            throw e;
        } catch (Exception e) {
            throw new ServletException("Unexpected error in correlation filter", e);
        } finally {
            MDC.remove(CORRELATION_ID_MDC_KEY);
        }
    }
    
    private String getOrGenerateCorrelationId(HttpServletRequest request) {
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        return correlationId != null ? correlationId : generateCorrelationId();
    }
    
    private String generateCorrelationId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}