package com.zufar.icedlatte.security.signup.verification;

public record PasswordResetTokenPayload(String email) implements EmailTokenPayload {}
