package com.zufar.icedlatte.security.crypto;

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

public class AesGcmStringProtector {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String CIPHER_ALGORITHM = "AES/GCM/NoPadding";
    private static final String KEY_ALGORITHM = "AES";
    private static final int IV_BYTES = 12;
    private static final int GCM_TAG_BITS = 128;

    private final SecretKey encryptionKey;
    private final String payloadDescription;

    public AesGcmStringProtector(String base64Secret, String payloadDescription) {
        this.encryptionKey = deriveEncryptionKey(base64Secret, payloadDescription);
        this.payloadDescription = payloadDescription;
    }

    public String protect(String plaintext) {
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
            throw new IllegalStateException("Failed to encrypt " + payloadDescription, e);
        }
    }

    public String unprotect(String encryptedPayload) {
        try {
            byte[] payload = Base64.getUrlDecoder().decode(encryptedPayload);
            if (payload.length <= IV_BYTES) {
                throw new IllegalArgumentException(payloadDescription + " payload is too short");
            }
            byte[] iv = Arrays.copyOfRange(payload, 0, IV_BYTES);
            byte[] ciphertext = Arrays.copyOfRange(payload, IV_BYTES, payload.length);
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, encryptionKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalStateException("Failed to decrypt " + payloadDescription, e);
        }
    }

    private static SecretKey deriveEncryptionKey(String base64Secret, String payloadDescription) {
        try {
            byte[] secretBytes = Base64.getDecoder().decode(base64Secret);
            byte[] keyBytes = MessageDigest.getInstance("SHA-256").digest(secretBytes);
            return new SecretKeySpec(keyBytes, KEY_ALGORITHM);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(payloadDescription + " encryption key must be valid Base64", e);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
