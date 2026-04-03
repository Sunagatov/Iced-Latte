package com.zufar.icedlatte.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zufar.icedlatte.common.util.ClientIpExtractor;
import com.zufar.icedlatte.security.configuration.RateLimiter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

/**
 * IP-only rate limiter that runs before JWT authentication.
 * Ensures that requests with invalid/expired/malformed tokens are still throttled,
 * since JwtAuthenticationFilter short-circuits those before RateLimitingFilter runs.
 * Uses a higher limit than the per-category limiter — it is a coarse flood guard, not a fine policy.
 */
@Slf4j
@Component
public class PreAuthRateLimitingFilter extends OncePerRequestFilter {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final RateLimiter rateLimiter;
    private final MeterRegistry meterRegistry;
    private final ClientIpExtractor clientIpExtractor;

    public PreAuthRateLimitingFilter(@Qualifier("preAuthRateLimiter") RateLimiter rateLimiter,
                                     MeterRegistry meterRegistry,
                                     ClientIpExtractor clientIpExtractor) {
        this.rateLimiter = rateLimiter;
        this.meterRegistry = meterRegistry;
        this.clientIpExtractor = clientIpExtractor;
    }

    @Value("${security.rate-limit.pre-auth.max-requests:200}")
    private int maxRequests;

    @Value("${security.rate-limit.pre-auth.window-duration:PT1M}")
    private Duration windowDuration;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI();
        return "OPTIONS".equalsIgnoreCase(method)
                || path.startsWith("/actuator/")
                || path.startsWith("/api/docs/");
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String ip = clientIpExtractor.extract(request);
        String key = "pre-auth:ip:" + ip;

        var result = rateLimiter.tryConsume(key, maxRequests, windowDuration);

        if (!result.allowed()) {
            meterRegistry.counter("rate_limit.requests.blocked", "category", "pre-auth").increment();
            long retryAfterSeconds = Math.max(1, TimeUnit.MILLISECONDS.toSeconds(result.resetTimeMillis() - System.currentTimeMillis()));
            log.warn("rate_limit.exceeded: category=pre-auth, identityType=ip, clientIp={}, method={}, path={}, retryAfterSeconds={}, limit={}, remaining={}",
                    ip, request.getMethod(), ClientIpExtractor.sanitize(request.getRequestURI()),
                    retryAfterSeconds, result.limit(), Math.max(0, result.remaining()));
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
            response.setHeader("X-RateLimit-Limit", String.valueOf(result.limit()));
            response.setHeader("X-RateLimit-Remaining", "0");
            response.setHeader("X-RateLimit-Reset", String.valueOf(TimeUnit.MILLISECONDS.toSeconds(result.resetTimeMillis())));
            ObjectNode json = OBJECT_MAPPER.createObjectNode()
                    .put("error", "Rate limit exceeded")
                    .put("message", "Too many requests. Please try again later.")
                    .put("status", HttpStatus.TOO_MANY_REQUESTS.value())
                    .put("timestamp", Instant.now().toString())
                    .put("retryAfter", retryAfterSeconds);
            byte[] bytes = OBJECT_MAPPER.writeValueAsBytes(json);
            response.setContentLength(bytes.length);
            response.getOutputStream().write(bytes);
            return;
        }

        meterRegistry.counter("rate_limit.requests.allowed", "category", "pre-auth").increment();
        filterChain.doFilter(request, response);
    }
}
