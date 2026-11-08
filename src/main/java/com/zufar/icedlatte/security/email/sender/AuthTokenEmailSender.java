package com.zufar.icedlatte.security.email.sender;

public interface AuthTokenEmailSender {

    default void sendPasswordReset(String email, String token) {
        sendTemporaryCode(email, token);
    }

    default void sendPasswordChanged(String email) {}

    void sendTemporaryCode(String email, String message);
}
