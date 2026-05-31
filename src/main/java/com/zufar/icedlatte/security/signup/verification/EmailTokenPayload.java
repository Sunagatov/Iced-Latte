package com.zufar.icedlatte.security.signup.verification;

public sealed interface EmailTokenPayload permits EmailVerificationTokenPayload, PasswordResetTokenPayload {

    String email();
}
