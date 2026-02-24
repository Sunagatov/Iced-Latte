package com.zufar.icedlatte.email.api.token;

import com.zufar.icedlatte.email.exception.TimeTokenException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.OffsetDateTime;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.data.redis.host")
public class RedisTokenTimeExpirationCache implements TokenTimeExpirationCache {

    private static final String KEY_PREFIX = "email:rate:";

    private final RedisTemplate<String, String> redisTemplate;

    @Value("${temporary-cache.time.token}")
    private int expireTimeMinutes;

    @Override
    public void manageEmailSendingRate(String email) {
        OffsetDateTime expiry = OffsetDateTime.now().plusMinutes(expireTimeMinutes);
        redisTemplate.opsForValue().set(KEY_PREFIX + email, expiry.toString(), Duration.ofMinutes(expireTimeMinutes));
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
