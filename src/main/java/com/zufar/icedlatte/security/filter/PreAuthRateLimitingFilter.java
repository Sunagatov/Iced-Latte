package com.zufar.icedlatte.security.filter;

import com.zufar.icedlatte.common.util.ClientIpExtractor;
import com.zufar.icedlatte.security.configuration.RateLimiter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

/**
 * IP-only rate limiter that runs before JWT authentication.
 * Ensures that requests with invalid/expired/malformed tokens are still throttled,
 * since JwtAuthenticationFilter short-circuits those before RateLimitingFilter runs.
 * Uses a higher limit than the per-category limiter — it is a coarse flood guard, not a fine policy.
 */
@Slf4j
@Component
public class PreAuthRateLimitingFilter extends OncePerRequestFilter {

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

    @PostConstruct
    void validate() {
        if (maxRequests <= 0) throw new IllegalStateException(
                "security.rate-limit.pre-auth.max-requests must be > 0, got: " + maxRequests);
        if (windowDuration == null || windowDuration.isZero() || windowDuration.isNegative()) throw new IllegalStateException(
                "security.rate-limit.pre-auth.window-duration must be positive, got: " + windowDuration);
    }

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

        RateLimitResponseWriter.writeRateLimitHeaders(response, result);

        if (!result.allowed()) {
            meterRegistry.counter("rate_limit.requests.blocked", "category", "pre-auth").increment();
            long retryAfterSeconds = Math.max(1, java.util.concurrent.TimeUnit.MILLISECONDS.toSeconds(result.resetTimeMillis() - System.currentTimeMillis()));
            log.warn("rate_limit.exceeded: category=pre-auth, identityType=ip, clientIp={}, method={}, path={}, retryAfterSeconds={}, limit={}, remaining={}",
                    ip, request.getMethod(), ClientIpExtractor.sanitize(request.getRequestURI()),
                    retryAfterSeconds, result.limit(), Math.max(0, result.remaining()));
            RateLimitResponseWriter.writeTooManyRequests(response, result);
            return;
        }

        meterRegistry.counter("rate_limit.requests.allowed", "category", "pre-auth").increment();
        filterChain.doFilter(request, response);
    }
}
