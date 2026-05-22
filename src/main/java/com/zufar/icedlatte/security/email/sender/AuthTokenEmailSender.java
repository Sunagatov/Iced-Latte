package com.zufar.icedlatte.security.email.sender;

public interface AuthTokenEmailSender {

    void sendTemporaryCode(String email, String message);
}
