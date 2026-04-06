package com.zufar.icedlatte.email.api.token;

import com.zufar.icedlatte.openapi.dto.UserRegistrationRequest;

public interface TokenCache {

    void addToken(String tokenKey,
                  UserRegistrationRequest request,
                  TokenPurpose purpose);

    UserRegistrationRequest getToken(String tokenKey,
                                     TokenPurpose expectedPurpose);

    void removeToken(String tokenKey);
}
