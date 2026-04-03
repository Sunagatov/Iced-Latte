package com.zufar.icedlatte.security.filter;

import com.zufar.icedlatte.common.util.ClientIpExtractor;
import com.zufar.icedlatte.security.configuration.RateLimiter;
import com.zufar.icedlatte.security.configuration.RateLimitingConfiguration;
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

@Slf4j
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private final RateLimiter rateLimiter;
    private final MeterRegistry meterRegistry;
    private final ClientIpExtractor clientIpExtractor;

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
        assertPositive("global", globalMaxRequests, globalWindowDuration);
        assertPositive("auth", authMaxRequests, authWindowDuration);
        assertPositive("search", searchMaxRequests, searchWindowDuration);
        assertPositive("telemetry", telemetryMaxRequests, telemetryWindowDuration);
        assertPositive("payment", paymentMaxRequests, paymentWindowDuration);
    }

    private static void assertPositive(String category, int maxRequests, Duration window) {
        if (maxRequests <= 0) throw new IllegalStateException(
                "security.rate-limit." + category + ".max-requests must be > 0, got: " + maxRequests);
        if (window == null || window.isZero() || window.isNegative()) throw new IllegalStateException(
                "security.rate-limit." + category + ".window-duration must be positive, got: " + window);
    }

    public RateLimitingFilter(@Qualifier("postAuthRateLimiter") RateLimiter rateLimiter,
                              MeterRegistry meterRegistry,
                              ClientIpExtractor clientIpExtractor) {
        this.rateLimiter = rateLimiter;
        this.meterRegistry = meterRegistry;
        this.clientIpExtractor = clientIpExtractor;
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
        String category = getRateLimitCategory(request);
        String rateLimitKey = buildRateLimitKey(request, category);
        RateLimitConfig config = getRateLimitConfig(category);

        RateLimitingConfiguration.RateLimitResult result = rateLimiter.tryConsume(
                rateLimitKey, config.maxRequests(), config.windowDuration());

        RateLimitResponseWriter.writeRateLimitHeaders(response, result);

        if (!result.allowed()) {
            meterRegistry.counter("rate_limit.requests.blocked", "category", category).increment();
            logExceeded(request, category, result);
            RateLimitResponseWriter.writeTooManyRequests(response, result);
            return;
        }

        meterRegistry.counter("rate_limit.requests.allowed", "category", category).increment();
        filterChain.doFilter(request, response);
    }

    private String buildRateLimitKey(HttpServletRequest request, String category) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            // auth category: per-user key so one account can't exhaust another's budget
            // other categories: per-user key so IP changes don't create extra budgets
            return category + ":user:" + auth.getName();
        }
        return category + ":ip:" + clientIpExtractor.extract(request);
    }

    private String getRateLimitCategory(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Google OAuth paths involve browser redirects that can fire several times per login;
        // keep them under the looser global bucket to avoid 429s during normal OAuth flows.
        if (path.startsWith("/api/v1/auth/google")) return "global";
        if (path.startsWith("/api/v1/auth/")) return "auth";
        if (path.equals("/api/v1/payment") || path.startsWith("/api/v1/payment/")) return "payment";
        if (path.equals("/api/v1/products") && request.getParameter("keyword") != null) return "search";
        if (path.startsWith("/api/v1/telemetry/")) return "telemetry";
        return "global";
    }

    private RateLimitConfig getRateLimitConfig(String category) {
        return switch (category) {
            case "auth" -> new RateLimitConfig(authMaxRequests, authWindowDuration);
            case "search" -> new RateLimitConfig(searchMaxRequests, searchWindowDuration);
            case "telemetry" -> new RateLimitConfig(telemetryMaxRequests, telemetryWindowDuration);
            case "payment" -> new RateLimitConfig(paymentMaxRequests, paymentWindowDuration);
            default -> new RateLimitConfig(globalMaxRequests, globalWindowDuration);
        };
    }

    private void logExceeded(HttpServletRequest request, String category, RateLimitingConfiguration.RateLimitResult result) {
        String clientIp = clientIpExtractor.extract(request);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String identityType = (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) ? "user" : "ip";
        long retryAfterSeconds = Math.max(1, java.util.concurrent.TimeUnit.MILLISECONDS.toSeconds(result.resetTimeMillis() - System.currentTimeMillis()));
        log.warn("rate_limit.exceeded: category={}, identityType={}, clientIp={}, method={}, path={}, retryAfterSeconds={}, limit={}, remaining={}",
                category, identityType, clientIp, request.getMethod(), ClientIpExtractor.sanitize(request.getRequestURI()),
                retryAfterSeconds, result.limit(), Math.max(0, result.remaining()));
    }

    private record RateLimitConfig(int maxRequests, Duration windowDuration) {
    }
}
