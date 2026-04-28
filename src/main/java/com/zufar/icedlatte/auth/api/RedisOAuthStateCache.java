package com.zufar.icedlatte.auth.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.data.redis.host")
public class RedisOAuthStateCache implements OAuthStateCache {

    private static final String KEY_PREFIX = "oauth:state:";

    private final RedisTemplate<String, String> redisTemplate;

    @Value("${oauth.state-ttl-minutes:10}")
    private int ttlMinutes;

    @Override
    public void store(String nonce,
                      String callbackBase) {
        redisTemplate.opsForValue()
                .set(KEY_PREFIX + nonce, callbackBase, Duration.ofMinutes(ttlMinutes));
    }

    @Override
    public String consume(String nonce) {
        return redisTemplate.opsForValue().getAndDelete(KEY_PREFIX + nonce);
    }
}
