package com.zufar.icedlatte.security.oauth.login;

import com.zufar.icedlatte.security.oauth.config.OAuthProvider;
import com.zufar.icedlatte.security.oauth.dto.OAuthProfile;

import java.net.URI;

public interface OAuthProviderClient {

    OAuthProvider provider();

    URI buildAuthorizationUri(String state);

    OAuthProfile exchangeCode(String authorizationCode);
}
