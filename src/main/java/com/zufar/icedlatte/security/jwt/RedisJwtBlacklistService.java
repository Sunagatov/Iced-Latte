package com.zufar.icedlatte.security.jwt;

import com.zufar.icedlatte.security.configuration.JwtProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;


@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.data.redis.host")
public class RedisJwtBlacklistService implements JwtBlacklistService {

    private static final String BLACKLIST_KEY_PREFIX = "jwt:blacklist:";
    private static final String BLACKLIST_VALUE = "revoked";

    private final RedisTemplate<String, String> redisTemplate;
    private final JwtProperties jwtProperties;

    @Retryable(retryFor = DataAccessException.class, backoff = @Backoff(delay = 100))
    public void blacklistToken(String token) {
        if (!StringUtils.hasText(token)) {
            log.warn("Attempted to blacklist empty token");
            return;
        }
        String key = buildBlacklistKey(token);
        redisTemplate.opsForValue().set(key, BLACKLIST_VALUE, jwtProperties.expiration());
        log.debug("Token blacklisted successfully with TTL: {} seconds", jwtProperties.expiration().toSeconds());
    }

    public boolean isBlacklisted(String token) {
        if (!StringUtils.hasText(token)) {
            log.warn("Token validation attempted with empty token");
            return true;
        }
        String key = buildBlacklistKey(token);
        try {
            Boolean hasKey = redisTemplate.hasKey(key);
            //noinspection ConstantConditions - hasKey is @Nullable per Spring's RedisOperations contract
            boolean isBlacklisted = hasKey != null && hasKey;
            log.debug("Token blacklist check: {} - {}", key.substring(0, Math.min(key.length(), 50)),
                    isBlacklisted ? "BLACKLISTED" : "VALID");
            // amazonq-ignore-next-line
            return isBlacklisted;
        } catch (RuntimeException ex) {
            log.error("Redis isBlacklisted failed - failing secure", ex);
            return true;
        }
    }

    private String buildBlacklistKey(String token) {
        return BLACKLIST_KEY_PREFIX + sha256(token);
    }
}
