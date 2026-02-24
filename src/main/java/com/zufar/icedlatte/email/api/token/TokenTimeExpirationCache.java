package com.zufar.icedlatte.email.api.token;

public interface TokenTimeExpirationCache {
    void manageEmailSendingRate(String email);
    void validateTimeToken(String email);
    void removeToken(String email);
}
