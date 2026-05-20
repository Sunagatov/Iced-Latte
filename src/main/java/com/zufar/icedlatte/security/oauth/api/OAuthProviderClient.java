package com.zufar.icedlatte.security.oauth.api;

import com.zufar.icedlatte.security.oauth.dto.OAuthProfile;

import java.net.URI;

public interface OAuthProviderClient {

    OAuthProvider provider();

    URI buildAuthorizationUri(String state);

    OAuthProfile exchangeCode(String authorizationCode);
}
