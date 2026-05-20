package com.zufar.icedlatte.security.exception.signin;

import com.zufar.icedlatte.security.exception.AuthException;

public final class UserAccountLockedException extends AuthException {

    public UserAccountLockedException(int userAccountLockoutDurationMinutes) {
        super(String.format("Account temporarily locked due to too many failed login attempts. Try again in %d minutes or reset your password.", userAccountLockoutDurationMinutes));
    }
}
