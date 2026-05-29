package com.zufar.icedlatte.security.oauth.flow;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zufar.icedlatte.openapi.dto.UserAuthenticationResponse;
import com.zufar.icedlatte.security.service.cache.ExpiringKeyValueStore;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OAuthTokenHandoffStore {

    private static final String KEY_PREFIX = "oauth:handoff:";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final ExpiringKeyValueStore temporaryStore;
    private final ObjectMapper objectMapper;

    @Value("${oauth.handoff-ttl:PT1M}")
    private Duration ttl;

    public String store(UserAuthenticationResponse tokens) {
        String code = newHandoffCode();
        temporaryStore.put(namespacedKey(code), serialize(tokens), ttl);
        return code;
    }

    public Optional<UserAuthenticationResponse> consume(String code) {
        return temporaryStore.take(namespacedKey(code)).map(this::deserialize);
    }

    private static String newHandoffCode() {
        byte[] randomBytes = new byte[32];
        RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private String serialize(UserAuthenticationResponse tokens) {
        try {
            TokenPair tokenPair = new TokenPair(tokens.getToken(), tokens.getRefreshToken());
            return objectMapper.writeValueAsString(tokenPair);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize OAuth token handoff", e);
        }
    }

    private UserAuthenticationResponse deserialize(String raw) {
        try {
            TokenPair tokenPair = objectMapper.readValue(raw, TokenPair.class);
            UserAuthenticationResponse response = new UserAuthenticationResponse();
            response.setToken(tokenPair.token());
            response.setRefreshToken(tokenPair.refreshToken());
            return response;
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize OAuth token handoff", e);
        }
    }

    private static String namespacedKey(String code) {
        return KEY_PREFIX + code;
    }

    private record TokenPair(String token, String refreshToken) {}
}
