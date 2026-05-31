package com.zufar.icedlatte.security.signup.verification;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zufar.icedlatte.security.crypto.AesGcmStringProtector;
import com.zufar.icedlatte.security.jwt.config.JwtProperties;

@Component
class EmailTokenPayloadProtector {

    private static final String PAYLOAD_DESCRIPTION = "email token payload";

    private final ObjectMapper objectMapper;
    private final AesGcmStringProtector protector;

    EmailTokenPayloadProtector(
            ObjectMapper objectMapper, JwtProperties jwtProperties, EmailTokenProperties emailTokenProperties) {
        this.objectMapper = objectMapper;
        String keySource = StringUtils.hasText(emailTokenProperties.tokenEncryptionKey())
                ? emailTokenProperties.tokenEncryptionKey()
                : jwtProperties.refreshSecret();
        this.protector = new AesGcmStringProtector(keySource, PAYLOAD_DESCRIPTION);
    }

    String protect(EmailTokenPayload entry) {
        try {
            return protector.protect(objectMapper.writeValueAsString(entry));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize email token payload", e);
        }
    }

    <T extends EmailTokenPayload> T unprotect(String protectedEntry, Class<T> payloadType) {
        try {
            return objectMapper.readValue(protector.unprotect(protectedEntry), payloadType);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize email token payload", e);
        }
    }
}
