package com.zufar.icedlatte.email.api.token;

import com.zufar.icedlatte.common.temporarycache.ExpiringKeyValueStore;
import com.zufar.icedlatte.email.exception.TimeTokenException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.OffsetDateTime;

@Component
@RequiredArgsConstructor
public class EmailTokenCooldownCache {

    private static final String KEY_PREFIX = "email:rate:";

    private final ExpiringKeyValueStore temporaryStore;

    @Value("${temporary-cache.time.token}")
    private int expireTimeMinutes;

    public void manageEmailSendingRate(String email) {
        Duration ttl = Duration.ofMinutes(expireTimeMinutes);
        temporaryStore.put(namespacedKey(email), OffsetDateTime.now().plus(ttl).toString(), ttl);
    }

    public void validateTimeToken(String email) {
        temporaryStore.get(namespacedKey(email), String.class)
                .map(OffsetDateTime::parse)
                .ifPresent(expiry -> {
                    throw new TimeTokenException(email, expiry);
                });
    }

    public void removeToken(String email) {
        temporaryStore.remove(namespacedKey(email));
    }

    private String namespacedKey(String email) {
        return KEY_PREFIX + email;
    }
}
