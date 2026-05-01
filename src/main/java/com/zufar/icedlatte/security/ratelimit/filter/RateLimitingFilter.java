package com.zufar.icedlatte.security.ratelimit.filter;

import com.zufar.icedlatte.common.http.ApiPaths;
import com.zufar.icedlatte.common.util.ClientIpExtractor;
import com.zufar.icedlatte.security.configuration.AuthPaths;
import com.zufar.icedlatte.security.jwt.JwtBlacklistValidator;
import com.zufar.icedlatte.security.jwt.JwtClaimExtractor;
import com.zufar.icedlatte.security.jwt.JwtTokenFromAuthHeaderExtractor;
import com.zufar.icedlatte.security.ratelimit.RateLimitCategory;
import com.zufar.icedlatte.security.ratelimit.RateLimitProperties;
import com.zufar.icedlatte.security.ratelimit.RateLimitProperties.Bucket;
import com.zufar.icedlatte.security.ratelimit.RateLimitResult;
import com.zufar.icedlatte.security.ratelimit.RateLimitResponseWriter;
import com.zufar.icedlatte.security.ratelimit.RateLimiter;
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
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private final RateLimiter openRateLimiter;
    private final RateLimiter closedRateLimiter;
    private final MeterRegistry meterRegistry;
    private final ClientIpExtractor clientIpExtractor;
    private final JwtTokenFromAuthHeaderExtractor jwtTokenFromAuthHeaderExtractor;
    private final JwtClaimExtractor jwtClaimExtractor;
    private final JwtBlacklistValidator jwtBlacklistValidator;
    private final RateLimitProperties properties;

    private final Cache<String, Boolean> warnedKeys = Caffeine.newBuilder()
            .maximumSize(5_000)
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build();

    @PostConstruct
    void validate() {
        assertPositive("pre-auth", properties.getPreAuth());
        assertPositive("auth", properties.getAuth());
        assertPositive("global", properties.getGlobal());
        assertPositive("search", properties.getSearch());
        assertPositive("telemetry", properties.getTelemetry());
        assertPositive("payment", properties.getPayment());
    }

    private static void assertPositive(String bucketName, Bucket bucket) {
        if (bucket.getMaxRequests() <= 0) {
            throw new IllegalStateException(
                    "security.rate-limit." + bucketName + ".max-requests must be > 0, got: " + bucket.getMaxRequests());
        }
        if (bucket.getWindowDuration() == null || bucket.getWindowDuration().isZero() || bucket.getWindowDuration().isNegative()) {
            throw new IllegalStateException(
                    "security.rate-limit." + bucketName + ".window-duration must be positive, got: " + bucket.getWindowDuration());
        }
    }

    public RateLimitingFilter(@Qualifier("openRateLimiter") RateLimiter openRateLimiter,
                              @Qualifier("closedRateLimiter") RateLimiter closedRateLimiter,
                              MeterRegistry meterRegistry,
                              ClientIpExtractor clientIpExtractor,
                              JwtTokenFromAuthHeaderExtractor jwtTokenFromAuthHeaderExtractor,
                              JwtClaimExtractor jwtClaimExtractor,
                              JwtBlacklistValidator jwtBlacklistValidator,
                              RateLimitProperties properties) {
        this.openRateLimiter = openRateLimiter;
        this.closedRateLimiter = closedRateLimiter;
        this.meterRegistry = meterRegistry;
        this.clientIpExtractor = clientIpExtractor;
        this.jwtTokenFromAuthHeaderExtractor = jwtTokenFromAuthHeaderExtractor;
        this.jwtClaimExtractor = jwtClaimExtractor;
        this.jwtBlacklistValidator = jwtBlacklistValidator;
        this.properties = properties;
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI();
        return "OPTIONS".equalsIgnoreCase(method)
                || path.startsWith(ApiPaths.ACTUATOR_ROOT)
                || path.startsWith(ApiPaths.DOCS_ROOT);
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String ip = clientIpExtractor.extract(request);

        if (isStrictPreAuthPath(request.getRequestURI())
                && isBlocked(request, response, "auth:ip:" + ip, RateLimitCategory.AUTH_PRE, properties.getAuth(), closedRateLimiter, "ip", ip)) {
            return;
        }

        if (isBlocked(request, response, "pre-auth:ip:" + ip, RateLimitCategory.PRE_AUTH, properties.getPreAuth(), openRateLimiter, "ip", ip)) {
            return;
        }

        RateLimitCategory category = resolvePrimaryCategory(request);
        Identity identity = resolveIdentity(request, ip);
        if (isBlocked(request, response, identity.key(category), category, bucketFor(category), openRateLimiter, identity.type(), ip)) {
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isBlocked(HttpServletRequest request,
                              HttpServletResponse response,
                              String key,
                              RateLimitCategory category,
                              Bucket bucket,
                              RateLimiter limiter,
                              String identityType,
                              String clientIp) throws IOException {
        RateLimitResult result = limiter.tryConsume(key, bucket.getMaxRequests(), bucket.getWindowDuration());
        RateLimitResponseWriter.writeRateLimitHeaders(response, result);

        if (!result.allowed()) {
            meterRegistry.counter("rate_limit.requests.blocked", "category", category.value()).increment();
            logExceeded(request, category, result, key, identityType, clientIp);
            RateLimitResponseWriter.writeTooManyRequests(response, result);
            return true;
        }

        meterRegistry.counter("rate_limit.requests.allowed", "category", category.value()).increment();
        return false;
    }

    private Bucket bucketFor(RateLimitCategory category) {
        return switch (category) {
            case AUTH, AUTH_PRE -> properties.getAuth();
            case SEARCH -> properties.getSearch();
            case TELEMETRY -> properties.getTelemetry();
            case PAYMENT -> properties.getPayment();
            case PRE_AUTH -> properties.getPreAuth();
            default -> properties.getGlobal();
        };
    }

    private RateLimitCategory resolvePrimaryCategory(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (isGlobalAuthPath(path)) {
            return RateLimitCategory.GLOBAL;
        }
        if (path.startsWith(AuthPaths.ROOT_PREFIX)) {
            return RateLimitCategory.AUTH;
        }
        if (path.equals(ApiPaths.PAYMENT) || path.startsWith(ApiPaths.PAYMENT + "/")) {
            return RateLimitCategory.PAYMENT;
        }
        if (path.equals(ApiPaths.PRODUCTS) && request.getParameter("keyword") != null) {
            return RateLimitCategory.SEARCH;
        }
        if (path.startsWith("/api/v1/telemetry/")) {
            return RateLimitCategory.TELEMETRY;
        }
        return RateLimitCategory.GLOBAL;
    }

    private boolean isStrictPreAuthPath(String path) {
        return path.equals(AuthPaths.AUTHENTICATE) || path.equals(AuthPaths.ROOT + "/register");
    }

    private boolean isGlobalAuthPath(String path) {
        return path.startsWith(AuthPaths.GOOGLE)
                || path.equals(AuthPaths.AUTHENTICATE)
                || path.equals(AuthPaths.ROOT + "/register");
    }

    private Identity resolveIdentity(HttpServletRequest request, String ip) {
        return resolveUserIdentity(request)
                .map(user -> new Identity("user", user))
                .orElseGet(() -> new Identity("ip", ip));
    }

    private Optional<String> resolveUserIdentity(HttpServletRequest request) {
        try {
            String token = jwtTokenFromAuthHeaderExtractor.extract(request);
            jwtBlacklistValidator.validate(token);
            return Optional.of(jwtClaimExtractor.extractEmail(token));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private void logExceeded(HttpServletRequest request,
                             RateLimitCategory category,
                             RateLimitResult result,
                             String rateLimitKey,
                             String identityType,
                             String clientIp) {
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

    private record Identity(String type, String value) {
        String key(RateLimitCategory category) {
            return category.value() + ":" + type + ":" + value;
        }
    }
}
