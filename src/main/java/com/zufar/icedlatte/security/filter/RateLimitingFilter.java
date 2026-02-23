package com.zufar.icedlatte.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final RateLimitingConfiguration.InMemoryRateLimiter rateLimiter;
    
    @Value("${security.rate-limit.max-requests:100}")
    private int maxRequests;
    
    @Value("${security.rate-limit.window-duration:PT1M}")
    private Duration windowDuration;
    
    @Value("${security.rate-limit.auth-max-requests:5}")
    private int authMaxRequests;
    
    @Value("${security.rate-limit.auth-window-duration:PT1M}")
    private Duration authWindowDuration;

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
        if (requestPath.startsWith("/api/v1/auth/")) {
            return new RateLimitConfig(authMaxRequests, authWindowDuration);
        }
        if (requestPath.startsWith("/api/v1/telemetry/")) {
            // telemetry is high-volume by design; use a generous but bounded limit
            return new RateLimitConfig(maxRequests * 5, windowDuration);
        }
        return new RateLimitConfig(maxRequests, windowDuration);
    }
    
    private String getClientIpAddress(HttpServletRequest request) {
        return request.getRemoteAddr();
    }
    
    private void handleRateLimitExceeded(HttpServletResponse response, String clientIp, String requestPath,
                                          Duration windowDuration) throws IOException {
        log.warn("rate_limit.exceeded: ip={}, path={}", sanitize(clientIp), sanitize(requestPath));
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Retry-After", String.valueOf(windowDuration.toSeconds()));

        ObjectNode json = OBJECT_MAPPER.createObjectNode()
                .put("error", "Rate limit exceeded")
                .put("message", "Too many requests. Please try again later.")
                .put("status", HttpStatus.TOO_MANY_REQUESTS.value())
                .put("timestamp", java.time.Instant.now().toString());
        // amazonq-ignore-next-line
        byte[] responseBytes = OBJECT_MAPPER.writeValueAsBytes(json);
        response.setContentLength(responseBytes.length);
        response.getOutputStream().write(responseBytes);
    }
    
    private static String sanitize(String value) {
        return value == null ? "" : value.replaceAll("[\r\n]", "_");
    }

    // Record for rate limit configuration - Java 21 feature
    private record RateLimitConfig(int maxRequests, Duration windowDuration) {}
}
