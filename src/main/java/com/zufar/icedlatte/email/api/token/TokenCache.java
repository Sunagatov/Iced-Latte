package com.zufar.icedlatte.email.api.token;

import com.zufar.icedlatte.openapi.dto.UserRegistrationRequest;

public interface TokenCache {

    void addToken(String tokenKey,
                  UserRegistrationRequest request);

    UserRegistrationRequest getToken(String tokenKey);

    void removeToken(String tokenKey);
}
