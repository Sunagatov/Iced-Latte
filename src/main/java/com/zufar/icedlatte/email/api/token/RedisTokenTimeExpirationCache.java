package com.zufar.icedlatte.email.api.token;

import com.zufar.icedlatte.email.exception.TimeTokenException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.OffsetDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.data.redis.host")
public class RedisTokenTimeExpirationCache implements TokenTimeExpirationCache {

    private static final String KEY_PREFIX = "email:rate:";

    private final RedisTemplate<String, String> redisTemplate;

    @Value("${temporary-cache.time.token}")
    private int expireTimeMinutes;

    @PostConstruct
    void init() { log.info("token_expiration_cache.mode: Redis"); }

    @Override
    public void manageEmailSendingRate(String email) {
        OffsetDateTime expiry = OffsetDateTime.now()
                .plusMinutes(expireTimeMinutes);
        String key = KEY_PREFIX + email;
        String value = expiry.toString();
        Duration timeout = Duration.ofMinutes(expireTimeMinutes);
        redisTemplate.opsForValue()
                .set(key, value, timeout);
    }

    @Override
    public void validateTimeToken(String email) {
        String value = redisTemplate.opsForValue().get(KEY_PREFIX + email);
        if (value != null) throw new TimeTokenException(email, OffsetDateTime.parse(value));
    }

    @Override
    public void removeToken(String email) {
        redisTemplate.delete(KEY_PREFIX + email);
    }
}
