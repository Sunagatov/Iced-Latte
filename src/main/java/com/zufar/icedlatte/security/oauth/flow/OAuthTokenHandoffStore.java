package com.zufar.icedlatte.security.oauth.flow;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;
import java.util.regex.Pattern;

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
    private static final Pattern HANDOFF_CODE_PATTERN = Pattern.compile("[A-Za-z0-9_-]{43}");

    private final ExpiringKeyValueStore temporaryStore;
    private final ObjectMapper objectMapper;
    private final AesGcmStringProtector protector;
    private final OAuthFlowProperties properties;

    public OAuthTokenHandoffStore(
            ExpiringKeyValueStore temporaryStore,
            ObjectMapper objectMapper,
            JwtProperties jwtProperties,
            OAuthFlowProperties properties) {
        this.temporaryStore = temporaryStore;
        this.objectMapper = objectMapper;
        this.properties = properties;
        String keySource;
        if (StringUtils.hasText(properties.handoffEncryptionKey())) {
            keySource = properties.handoffEncryptionKey();
        } else {
            keySource = jwtProperties.refreshSecret();
        }
        this.protector = new AesGcmStringProtector(keySource, PAYLOAD_DESCRIPTION);
    }

    public String store(AuthenticationTokens tokens) {
        String code = newHandoffCode();
        temporaryStore.put(namespacedKey(code), protector.protect(serialize(tokens)), properties.handoffTtl());
        return code;
    }

    public Optional<AuthenticationTokens> consume(String code) {
        if (!isValidHandoffCode(code)) {
            return Optional.empty();
        }
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

    private static String namespacedKey(String code) {
        return KEY_PREFIX + code;
    }

    private static boolean isValidHandoffCode(String code) {
        return code != null && HANDOFF_CODE_PATTERN.matcher(code).matches();
    }
}
