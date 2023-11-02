package com.zufar.onlinestore.security.exception;

public class UserAccountLockedException extends RuntimeException {

    public UserAccountLockedException(String email) {
        super(String.format("User account with the email='%s' have been locked out due to multiple failed login attempts", email));
    }
}
