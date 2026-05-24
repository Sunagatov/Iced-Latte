package com.zufar.icedlatte.security.oauth.login;

import java.net.URI;

import com.zufar.icedlatte.security.oauth.config.OAuthProvider;
import com.zufar.icedlatte.security.oauth.dto.OAuthProfile;

public interface OAuthProviderClient {

    OAuthProvider provider();

    URI buildAuthorizationUri(String state);

    OAuthProfile exchangeCode(String authorizationCode);
}
