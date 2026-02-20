package com.zufar.icedlatte.security.exception;

public class UserAccountLockedException extends RuntimeException {

    public UserAccountLockedException(int userAccountLockoutDurationMinutes) {
        super(String.format("Account temporarily locked due to too many failed login attempts. Try again in %d minutes or reset your password.", userAccountLockoutDurationMinutes));
    }
}
