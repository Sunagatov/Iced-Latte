package com.zufar.icedlatte.email.sender;

public interface AuthTokenEmailSender {

    void sendTemporaryCode(String email, String message);
}
