package com.zufar.icedlatte.security.filter;

import com.zufar.icedlatte.security.configuration.RateLimitingConfiguration;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

/**
 * Rate limiting filter to prevent abuse and enhance security.
 * Uses Java 21 features for improved performance and readability.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitingFilter extends OncePerRequestFilter {

    private final RateLimitingConfiguration.InMemoryRateLimiter rateLimiter;
    
    @Value("${security.rate-limit.max-requests:100}")
    private int maxRequests;
    
    @Value("${security.rate-limit.window-duration:PT1M}")
    private Duration windowDuration;
    
    @Value("${security.rate-limit.auth-max-requests:5}")
    private int authMaxRequests;
    
    @Value("${security.rate-limit.auth-window-duration:PT1M}")
    private Duration authWindowDuration;

    @Value("${security.rate-limit.payment-max-requests:10}")
    private int paymentMaxRequests;

    @Value("${security.rate-limit.payment-window-duration:PT1M}")
    private Duration paymentWindowDuration;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                  @NonNull HttpServletResponse response,
                                  @NonNull FilterChain filterChain) throws ServletException, IOException {
        
        String clientIp = getClientIpAddress(request);
        String requestPath = request.getRequestURI();
        
        // Different rate limits for different endpoints
        var rateLimitConfig = getRateLimitConfig(requestPath);
        
        if (!rateLimiter.isAllowed(clientIp, rateLimitConfig.maxRequests(), rateLimitConfig.windowDuration())) {
            handleRateLimitExceeded(response, clientIp, requestPath, rateLimitConfig.windowDuration());
            return;
        }
        
        filterChain.doFilter(request, response);
    }
    
    private RateLimitConfig getRateLimitConfig(String requestPath) {
        // Using Java 21 pattern matching for switch expressions
        return switch (requestPath) {
            case String path when path.startsWith("/api/v1/auth/") -> 
                new RateLimitConfig(authMaxRequests, authWindowDuration);
            case String path when path.startsWith("/api/v1/payment/") ->
                new RateLimitConfig(paymentMaxRequests, paymentWindowDuration);
            default -> 
                new RateLimitConfig(maxRequests, windowDuration);
        };
    }
    
    private String getClientIpAddress(HttpServletRequest request) {
        return request.getRemoteAddr();
    }
    
    private void handleRateLimitExceeded(HttpServletResponse response, String clientIp, String requestPath,
                                          Duration windowDuration) throws IOException {
        log.warn("Rate limit exceeded for IP: {} on path: {}", sanitize(clientIp), sanitize(requestPath));
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Retry-After", String.valueOf(windowDuration.toSeconds()));
        
        String jsonResponse = String.format("""
            {
                "error": "Rate limit exceeded",
                "message": "Too many requests. Please try again later.",
                "status": %d,
                "timestamp": "%s"
            }
            """, 
            HttpStatus.TOO_MANY_REQUESTS.value(),
            java.time.Instant.now());
        
        response.getWriter().write(jsonResponse);
    }
    
    private static String sanitize(String value) {
        return value == null ? "" : value.replaceAll("[\r\n]", "_");
    }

    // Record for rate limit configuration - Java 21 feature
    private record RateLimitConfig(int maxRequests, Duration windowDuration) {}
}
