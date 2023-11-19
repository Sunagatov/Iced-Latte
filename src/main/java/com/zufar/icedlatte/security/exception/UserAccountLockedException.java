package com.zufar.icedlatte.security.exception;

public class UserAccountLockedException extends RuntimeException {

    public UserAccountLockedException(String email, int userAccountLockoutDurationMinutes) {
        super(String.format("The request was rejected due to an incorrect number of login attempts for the user with email='%s'. " +
                "Try again in %s minutes or reset your password", email, userAccountLockoutDurationMinutes));
    }
}
