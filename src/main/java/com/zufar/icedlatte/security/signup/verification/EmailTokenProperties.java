package com.zufar.icedlatte.security.signup.verification;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "email")
public record EmailTokenProperties(int verificationTokenLength, String tokenEncryptionKey) {

    public EmailTokenProperties {
        if (verificationTokenLength < 32) {
            throw new IllegalArgumentException("email.verification-token-length must be at least 32");
        }
    }
}
