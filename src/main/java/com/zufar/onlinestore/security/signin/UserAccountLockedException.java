package com.zufar.onlinestore.security.signin;

import org.springframework.security.authentication.LockedException;

public class UserAccountLockedException extends RuntimeException {

    public UserAccountLockedException(String email, LockedException exception) {
        super(String.format("User account with the email='%s' is locked.", email), exception);
    }
}
