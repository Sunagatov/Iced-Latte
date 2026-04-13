package com.zufar.icedlatte.security.filter;

import com.zufar.icedlatte.common.util.ClientIpExtractor;
import com.zufar.icedlatte.security.configuration.RateLimiter;
import com.zufar.icedlatte.security.configuration.SecurityConstants;
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
 * <p>
 * Also enforces the tight auth bucket (login/register) here under fail-closed policy,
 * so that a post-auth limiter backend failure cannot degrade auth throttling from 10/min to 200/min.
 */
@Slf4j
@Component
public class PreAuthRateLimitingFilter extends OncePerRequestFilter {

    @Value("${security.rate-limit.pre-auth.max-requests:200}")
    private int maxRequests;

    @Value("${security.rate-limit.pre-auth.window-duration:PT1M}")
    private Duration windowDuration;

    @Value("${security.rate-limit.auth.max-requests:10}")
    private int authMaxRequests;

    @Value("${security.rate-limit.auth.window-duration:PT1M}")
    private Duration authWindowDuration;

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

    @PostConstruct
    void validate() {
        if (maxRequests <= 0) throw new IllegalStateException(
                "security.rate-limit.pre-auth.max-requests must be > 0, got: " + maxRequests);
        if (windowDuration == null || windowDuration.isZero() || windowDuration.isNegative()) throw new IllegalStateException(
                "security.rate-limit.pre-auth.window-duration must be positive, got: " + windowDuration);
        if (authMaxRequests <= 0) throw new IllegalStateException(
                "security.rate-limit.auth.max-requests must be > 0, got: " + authMaxRequests);
        if (authWindowDuration == null || authWindowDuration.isZero() || authWindowDuration.isNegative()) throw new IllegalStateException(
                "security.rate-limit.auth.window-duration must be positive, got: " + authWindowDuration);
    }

    private boolean isAuthEndpoint(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.equals(SecurityConstants.AUTH_AUTHENTICATE_URL) || path.equals("/api/v1/auth/register");
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

        // Auth endpoints get their own tight bucket here (fail-closed) so that a post-auth
        // limiter backend failure cannot degrade auth throttling from 10/min to 200/min.
        if (isAuthEndpoint(request)) {
            var authResult = rateLimiter.tryConsume("auth:ip:" + ip, authMaxRequests, authWindowDuration);
            RateLimitResponseWriter.writeRateLimitHeaders(response, authResult);
            if (!authResult.allowed()) {
                meterRegistry.counter("rate_limit.requests.blocked", "category", "auth-pre").increment();
                long retryAfterSeconds = Math.max(1, java.util.concurrent.TimeUnit.MILLISECONDS.toSeconds(authResult.resetTimeMillis() - System.currentTimeMillis()));
                log.warn("rate_limit.exceeded: category=auth-pre, identity_type=ip, client_ip={}, method={}, path={}, retry_after_seconds={}, limit={}, remaining={}",
                        ip, request.getMethod(), ClientIpExtractor.sanitize(request.getRequestURI()),
                        retryAfterSeconds, authResult.limit(), Math.max(0, authResult.remaining()));
                RateLimitResponseWriter.writeTooManyRequests(response, authResult);
                return;
            }
            meterRegistry.counter("rate_limit.requests.allowed", "category", "auth-pre")
                    .increment();
        }

        String key = "pre-auth:ip:" + ip;
        var result = rateLimiter.tryConsume(key, maxRequests, windowDuration);

        RateLimitResponseWriter.writeRateLimitHeaders(response, result);

        if (!result.allowed()) {
            meterRegistry.counter("rate_limit.requests.blocked", "category", "pre-auth")
                    .increment();
            long retryAfterSeconds = Math.max(1, java.util.concurrent.TimeUnit.MILLISECONDS.toSeconds(result.resetTimeMillis() - System.currentTimeMillis()));
            log.warn("rate_limit.exceeded: category=pre-auth, identity_type=ip, client_ip={}, method={}, path={}, retry_after_seconds={}, limit={}, remaining={}",
                    ip, request.getMethod(), ClientIpExtractor.sanitize(request.getRequestURI()),
                    retryAfterSeconds, result.limit(), Math.max(0, result.remaining()));
            RateLimitResponseWriter.writeTooManyRequests(response, result);
            return;
        }

        meterRegistry.counter("rate_limit.requests.allowed", "category", "pre-auth")
                .increment();
        filterChain.doFilter(request, response);
    }
}
