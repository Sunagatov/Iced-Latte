package com.zufar.icedlatte.email.api.token;

import com.zufar.icedlatte.common.exception.BadRequestException;
import com.zufar.icedlatte.common.temporarycache.ExpiringKeyValueStore;
import com.zufar.icedlatte.openapi.dto.UserRegistrationRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class EmailTokenCache implements TokenCache {

    private static final String KEY_PREFIX = "email:token:";

    private final ExpiringKeyValueStore temporaryStore;

    @Value("${temporary-cache.time.token}")
    private int expireTimeMinutes;

    @Override
    public void addToken(String tokenKey,
                         UserRegistrationRequest request,
                         TokenPurpose purpose) {
        temporaryStore.put(namespacedKey(tokenKey),
                new TokenEntry(request, purpose),
                Duration.ofMinutes(expireTimeMinutes));
    }

    @Override
    public UserRegistrationRequest getToken(String tokenKey,
                                            TokenPurpose expectedPurpose) {
        TokenEntry entry = temporaryStore.get(namespacedKey(tokenKey), TokenEntry.class)
                .orElseThrow(() -> new BadRequestException("Incorrect token"));
        if (entry.purpose() != expectedPurpose) {
            throw new BadRequestException("Incorrect token");
        }
        return entry.request();
    }

    @Override
    public void removeToken(String tokenKey) {
        temporaryStore.remove(namespacedKey(tokenKey));
    }

    private String namespacedKey(String tokenKey) {
        return KEY_PREFIX + tokenKey;
    }

    private record TokenEntry(UserRegistrationRequest request, TokenPurpose purpose) { }
}
