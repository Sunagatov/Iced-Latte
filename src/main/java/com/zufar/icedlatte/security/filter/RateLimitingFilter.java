package com.zufar.icedlatte.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zufar.icedlatte.security.configuration.RateLimitingConfiguration;
import com.zufar.icedlatte.security.configuration.RateLimiter;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final RateLimiter rateLimiter;
    private final Counter allowedCounter;
    private final Counter blockedCounter;
    
    @Value("${security.rate-limit.global.max-requests:60}")
    private int globalMaxRequests;
    
    @Value("${security.rate-limit.global.window-duration:PT1M}")
    private Duration globalWindowDuration;
    
    @Value("${security.rate-limit.auth.max-requests:10}")
    private int authMaxRequests;
    
    @Value("${security.rate-limit.auth.window-duration:PT1M}")
    private Duration authWindowDuration;
    
    @Value("${security.rate-limit.ai.max-requests:5}")
    private int aiMaxRequests;
    
    @Value("${security.rate-limit.ai.window-duration:PT1M}")
    private Duration aiWindowDuration;
    
    @Value("${security.rate-limit.search.max-requests:30}")
    private int searchMaxRequests;
    
    @Value("${security.rate-limit.search.window-duration:PT1M}")
    private Duration searchWindowDuration;

    public RateLimitingFilter(RateLimiter rateLimiter, MeterRegistry meterRegistry) {
        this.rateLimiter = rateLimiter;
        this.allowedCounter = Counter.builder("rate_limit.requests.allowed")
                .description("Number of requests allowed by rate limiter")
                .register(meterRegistry);
        this.blockedCounter = Counter.builder("rate_limit.requests.blocked")
                .description("Number of requests blocked by rate limiter")
                .register(meterRegistry);
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                  @NonNull HttpServletResponse response,
                                  @NonNull FilterChain filterChain) throws ServletException, IOException {
        
        String requestPath = request.getRequestURI();
        String rateLimitKey = buildRateLimitKey(request);
        RateLimitConfig config = getRateLimitConfig(requestPath);
        
        RateLimitingConfiguration.RateLimitResult result = rateLimiter.tryConsume(
                rateLimitKey, config.maxRequests(), config.windowDuration());
        
        addRateLimitHeaders(response, result);
        
        if (!result.allowed()) {
            blockedCounter.increment();
            handleRateLimitExceeded(response, rateLimitKey, requestPath, result);
            return;
        }
        
        allowedCounter.increment();
        filterChain.doFilter(request, response);
    }
    
    private String buildRateLimitKey(HttpServletRequest request) {
        String clientIp = extractClientIp(request);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return "user:" + auth.getName() + ":" + clientIp;
        }
        return "ip:" + clientIp;
    }
    
    private String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            String firstIp = xForwardedFor.split(",")[0].trim();
            if (isValidIp(firstIp)) {
                return firstIp;
            }
        }
        return request.getRemoteAddr();
    }
    
    private boolean isValidIp(String ip) {
        return ip.matches("^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}$") || 
               ip.matches("^(?:[0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$");
    }
    
    private RateLimitConfig getRateLimitConfig(String requestPath) {
        if (requestPath.startsWith("/api/v1/auth/google")) {
            return new RateLimitConfig(globalMaxRequests, globalWindowDuration);
        }
        if (requestPath.startsWith("/api/v1/auth/")) {
            return new RateLimitConfig(authMaxRequests, authWindowDuration);
        }
        if (requestPath.contains("/ai/") || requestPath.contains("/openai/")) {
            return new RateLimitConfig(aiMaxRequests, aiWindowDuration);
        }
        if (requestPath.contains("/search")) {
            return new RateLimitConfig(searchMaxRequests, searchWindowDuration);
        }
        if (requestPath.startsWith("/api/v1/telemetry/")) {
            return new RateLimitConfig(globalMaxRequests * 2, globalWindowDuration);
        }
        return new RateLimitConfig(globalMaxRequests, globalWindowDuration);
    }
    
    private void addRateLimitHeaders(HttpServletResponse response, RateLimitingConfiguration.RateLimitResult result) {
        response.setHeader("X-RateLimit-Limit", String.valueOf(result.limit()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, result.remaining())));
        response.setHeader("X-RateLimit-Reset", String.valueOf(TimeUnit.MILLISECONDS.toSeconds(result.resetTimeMillis())));
    }
    
    private void handleRateLimitExceeded(HttpServletResponse response, String key, String path,
                                          RateLimitingConfiguration.RateLimitResult result) throws IOException {
        log.warn("rate_limit.exceeded: key={}, path={}", sanitize(key), sanitize(path));
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        
        long retryAfterSeconds = TimeUnit.MILLISECONDS.toSeconds(result.resetTimeMillis() - System.currentTimeMillis());
        response.setHeader("Retry-After", String.valueOf(Math.max(1, retryAfterSeconds)));

        ObjectNode json = OBJECT_MAPPER.createObjectNode()
                .put("error", "Rate limit exceeded")
                .put("message", "Too many requests. Please try again later.")
                .put("status", HttpStatus.TOO_MANY_REQUESTS.value())
                .put("timestamp", Instant.now().toString())
                .put("retryAfter", retryAfterSeconds);
        byte[] responseBytes = OBJECT_MAPPER.writeValueAsBytes(json);
        response.setContentLength(responseBytes.length);
        response.getOutputStream().write(responseBytes);
    }
    
    private static String sanitize(String value) {
        return value == null ? "" : value.replaceAll("[\r\n]", "_");
    }

    private record RateLimitConfig(int maxRequests, Duration windowDuration) {}
}
