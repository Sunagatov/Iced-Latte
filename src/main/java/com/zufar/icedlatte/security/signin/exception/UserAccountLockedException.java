package com.zufar.icedlatte.security.signin.exception;

public final class UserAccountLockedException extends AuthSecurityException {

    public UserAccountLockedException(int userAccountLockoutDurationMinutes) {
        super(String.format("Account temporarily locked due to too many failed login attempts. Try again in %d minutes or reset your password.", userAccountLockoutDurationMinutes));
    }
}
