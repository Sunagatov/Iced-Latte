package com.zufar.icedlatte.security.service.oauth;

import java.net.URI;

public interface OAuthProviderClient {

    OAuthProvider provider();

    URI buildAuthorizationUri(String state);

    OAuthProfile exchangeCode(String authorizationCode);
}
