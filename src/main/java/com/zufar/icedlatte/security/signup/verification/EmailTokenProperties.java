package com.zufar.icedlatte.security.signup.verification;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "email")
public record EmailTokenProperties(Integer verificationTokenLength, String tokenEncryptionKey) {

    public EmailTokenProperties {
        if (verificationTokenLength == null) {
            throw new IllegalArgumentException("email.verification-token-length must not be null");
        }
        if (verificationTokenLength < 32) {
            throw new IllegalArgumentException("email.verification-token-length must be at least 32");
        }
    }
}
