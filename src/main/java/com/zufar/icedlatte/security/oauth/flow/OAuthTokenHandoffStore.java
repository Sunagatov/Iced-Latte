package com.zufar.icedlatte.security.oauth.flow;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zufar.icedlatte.openapi.dto.UserAuthenticationResponse;
import com.zufar.icedlatte.security.jwt.config.JwtProperties;
import com.zufar.icedlatte.security.service.cache.ExpiringKeyValueStore;

@Component
public class OAuthTokenHandoffStore {

    private static final String KEY_PREFIX = "oauth:handoff:";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String CIPHER_ALGORITHM = "AES/GCM/NoPadding";
    private static final String KEY_ALGORITHM = "AES";
    private static final int IV_BYTES = 12;
    private static final int GCM_TAG_BITS = 128;

    private final ExpiringKeyValueStore temporaryStore;
    private final ObjectMapper objectMapper;
    private final SecretKey encryptionKey;

    @Value("${oauth.handoff-ttl:PT1M}")
    private Duration ttl;

    public OAuthTokenHandoffStore(
            ExpiringKeyValueStore temporaryStore,
            ObjectMapper objectMapper,
            JwtProperties jwtProperties,
            @Value("${oauth.handoff-encryption-key:}") String handoffEncryptionKey) {
        this.temporaryStore = temporaryStore;
        this.objectMapper = objectMapper;
        if (StringUtils.hasText(handoffEncryptionKey)) {
            this.encryptionKey = deriveEncryptionKey(handoffEncryptionKey);
        } else {
            this.encryptionKey = deriveEncryptionKey(jwtProperties.refreshSecret());
        }
    }

    public String store(UserAuthenticationResponse tokens) {
        validateConfiguredTtl();
        String code = newHandoffCode();
        temporaryStore.put(namespacedKey(code), encrypt(serialize(tokens)), ttl);
        return code;
    }

    public Optional<UserAuthenticationResponse> consume(String code) {
        return temporaryStore.take(namespacedKey(code)).map(this::decrypt).map(this::deserialize);
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

    private String encrypt(String plaintext) {
        try {
            byte[] iv = new byte[IV_BYTES];
            RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            ByteBuffer payload = ByteBuffer.allocate(iv.length + ciphertext.length);
            payload.put(iv);
            payload.put(ciphertext);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(payload.array());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to encrypt OAuth token handoff", e);
        }
    }

    private String decrypt(String encryptedPayload) {
        try {
            byte[] payload = Base64.getUrlDecoder().decode(encryptedPayload);
            if (payload.length <= IV_BYTES) {
                throw new IllegalArgumentException("OAuth token handoff payload is too short");
            }
            byte[] iv = Arrays.copyOfRange(payload, 0, IV_BYTES);
            byte[] ciphertext = Arrays.copyOfRange(payload, IV_BYTES, payload.length);
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, encryptionKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalStateException("Failed to decrypt OAuth token handoff", e);
        }
    }

    private void validateConfiguredTtl() {
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            throw new IllegalStateException("oauth.handoff-ttl must be positive");
        }
    }

    private static SecretKey deriveEncryptionKey(String refreshSecret) {
        try {
            byte[] secretBytes = Base64.getDecoder().decode(refreshSecret);
            byte[] keyBytes = MessageDigest.getInstance("SHA-256").digest(secretBytes);
            return new SecretKeySpec(keyBytes, KEY_ALGORITHM);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("JWT refresh secret must be valid Base64", e);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private static String namespacedKey(String code) {
        return KEY_PREFIX + code;
    }

    private record TokenPair(String token, String refreshToken) {}
}
