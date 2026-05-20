package com.zufar.icedlatte.security.service.email;

public interface AuthTokenEmailSender {

    void sendTemporaryCode(String email, String message);
}
