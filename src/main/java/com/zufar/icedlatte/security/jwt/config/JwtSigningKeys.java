package com.zufar.icedlatte.security.jwt.config;

import java.util.Arrays;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtSigningKeys {

    private static final String DEFAULT_ACCESS_KEY_ID = "access-current";
    private static final String DEFAULT_REFRESH_KEY_ID = "refresh-current";
    private static final String DEFAULT_PREVIOUS_ACCESS_KEY_ID = "access-previous";
    private static final String DEFAULT_PREVIOUS_REFRESH_KEY_ID = "refresh-previous";

    private final SecretKey accessSigningKey;
    private final SecretKey refreshSigningKey;
    private final SecretKey previousAccessSigningKey;
    private final SecretKey previousRefreshSigningKey;
    private final String accessKeyId;
    private final String refreshKeyId;
    private final String previousAccessKeyId;
    private final String previousRefreshKeyId;

    public JwtSigningKeys(JwtProperties jwtProperties) {
        byte[] accessKeyBytes = Decoders.BASE64.decode(jwtProperties.secret());
        byte[] refreshKeyBytes = Decoders.BASE64.decode(jwtProperties.refreshSecret());
        if (Arrays.equals(accessKeyBytes, refreshKeyBytes)) {
            throw new IllegalStateException("JWT access and refresh signing keys must be different");
        }
        this.accessSigningKey = Keys.hmacShaKeyFor(accessKeyBytes);
        this.refreshSigningKey = Keys.hmacShaKeyFor(refreshKeyBytes);
        this.accessKeyId = normalizeKeyId(jwtProperties.accessKeyId(), DEFAULT_ACCESS_KEY_ID);
        this.refreshKeyId = normalizeKeyId(jwtProperties.refreshKeyId(), DEFAULT_REFRESH_KEY_ID);
        this.previousAccessSigningKey = decodeOptional(jwtProperties.previousSecret());
        this.previousRefreshSigningKey = decodeOptional(jwtProperties.previousRefreshSecret());
        this.previousAccessKeyId = previousAccessSigningKey == null
                ? null
                : normalizeKeyId(jwtProperties.previousAccessKeyId(), DEFAULT_PREVIOUS_ACCESS_KEY_ID);
        this.previousRefreshKeyId = previousRefreshSigningKey == null
                ? null
                : normalizeKeyId(jwtProperties.previousRefreshKeyId(), DEFAULT_PREVIOUS_REFRESH_KEY_ID);

        validateDistinctKeyIds();
    }

    public SecretKey get() {
        return accessSigningKey;
    }

    public SecretKey getRefresh() {
        return refreshSigningKey;
    }

    public SecretKey getPrevious() {
        return previousAccessSigningKey;
    }

    public SecretKey getPreviousRefresh() {
        return previousRefreshSigningKey;
    }

    public String getKeyId() {
        return accessKeyId;
    }

    public String getRefreshKeyId() {
        return refreshKeyId;
    }

    public SecretKey resolveAccessVerificationKey(String keyId) {
        return resolveVerificationKey(
                keyId, accessSigningKey, accessKeyId, previousAccessSigningKey, previousAccessKeyId, "access");
    }

    public SecretKey resolveRefreshVerificationKey(String keyId) {
        return resolveVerificationKey(
                keyId, refreshSigningKey, refreshKeyId, previousRefreshSigningKey, previousRefreshKeyId, "refresh");
    }

    private SecretKey resolveVerificationKey(
            String keyId,
            SecretKey currentKey,
            String currentKeyId,
            SecretKey previousKey,
            String previousKeyId,
            String tokenType) {
        if (!StringUtils.hasText(keyId) || currentKeyId.equals(keyId)) {
            return currentKey;
        }
        if (previousKey != null && previousKeyId.equals(keyId)) {
            return previousKey;
        }
        throw new IllegalArgumentException("Unknown " + tokenType + " JWT key id: " + keyId);
    }

    private void validateDistinctKeyIds() {
        if (accessKeyId.equals(refreshKeyId)) {
            throw new IllegalStateException("JWT access and refresh key ids must be different");
        }
        if (previousAccessKeyId != null && previousAccessKeyId.equals(accessKeyId)) {
            throw new IllegalStateException("JWT access key ids must be unique across rotations");
        }
        if (previousRefreshKeyId != null && previousRefreshKeyId.equals(refreshKeyId)) {
            throw new IllegalStateException("JWT refresh key ids must be unique across rotations");
        }
    }

    private static SecretKey decodeOptional(String encodedKey) {
        if (!StringUtils.hasText(encodedKey)) {
            return null;
        }
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(encodedKey));
    }

    private static String normalizeKeyId(String configuredKeyId, String fallbackKeyId) {
        return StringUtils.hasText(configuredKeyId) ? configuredKeyId : fallbackKeyId;
    }
}
