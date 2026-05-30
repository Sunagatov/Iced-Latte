package com.zufar.icedlatte.security.oauth.flow;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zufar.icedlatte.security.crypto.AesGcmStringProtector;
import com.zufar.icedlatte.security.jwt.config.JwtProperties;
import com.zufar.icedlatte.security.service.cache.ExpiringKeyValueStore;
import com.zufar.icedlatte.security.session.token.AuthenticationTokens;

@Component
public class OAuthTokenHandoffStore {

    private static final String KEY_PREFIX = "oauth:handoff:";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String PAYLOAD_DESCRIPTION = "OAuth token handoff";

    private final ExpiringKeyValueStore temporaryStore;
    private final ObjectMapper objectMapper;
    private final AesGcmStringProtector protector;

    @Value("${oauth.handoff-ttl:PT1M}")
    private Duration ttl;

    public OAuthTokenHandoffStore(
            ExpiringKeyValueStore temporaryStore,
            ObjectMapper objectMapper,
            JwtProperties jwtProperties,
            @Value("${oauth.handoff-encryption-key:}") String handoffEncryptionKey) {
        this.temporaryStore = temporaryStore;
        this.objectMapper = objectMapper;
        String keySource;
        if (StringUtils.hasText(handoffEncryptionKey)) {
            keySource = handoffEncryptionKey;
        } else {
            keySource = jwtProperties.refreshSecret();
        }
        this.protector = new AesGcmStringProtector(keySource, PAYLOAD_DESCRIPTION);
    }

    public String store(AuthenticationTokens tokens) {
        validateConfiguredTtl();
        String code = newHandoffCode();
        temporaryStore.put(namespacedKey(code), protector.protect(serialize(tokens)), ttl);
        return code;
    }

    public Optional<AuthenticationTokens> consume(String code) {
        return temporaryStore
                .take(namespacedKey(code))
                .map(protector::unprotect)
                .map(this::deserialize);
    }

    private static String newHandoffCode() {
        byte[] randomBytes = new byte[32];
        RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private String serialize(AuthenticationTokens tokens) {
        try {
            return objectMapper.writeValueAsString(tokens);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize OAuth token handoff", e);
        }
    }

    private AuthenticationTokens deserialize(String raw) {
        try {
            return objectMapper.readValue(raw, AuthenticationTokens.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize OAuth token handoff", e);
        }
    }

    private void validateConfiguredTtl() {
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            throw new IllegalStateException("oauth.handoff-ttl must be positive");
        }
    }

    private static String namespacedKey(String code) {
        return KEY_PREFIX + code;
    }
}
