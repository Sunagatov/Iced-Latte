package com.zufar.icedlatte.security.ratelimit.filter;

import com.zufar.icedlatte.common.config.CaffeineSizeProperties;
import com.zufar.icedlatte.common.http.ApiPaths;
import com.zufar.icedlatte.common.exception.handler.ProblemTypeUriFactory;
import com.zufar.icedlatte.common.exception.ProblemType;
import com.zufar.icedlatte.common.util.ClientIpExtractor;
import com.zufar.icedlatte.security.configuration.AuthPaths;
import com.zufar.icedlatte.security.jwt.JwtTokenBlacklist;
import com.zufar.icedlatte.security.jwt.JwtBearerTokenResolver;
import com.zufar.icedlatte.security.jwt.JwtTokenClaims;
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
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final Set<String> READ_METHODS = Set.of("GET", "HEAD", "OPTIONS");

    private final RateLimiter openRateLimiter;
    private final RateLimiter closedRateLimiter;
    private final MeterRegistry meterRegistry;
    private final ClientIpExtractor clientIpExtractor;
    private final JwtBearerTokenResolver jwtBearerTokenResolver;
    private final JwtTokenClaims jwtTokenClaims;
    private final JwtTokenBlacklist jwtTokenBlacklist;
    private final RateLimitProperties properties;
    private final ProblemTypeUriFactory problemTypeUriFactory;

    private final Cache<String, Boolean> warnedKeys;

    /** Tracks how many times an IP has been blocked within the ban window. */
    private final Cache<String, AtomicInteger> blockCounts;

    @PostConstruct
    void validate() {
        assertPositive("pre-auth", properties.getPreAuth());
        assertPositive("auth", properties.getAuth());
        assertPositive("global", properties.getGlobal());
        assertPositive("search", properties.getSearch());
        assertPositive("telemetry", properties.getTelemetry());
        assertPositive("payment", properties.getPayment());
        assertPositive("write", properties.getWrite());
        assertPositive("file-upload", properties.getFileUpload());
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
                              JwtBearerTokenResolver jwtBearerTokenResolver,
                              JwtTokenClaims jwtTokenClaims,
                              JwtTokenBlacklist jwtTokenBlacklist,
                              RateLimitProperties properties,
                              ProblemTypeUriFactory problemTypeUriFactory,
                              CaffeineSizeProperties caffeineSizeProperties) {
        this.openRateLimiter = openRateLimiter;
        this.closedRateLimiter = closedRateLimiter;
        this.meterRegistry = meterRegistry;
        this.clientIpExtractor = clientIpExtractor;
        this.jwtBearerTokenResolver = jwtBearerTokenResolver;
        this.jwtTokenClaims = jwtTokenClaims;
        this.jwtTokenBlacklist = jwtTokenBlacklist;
        this.properties = properties;
        this.problemTypeUriFactory = problemTypeUriFactory;
        this.warnedKeys = Caffeine.newBuilder()
                .maximumSize(caffeineSizeProperties.rateLimitFilterSize())
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .build();
        this.blockCounts = Caffeine.newBuilder()
                .maximumSize(caffeineSizeProperties.rateLimitFilterSize())
                .expireAfterWrite(properties.getBanDuration())
                .build();
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

        // #7: Short-circuit ban for repeat offenders
        if (isBanned(ip)) {
            meterRegistry.counter("rate_limit.requests.banned").increment();
            RateLimitResult banResult = new RateLimitResult(false, 0, 0,
                    System.currentTimeMillis() + properties.getBanDuration().toMillis());
            RateLimitResponseWriter.writeTooManyRequests(response, banResult, problemTypeUriFactory.build(ProblemType.RATE_LIMITED));
            return;
        }

        if (isStrictPreAuthPath(request.getRequestURI())
                && isBlocked(request, response, "auth:ip:" + ip, RateLimitCategory.AUTH_PRE, properties.getAuth(), closedRateLimiter, "ip", ip)) {
            recordBlock(ip);
            return;
        }

        if (isBlocked(request, response, "pre-auth:ip:" + ip, RateLimitCategory.PRE_AUTH, properties.getPreAuth(), openRateLimiter, "ip", ip)) {
            recordBlock(ip);
            return;
        }

        RateLimitCategory category = resolvePrimaryCategory(request);
        Identity identity = resolveIdentity(request, ip);
        if (isBlocked(request, response, identity.key(category), category, bucketFor(category), openRateLimiter, identity.type(), ip)) {
            recordBlock(ip);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isBanned(String ip) {
        AtomicInteger count = blockCounts.getIfPresent(ip);
        return count != null && count.get() >= properties.getBanThreshold();
    }

    private void recordBlock(String ip) {
        blockCounts.get(ip, _ -> new AtomicInteger(0)).incrementAndGet();
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
            RateLimitResponseWriter.writeTooManyRequests(response, result, problemTypeUriFactory.build(ProblemType.RATE_LIMITED));
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
            case WRITE -> properties.getWrite();
            case FILE_UPLOAD -> properties.getFileUpload();
            case PRE_AUTH -> properties.getPreAuth();
            case GLOBAL -> properties.getGlobal();
        };
    }

    private RateLimitCategory resolvePrimaryCategory(HttpServletRequest request) {
        String path = request.getRequestURI();
        return switch (path) {
            case String uri when uri.startsWith(AuthPaths.ROOT_PREFIX) && !isGlobalAuthPath(uri) -> RateLimitCategory.AUTH;
            case String uri when uri.startsWith(ApiPaths.USERS_PASSWORD_RESET) -> RateLimitCategory.AUTH;
            case String uri when uri.equals(ApiPaths.PAYMENT) || uri.startsWith(ApiPaths.PAYMENT + "/") -> RateLimitCategory.PAYMENT;
            case String uri when uri.equals(ApiPaths.PRODUCTS) && request.getParameter("keyword") != null -> RateLimitCategory.SEARCH;
            case String uri when uri.startsWith("/api/v1/telemetry/") -> RateLimitCategory.TELEMETRY;
            case String uri when isFileUploadRequest(request, uri) -> RateLimitCategory.FILE_UPLOAD;
            case String _ when !READ_METHODS.contains(request.getMethod()) -> RateLimitCategory.WRITE;
            default -> RateLimitCategory.GLOBAL;
        };
    }

    private boolean isFileUploadRequest(HttpServletRequest request,
                                        String path) {
        String contentType = request.getContentType();
        return contentType != null
                && contentType.startsWith("multipart/")
                && (path.endsWith("/avatar") || path.contains("/images"));
    }

    // #2: Password reset is now a strict pre-auth path
    private boolean isStrictPreAuthPath(String path) {
        return path.equals(AuthPaths.AUTHENTICATE)
                || path.equals(AuthPaths.ROOT + "/register")
                || path.startsWith(ApiPaths.USERS_PASSWORD_RESET);
    }

    private boolean isGlobalAuthPath(String path) {
        return path.startsWith(AuthPaths.OAUTH + "/")
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
            String token = jwtBearerTokenResolver.extract(request);
            jwtTokenBlacklist.validateNotBlacklisted(token);
            return Optional.of(jwtTokenClaims.extractAccessTokenEmail(token));
        } catch (Exception _) {
            return Optional.empty();
        }
    }

    private void logExceeded(HttpServletRequest request,
                             RateLimitCategory category,
                             RateLimitResult result,
                             String rateLimitKey,
                             String identityType,
                             String clientIp) {
        long seconds = TimeUnit.MILLISECONDS.toSeconds(result.resetTimeMillis() - System.currentTimeMillis());
        long retryAfterSeconds = Math.max(1, seconds);

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
