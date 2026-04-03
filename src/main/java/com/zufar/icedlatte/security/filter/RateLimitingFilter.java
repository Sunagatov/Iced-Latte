package com.zufar.icedlatte.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zufar.icedlatte.common.util.ClientIpExtractor;
import com.zufar.icedlatte.security.configuration.RateLimiter;
import com.zufar.icedlatte.security.configuration.RateLimitingConfiguration;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.jspecify.annotations.NonNull;
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

    @Value("${security.rate-limit.payment.max-requests:20}")
    private int paymentMaxRequests;

    @Value("${security.rate-limit.payment.window-duration:PT1M}")
    private Duration paymentWindowDuration;

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

        addRateLimitHeaders(response, result);

        if (!result.allowed()) {
            meterRegistry.counter("rate_limit.requests.blocked", "category", category).increment();
            handleRateLimitExceeded(response, request, category, result);
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

    private void addRateLimitHeaders(HttpServletResponse response, RateLimitingConfiguration.RateLimitResult result) {
        response.setHeader("X-RateLimit-Limit", String.valueOf(result.limit()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, result.remaining())));
        response.setHeader("X-RateLimit-Reset", String.valueOf(TimeUnit.MILLISECONDS.toSeconds(result.resetTimeMillis())));
    }

    private void handleRateLimitExceeded(HttpServletResponse response, HttpServletRequest request,
                                         String category,
                                         RateLimitingConfiguration.RateLimitResult result) throws IOException {
        String clientIp = clientIpExtractor.extract(request);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String identityType = (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) ? "user" : "ip";
        long retryAfterSeconds = Math.max(1, TimeUnit.MILLISECONDS.toSeconds(result.resetTimeMillis() - System.currentTimeMillis()));
        log.warn("rate_limit.exceeded: category={}, identityType={}, clientIp={}, method={}, path={}, retryAfterSeconds={}, limit={}, remaining={}",
                category, identityType, clientIp, request.getMethod(), ClientIpExtractor.sanitize(request.getRequestURI()),
                retryAfterSeconds, result.limit(), Math.max(0, result.remaining()));
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
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

    private record RateLimitConfig(int maxRequests, Duration windowDuration) {
    }
}
