package com.zufar.icedlatte.ratelimit.filter;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

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

    private static final String UNKNOWN_IDENTITY = "unknown";

    private final RateLimiter openRateLimiter;
    private final RateLimiter closedRateLimiter;
    private final MeterRegistry meterRegistry;
    private final ClientIpExtractor clientIpExtractor;
    private final AuthenticatedRequestIdentityProvider authenticatedRequestIdentityProvider;
    private final RateLimitProperties properties;
    private final ProblemTypeUriFactory problemTypeUriFactory;
    private final RateLimitBanTracker banTracker;

    private final Cache<String, Boolean> warnedKeys;

    public RateLimitingFilter(
            @Qualifier("openRateLimiter") RateLimiter openRateLimiter,
            @Qualifier("closedRateLimiter") RateLimiter closedRateLimiter,
            MeterRegistry meterRegistry,
            ClientIpExtractor clientIpExtractor,
            AuthenticatedRequestIdentityProvider authenticatedRequestIdentityProvider,
            RateLimitProperties properties,
            ProblemTypeUriFactory problemTypeUriFactory,
            CaffeineSizeProperties caffeineSizeProperties) {
        RateLimitPropertiesValidator.validate(properties);
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
        String ip = normalizeIdentity(clientIpExtractor.extract(request));

        if (banTracker.isBanned(ip)) {
            meterRegistry.counter("rate_limit.requests.banned").increment();
            long resetTimeMillis =
                    System.currentTimeMillis() + properties.getBanDuration().toMillis();
            long banDuration = properties.getBanDuration().toSeconds();
            RateLimitResult banResult = new RateLimitResult(false, 0, 0, resetTimeMillis, banDuration);
            String problemTypeUri = problemTypeUriFactory.build(ProblemType.RATE_LIMITED);
            RateLimitResponseWriter.writeTooManyRequests(response, banResult, problemTypeUri);
            return;
        }

        Bucket auth = properties.getAuth();
        String key = "auth:ip:" + ip;
        RateLimitCategory authPre = RateLimitCategory.AUTH_PRE;
        if (RateLimitRouteClassifier.isStrictPreAuthPath(request.getRequestURI())
                && isBlocked(request, response, key, authPre, auth, closedRateLimiter, "ip", ip)) {
            banTracker.recordBlock(ip);
            return;
        }

        key = "pre-auth:ip:" + ip;
        Bucket preAuth = properties.getPreAuth();
        if (isBlocked(request, response, key, RateLimitCategory.PRE_AUTH, preAuth, openRateLimiter, "ip", ip)) {
            banTracker.recordBlock(ip);
            return;
        }

        RateLimitCategory category = RateLimitRouteClassifier.classify(request);
        Identity identity = resolveIdentity(request, ip);
        Bucket bucket = RateLimitBucketResolver.bucketFor(category, properties);
        key = identity.key(category);
        if (isBlocked(request, response, key, category, bucket, openRateLimiter, identity.type(), ip)) {
            banTracker.recordBlock(ip);
        } else {
            filterChain.doFilter(request, response);
        }
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

        if (result.allowed()) {
            meterRegistry
                    .counter("rate_limit.requests.allowed", "category", category.getValue())
                    .increment();
            return false;
        }
        meterRegistry
                .counter("rate_limit.requests.blocked", "category", category.getValue())
                .increment();
        logExceeded(request, category, result, key, identityType, clientIp);
        String problemTypeUri = problemTypeUriFactory.build(ProblemType.RATE_LIMITED);
        RateLimitResponseWriter.writeTooManyRequests(response, result, problemTypeUri);
        return true;
    }

    private Identity resolveIdentity(HttpServletRequest request, String ip) {
        return resolveUserIdentity(request)
                .map(user -> new Identity("user", user))
                .orElseGet(() -> new Identity("ip", ip));
    }

    private Optional<String> resolveUserIdentity(HttpServletRequest request) {
        return authenticatedRequestIdentityProvider
                .findIdentity(request)
                .map(RateLimitingFilter::sanitizeIdentity)
                .filter(identity -> !identity.isBlank());
    }

    private static String normalizeIdentity(String identity) {
        String normalized = sanitizeIdentity(identity);
        return normalized.isBlank() ? UNKNOWN_IDENTITY : normalized;
    }

    private static String sanitizeIdentity(String identity) {
        return ClientIpExtractor.sanitize(identity).trim();
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
        String categoryValue = category.getValue();
        String method = request.getMethod();
        String requestPath = ClientIpExtractor.sanitize(request.getRequestURI());

        boolean firstBlock = warnedKeys.getIfPresent(rateLimitKey) == null;
        if (!firstBlock) {
            String logMessage = "rate_limit.exceeded: category={}, identity_type={}, client_ip={}, method={}, path={},"
                    + " retry_after_seconds={}";
            log.debug(logMessage, categoryValue, identityType, clientIp, method, requestPath, retryAfterSeconds);
            return;
        }

        warnedKeys.put(rateLimitKey, Boolean.TRUE);
        String logMessage = "rate_limit.exceeded: category={}, identity_type={}, client_ip={}, method={}, path={},"
                + " retry_after_seconds={}, limit={}, remaining={}";
        int maxLimit = Math.max(0, result.remaining());
        log.warn(
                logMessage,
                categoryValue,
                identityType,
                clientIp,
                method,
                requestPath,
                retryAfterSeconds,
                result.limit(),
                maxLimit);
    }

    private record Identity(String type, String value) {
        String key(RateLimitCategory category) {
            return category.getValue() + ":" + type + ":" + value;
        }
    }
}
