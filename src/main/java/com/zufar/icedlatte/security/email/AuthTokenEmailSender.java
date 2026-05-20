package com.zufar.icedlatte.security.email;

public interface AuthTokenEmailSender {

    void sendTemporaryCode(String email, String message);
}
