package com.zufar.icedlatte.security.ratelimit.filter;

import com.zufar.icedlatte.common.util.ClientIpExtractor;
import com.zufar.icedlatte.security.ratelimit.RateLimitCategory;
import com.zufar.icedlatte.security.ratelimit.RateLimitRequestClassifier;
import com.zufar.icedlatte.security.ratelimit.RateLimitResponseWriter;
import com.zufar.icedlatte.security.ratelimit.RateLimiter;
import com.zufar.icedlatte.security.ratelimit.RateLimitingConfiguration.RateLimitResult;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
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
import java.util.concurrent.TimeUnit;

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

    private final RateLimiter floodRateLimiter;
    private final RateLimiter authRateLimiter;
    private final MeterRegistry meterRegistry;
    private final ClientIpExtractor clientIpExtractor;
    private final Cache<String, Boolean> warnedKeys = Caffeine.newBuilder()
            .maximumSize(5_000)
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build();

    public PreAuthRateLimitingFilter(@Qualifier("preAuthFloodRateLimiter") RateLimiter floodRateLimiter,
                                     @Qualifier("preAuthAuthRateLimiter") RateLimiter authRateLimiter,
                                     MeterRegistry meterRegistry,
                                     ClientIpExtractor clientIpExtractor) {
        this.floodRateLimiter = floodRateLimiter;
        this.authRateLimiter = authRateLimiter;
        this.meterRegistry = meterRegistry;
        this.clientIpExtractor = clientIpExtractor;
    }

    @PostConstruct
    void validate() {
        validatePositive("security.rate-limit.pre-auth.max-requests", maxRequests);
        validatePositive("security.rate-limit.pre-auth.window-duration", windowDuration);
        validatePositive("security.rate-limit.auth.max-requests", authMaxRequests);
        validatePositive("security.rate-limit.auth.window-duration", authWindowDuration);
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        return RateLimitRequestClassifier.shouldSkip(request);
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String ip = clientIpExtractor.extract(request);

        // Auth endpoints get their own tight bucket here (fail-closed) so that a post-auth
        // limiter backend failure cannot degrade auth throttling from 10/min to 200/min.
        if (RateLimitRequestClassifier.isStrictPreAuthPath(request)) {
            var authResult = authRateLimiter.tryConsume("auth:ip:" + ip, authMaxRequests, authWindowDuration);
            RateLimitResponseWriter.writeRateLimitHeaders(response, authResult);
            if (!authResult.allowed()) {
                blockRequest(request, response, authResult, ip, RateLimitCategory.AUTH_PRE, "auth:ip:" + ip);
                return;
            }
            recordAllowed(RateLimitCategory.AUTH_PRE);
        }

        String key = "pre-auth:ip:" + ip;
        var result = floodRateLimiter.tryConsume(key, maxRequests, windowDuration);

        RateLimitResponseWriter.writeRateLimitHeaders(response, result);

        if (!result.allowed()) {
            blockRequest(request, response, result, ip, RateLimitCategory.PRE_AUTH, key);
            return;
        }

        recordAllowed(RateLimitCategory.PRE_AUTH);
        filterChain.doFilter(request, response);
    }

    private void blockRequest(HttpServletRequest request,
                              HttpServletResponse response,
                              RateLimitResult result,
                              String ip,
                              RateLimitCategory category,
                              String rateLimitKey) throws IOException {
        meterRegistry.counter("rate_limit.requests.blocked", "category", category.value()).increment();
        long retryAfterSeconds = retryAfterSeconds(result);
        boolean firstBlock = warnedKeys.getIfPresent(rateLimitKey) == null;
        if (firstBlock) {
            warnedKeys.put(rateLimitKey, Boolean.TRUE);
            log.warn("rate_limit.exceeded: category={}, identity_type=ip, client_ip={}, method={}, path={}, retry_after_seconds={}, limit={}, remaining={}",
                    category.value(),
                    ip,
                    request.getMethod(),
                    ClientIpExtractor.sanitize(request.getRequestURI()),
                    retryAfterSeconds,
                    result.limit(),
                    Math.max(0, result.remaining()));
        } else {
            log.debug("rate_limit.exceeded: category={}, identity_type=ip, client_ip={}, method={}, path={}, retry_after_seconds={}",
                    category.value(),
                    ip,
                    request.getMethod(),
                    ClientIpExtractor.sanitize(request.getRequestURI()),
                    retryAfterSeconds);
        }
        RateLimitResponseWriter.writeTooManyRequests(response, result);
    }

    private void recordAllowed(RateLimitCategory category) {
        meterRegistry.counter("rate_limit.requests.allowed", "category", category.value()).increment();
    }

    private long retryAfterSeconds(RateLimitResult result) {
        return Math.max(1, java.util.concurrent.TimeUnit.MILLISECONDS.toSeconds(result.resetTimeMillis() - System.currentTimeMillis()));
    }

    private void validatePositive(String property, int value) {
        if (value <= 0) {
            throw new IllegalStateException(property + " must be > 0, got: " + value);
        }
    }

    private void validatePositive(String property, Duration value) {
        if (value == null || value.isZero() || value.isNegative()) {
            throw new IllegalStateException(property + " must be positive, got: " + value);
        }
    }
}
