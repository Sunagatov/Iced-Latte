package com.zufar.icedlatte.security.signup.verification;

public record EmailVerificationTokenPayload(String email, EmailRegistrationPayload registration, String encodedPassword)
        implements EmailTokenPayload {}
