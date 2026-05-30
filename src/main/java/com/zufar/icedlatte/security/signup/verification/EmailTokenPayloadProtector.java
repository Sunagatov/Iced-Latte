package com.zufar.icedlatte.security.signup.verification;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zufar.icedlatte.security.jwt.config.JwtProperties;

@Component
public class EmailTokenPayloadProtector {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String CIPHER_ALGORITHM = "AES/GCM/NoPadding";
    private static final String KEY_ALGORITHM = "AES";
    private static final int IV_BYTES = 12;
    private static final int GCM_TAG_BITS = 128;

    private final ObjectMapper objectMapper;
    private final SecretKey encryptionKey;

    public EmailTokenPayloadProtector(
            ObjectMapper objectMapper,
            JwtProperties jwtProperties,
            @Value("${email.token-encryption-key:}") String tokenEncryptionKey) {
        this.objectMapper = objectMapper;
        String keySource = StringUtils.hasText(tokenEncryptionKey) ? tokenEncryptionKey : jwtProperties.refreshSecret();
        this.encryptionKey = deriveEncryptionKey(keySource);
    }

    public String protect(EmailTokenEntry entry) {
        try {
            return encrypt(objectMapper.writeValueAsString(entry));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize email token entry", e);
        }
    }

    public EmailTokenEntry unprotect(String protectedEntry) {
        try {
            return objectMapper.readValue(decrypt(protectedEntry), EmailTokenEntry.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize email token entry", e);
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
            throw new IllegalStateException("Failed to encrypt email token entry", e);
        }
    }

    private String decrypt(String encryptedPayload) {
        try {
            byte[] payload = Base64.getUrlDecoder().decode(encryptedPayload);
            if (payload.length <= IV_BYTES) {
                throw new IllegalArgumentException("Email token payload is too short");
            }
            byte[] iv = Arrays.copyOfRange(payload, 0, IV_BYTES);
            byte[] ciphertext = Arrays.copyOfRange(payload, IV_BYTES, payload.length);
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, encryptionKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalStateException("Failed to decrypt email token entry", e);
        }
    }

    private static SecretKey deriveEncryptionKey(String secret) {
        try {
            byte[] secretBytes = Base64.getDecoder().decode(secret);
            byte[] keyBytes = MessageDigest.getInstance("SHA-256").digest(secretBytes);
            return new SecretKeySpec(keyBytes, KEY_ALGORITHM);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Email token encryption key must be valid Base64", e);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
