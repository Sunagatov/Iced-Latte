package com.zufar.icedlatte.ratelimit.filter;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.zufar.icedlatte.common.config.CaffeineSizeProperties;
import com.zufar.icedlatte.common.exception.ProblemType;
import com.zufar.icedlatte.common.exception.handler.ProblemTypeUriFactory;
import com.zufar.icedlatte.common.util.ClientIpExtractor;
import com.zufar.icedlatte.ratelimit.api.AuthenticatedRequestIdentityProvider;
import com.zufar.icedlatte.ratelimit.api.RateLimitResult;
import com.zufar.icedlatte.ratelimit.api.RateLimiter;
import com.zufar.icedlatte.ratelimit.configuration.RateLimitProperties;
import com.zufar.icedlatte.ratelimit.configuration.RateLimitProperties.Bucket;
import com.zufar.icedlatte.ratelimit.dto.RateLimitCategory;
import com.zufar.icedlatte.ratelimit.util.RateLimitResponseWriter;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private final RateLimiter openRateLimiter;
    private final RateLimiter closedRateLimiter;
    private final MeterRegistry meterRegistry;
    private final ClientIpExtractor clientIpExtractor;
    private final AuthenticatedRequestIdentityProvider authenticatedRequestIdentityProvider;
    private final RateLimitProperties properties;
    private final ProblemTypeUriFactory problemTypeUriFactory;
    private final RateLimitBanTracker banTracker;

    private final Cache<String, Boolean> warnedKeys;

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
        assertPositiveBanConfiguration(properties);
    }

    private static void assertPositive(String bucketName, Bucket bucket) {
        if (bucket.getMaxRequests() <= 0) {
            throw new IllegalStateException(
                    "security.rate-limit." + bucketName + ".max-requests must be > 0, got: " + bucket.getMaxRequests());
        }
        if (bucket.getWindowDuration() == null
                || bucket.getWindowDuration().isZero()
                || bucket.getWindowDuration().isNegative()) {
            throw new IllegalStateException("security.rate-limit." + bucketName
                    + ".window-duration must be positive, got: " + bucket.getWindowDuration());
        }
    }

    public RateLimitingFilter(
            @Qualifier("openRateLimiter") RateLimiter openRateLimiter,
            @Qualifier("closedRateLimiter") RateLimiter closedRateLimiter,
            MeterRegistry meterRegistry,
            ClientIpExtractor clientIpExtractor,
            AuthenticatedRequestIdentityProvider authenticatedRequestIdentityProvider,
            RateLimitProperties properties,
            ProblemTypeUriFactory problemTypeUriFactory,
            CaffeineSizeProperties caffeineSizeProperties) {
        assertPositiveBanConfiguration(properties);
        this.openRateLimiter = openRateLimiter;
        this.closedRateLimiter = closedRateLimiter;
        this.meterRegistry = meterRegistry;
        this.clientIpExtractor = clientIpExtractor;
        this.authenticatedRequestIdentityProvider = authenticatedRequestIdentityProvider;
        this.properties = properties;
        this.problemTypeUriFactory = problemTypeUriFactory;
        this.banTracker = new RateLimitBanTracker(properties, caffeineSizeProperties);
        this.warnedKeys = Caffeine.newBuilder()
                .maximumSize(caffeineSizeProperties.rateLimitFilterSize())
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .build();
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        return RateLimitRouteClassifier.shouldSkip(request);
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        String ip = clientIpExtractor.extract(request);

        // #7: Short-circuit ban for repeat offenders
        if (banTracker.isBanned(ip)) {
            meterRegistry.counter("rate_limit.requests.banned").increment();
            RateLimitResult banResult = new RateLimitResult(
                    false,
                    0,
                    0,
                    System.currentTimeMillis() + properties.getBanDuration().toMillis(),
                    properties.getBanDuration().toSeconds());
            RateLimitResponseWriter.writeTooManyRequests(
                    response, banResult, problemTypeUriFactory.build(ProblemType.RATE_LIMITED));
            return;
        }

        if (RateLimitRouteClassifier.isStrictPreAuthPath(request.getRequestURI())
                && isBlocked(
                        request,
                        response,
                        "auth:ip:" + ip,
                        RateLimitCategory.AUTH_PRE,
                        properties.getAuth(),
                        closedRateLimiter,
                        "ip",
                        ip)) {
            banTracker.recordBlock(ip);
            return;
        }

        if (isBlocked(
                request,
                response,
                "pre-auth:ip:" + ip,
                RateLimitCategory.PRE_AUTH,
                properties.getPreAuth(),
                openRateLimiter,
                "ip",
                ip)) {
            banTracker.recordBlock(ip);
            return;
        }

        RateLimitCategory category = RateLimitRouteClassifier.classify(request);
        Identity identity = resolveIdentity(request, ip);
        if (isBlocked(
                request,
                response,
                identity.key(category),
                category,
                bucketFor(category),
                openRateLimiter,
                identity.type(),
                ip)) {
            banTracker.recordBlock(ip);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isBlocked(
            HttpServletRequest request,
            HttpServletResponse response,
            String key,
            RateLimitCategory category,
            Bucket bucket,
            RateLimiter limiter,
            String identityType,
            String clientIp)
            throws IOException {
        RateLimitResult result = limiter.tryConsume(key, bucket.getMaxRequests(), bucket.getWindowDuration());
        RateLimitResponseWriter.writeRateLimitHeaders(response, result);

        if (!result.allowed()) {
            meterRegistry
                    .counter("rate_limit.requests.blocked", "category", category.getValue())
                    .increment();
            logExceeded(request, category, result, key, identityType, clientIp);
            RateLimitResponseWriter.writeTooManyRequests(
                    response, result, problemTypeUriFactory.build(ProblemType.RATE_LIMITED));
            return true;
        }

        meterRegistry
                .counter("rate_limit.requests.allowed", "category", category.getValue())
                .increment();
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

    private static void assertPositiveBanConfiguration(RateLimitProperties properties) {
        if (properties.getBanThreshold() <= 0) {
            throw new IllegalStateException(
                    "security.rate-limit.ban-threshold must be > 0, got: " + properties.getBanThreshold());
        }
        if (properties.getBanDuration() == null
                || properties.getBanDuration().isZero()
                || properties.getBanDuration().isNegative()) {
            throw new IllegalStateException(
                    "security.rate-limit.ban-duration must be positive, got: " + properties.getBanDuration());
        }
    }

    private Identity resolveIdentity(HttpServletRequest request, String ip) {
        return resolveUserIdentity(request)
                .map(user -> new Identity("user", user))
                .orElseGet(() -> new Identity("ip", ip));
    }

    private Optional<String> resolveUserIdentity(HttpServletRequest request) {
        return authenticatedRequestIdentityProvider.findIdentity(request);
    }

    private void logExceeded(
            HttpServletRequest request,
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
            log.warn(
                    "rate_limit.exceeded: category={}, identity_type={}, client_ip={}, method={}, path={}, retry_after_seconds={}, limit={}, remaining={}",
                    category.getValue(),
                    identityType,
                    clientIp,
                    request.getMethod(),
                    ClientIpExtractor.sanitize(request.getRequestURI()),
                    retryAfterSeconds,
                    result.limit(),
                    Math.max(0, result.remaining()));
        } else {
            log.debug(
                    "rate_limit.exceeded: category={}, identity_type={}, client_ip={}, method={}, path={}, retry_after_seconds={}",
                    category.getValue(),
                    identityType,
                    clientIp,
                    request.getMethod(),
                    ClientIpExtractor.sanitize(request.getRequestURI()),
                    retryAfterSeconds);
        }
    }

    private record Identity(String type, String value) {
        String key(RateLimitCategory category) {
            return category.getValue() + ":" + type + ":" + value;
        }
    }
}
