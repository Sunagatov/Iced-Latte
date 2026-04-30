package com.zufar.icedlatte.security.ratelimit.filter;

import com.zufar.icedlatte.common.util.ClientIpExtractor;
import com.zufar.icedlatte.security.configuration.SecurityConstants;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private final RateLimiter rateLimiter;
    private final MeterRegistry meterRegistry;
    private final ClientIpExtractor clientIpExtractor;

    // Tracks keys that have already had their first-block WARN emitted in the current window.
    // TTL matches the longest possible window (auth = 1 min by default) so entries expire naturally.
    private final Cache<String, Boolean> warnedKeys = Caffeine.newBuilder()
            .maximumSize(5_000)
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build();

    @Value("${security.rate-limit.global.max-requests:60}")
    private int globalMaxRequests;

    @Value("${security.rate-limit.global.window-duration:PT1M}")
    private Duration globalWindowDuration;

    @Value("${security.rate-limit.auth.max-requests:10}")
    private int authMaxRequests;

    @Value("${security.rate-limit.auth.window-duration:PT1M}")
    private Duration authWindowDuration;

    @Value("${security.rate-limit.search.max-requests:30}")
    private int searchMaxRequests;

    @Value("${security.rate-limit.search.window-duration:PT1M}")
    private Duration searchWindowDuration;

    @Value("${security.rate-limit.telemetry.max-requests:120}")
    private int telemetryMaxRequests;

    @Value("${security.rate-limit.telemetry.window-duration:PT1M}")
    private Duration telemetryWindowDuration;

    // payment bucket is kept intentionally: routes /api/v1/payment/** when Stripe is re-enabled.
    // See architecture.md STRIPE_RESTORE_POINT for restore instructions.
    @Value("${security.rate-limit.payment.max-requests:20}")
    private int paymentMaxRequests;

    @Value("${security.rate-limit.payment.window-duration:PT1M}")
    private Duration paymentWindowDuration;

    @PostConstruct
    void validate() {
        assertPositive(RateLimitCategory.GLOBAL, globalMaxRequests, globalWindowDuration);
        assertPositive(RateLimitCategory.AUTH, authMaxRequests, authWindowDuration);
        assertPositive(RateLimitCategory.SEARCH, searchMaxRequests, searchWindowDuration);
        assertPositive(RateLimitCategory.TELEMETRY, telemetryMaxRequests, telemetryWindowDuration);
        assertPositive(RateLimitCategory.PAYMENT, paymentMaxRequests, paymentWindowDuration);
    }

    private static void assertPositive(RateLimitCategory category,
                                       int maxRequests,
                                       Duration window) {
        if (maxRequests <= 0) throw new IllegalStateException(
                "security.rate-limit." + category.value() + ".max-requests must be > 0, got: " + maxRequests);
        if (window == null || window.isZero() || window.isNegative()) throw new IllegalStateException(
                "security.rate-limit." + category.value() + ".window-duration must be positive, got: " + window);
    }

    public RateLimitingFilter(@Qualifier("postAuthRateLimiter") RateLimiter rateLimiter,
                              MeterRegistry meterRegistry,
                              ClientIpExtractor clientIpExtractor) {
        this.rateLimiter = rateLimiter;
        this.meterRegistry = meterRegistry;
        this.clientIpExtractor = clientIpExtractor;
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        return RateLimitRequestClassifier.shouldSkip(request);
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        RateLimitCategory category = RateLimitRequestClassifier.resolvePostAuthCategory(request);
        String rateLimitKey = buildRateLimitKey(request, category);
        RateLimitConfig config = getRateLimitConfig(category);

        RateLimitResult result = rateLimiter.tryConsume(
                rateLimitKey, config.maxRequests(), config.windowDuration());

        RateLimitResponseWriter.writeRateLimitHeaders(response, result);

        if (!result.allowed()) {
            meterRegistry.counter("rate_limit.requests.blocked", "category", category.value()).increment();
            logExceeded(request, category, result, rateLimitKey);
            RateLimitResponseWriter.writeTooManyRequests(response, result);
            return;
        }

        meterRegistry.counter("rate_limit.requests.allowed", "category", category.value()).increment();
        filterChain.doFilter(request, response);
    }

    private String buildRateLimitKey(HttpServletRequest request,
                                     RateLimitCategory category) {
        Authentication auth = SecurityContextHolder.getContext()
                .getAuthentication();

        if (auth != null && auth.isAuthenticated() && !SecurityConstants.ANONYMOUS_PRINCIPAL.equals(auth.getPrincipal())) {
            // auth category: per-user key so one account can't exhaust another's budget
            // other categories: per-user key so IP changes don't create extra budgets
            return category.value() + ":user:" + auth.getName();
        }
        return category.value() + ":ip:" + clientIpExtractor.extract(request);
    }

    private RateLimitConfig getRateLimitConfig(RateLimitCategory category) {
        return switch (category) {
            case AUTH -> new RateLimitConfig(authMaxRequests, authWindowDuration);
            case SEARCH -> new RateLimitConfig(searchMaxRequests, searchWindowDuration);
            case TELEMETRY -> new RateLimitConfig(telemetryMaxRequests, telemetryWindowDuration);
            case PAYMENT -> new RateLimitConfig(paymentMaxRequests, paymentWindowDuration);
            default -> new RateLimitConfig(globalMaxRequests, globalWindowDuration);
        };
    }

    private void logExceeded(HttpServletRequest request,
                             RateLimitCategory category,
                             RateLimitResult result,
                             String rateLimitKey) {
        String clientIp = clientIpExtractor.extract(request);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String identityType = (auth != null && auth.isAuthenticated() &&
                !SecurityConstants.ANONYMOUS_PRINCIPAL.equals(auth.getPrincipal())) ? "user" : "ip";
        long retryAfterSeconds = Math.max(1, java.util.concurrent.TimeUnit.MILLISECONDS.toSeconds(result.resetTimeMillis() - System.currentTimeMillis()));

        boolean firstBlock = warnedKeys.getIfPresent(rateLimitKey) == null;
        if (firstBlock) {
            warnedKeys.put(rateLimitKey, Boolean.TRUE);
            log.warn("rate_limit.exceeded: category={}, identity_type={}, client_ip={}, method={}, path={}, retry_after_seconds={}, limit={}, remaining={}",
                    category.value(), identityType, clientIp, request.getMethod(), ClientIpExtractor.sanitize(request.getRequestURI()),
                    retryAfterSeconds, result.limit(), Math.max(0, result.remaining()));
        } else {
            log.debug("rate_limit.exceeded: category={}, identity_type={}, client_ip={}, method={}, path={}, retry_after_seconds={}",
                    category.value(), identityType, clientIp, request.getMethod(), ClientIpExtractor.sanitize(request.getRequestURI()),
                    retryAfterSeconds);
        }
    }

    private record RateLimitConfig(int maxRequests, Duration windowDuration) {}
}
