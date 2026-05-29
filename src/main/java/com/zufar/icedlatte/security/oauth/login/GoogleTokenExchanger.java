package com.zufar.icedlatte.security.oauth.login;

import java.io.IOException;
import java.net.URI;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.util.Collections;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.web.util.UriComponentsBuilder;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.zufar.icedlatte.common.exception.UnauthorizedException;
import com.zufar.icedlatte.security.oauth.config.GoogleOAuthProperties;
import com.zufar.icedlatte.security.oauth.config.OAuthProvider;
import com.zufar.icedlatte.security.oauth.dto.OAuthProfile;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@ConditionalOnProperty(name = "google.enabled", havingValue = "true")
public class GoogleTokenExchanger implements OAuthProviderClient {

    private final String authServerUrl;
    private final String clientId;
    private final String redirectUri;
    private final String scope;
    private final GoogleIdTokenVerifier verifier;
    private final GoogleAuthorizationCodeFlow flow;

    public GoogleTokenExchanger(GoogleOAuthProperties properties) {
        var transport = new NetHttpTransport.Builder().build();
        var json = GsonFactory.getDefaultInstance();
        Duration connectTimeout = properties.timeout().connectTimeout();
        Duration readTimeout = properties.timeout().readTimeout();

        this.authServerUrl = properties.auth().server().url();
        this.clientId = properties.clientId();
        this.redirectUri = properties.redirectUri();
        this.scope = properties.scope();
        this.verifier = new GoogleIdTokenVerifier.Builder(transport, json)
                .setAudience(Collections.singletonList(clientId))
                .build();

        List<String> scopes = List.of(scope.split("\\s+"));
        String clientSecret = properties.clientSecret();

        this.flow = new GoogleAuthorizationCodeFlow.Builder(transport, json, clientId, clientSecret, scopes)
                .setRequestInitializer(requestInitializer(connectTimeout, readTimeout))
                .setAccessType("offline")
                .build();
    }

    @Override
    public OAuthProvider provider() {
        return OAuthProvider.GOOGLE;
    }

    @Override
    public URI buildAuthorizationUri(String state) {
        return UriComponentsBuilder.fromUriString(authServerUrl)
                .queryParam("scope", scope)
                .queryParam("access_type", "offline")
                .queryParam("response_type", "code")
                .queryParam("redirect_uri", redirectUri)
                .queryParam("client_id", clientId)
                .queryParam("state", state)
                .build()
                .toUri();
    }

    @Override
    public OAuthProfile exchangeCode(String authorizationCode) {
        try {
            GoogleIdToken.Payload payload = exchange(authorizationCode);
            return new OAuthProfile(
                    payload.getSubject(),
                    payload.getEmail(),
                    Boolean.TRUE.equals(payload.getEmailVerified()),
                    (String) payload.get("given_name"),
                    (String) payload.get("family_name"));
        } catch (GeneralSecurityException | IOException _) {
            throw new UnauthorizedException("Google authentication failed.");
        }
    }

    public GoogleIdToken.Payload exchange(String authorizationCode) throws GeneralSecurityException, IOException {
        var tokenResponse = flow.newTokenRequest(authorizationCode)
                .setRedirectUri(redirectUri)
                .execute();
        Object rawIdToken = tokenResponse.get("id_token");
        if (!(rawIdToken instanceof String idTokenValue) || idTokenValue.isBlank()) {
            throw new UnauthorizedException("Google authentication failed.");
        }
        GoogleIdToken idToken = verifier.verify(idTokenValue);
        if (idToken == null) {
            throw new UnauthorizedException("Google authentication failed.");
        }
        return idToken.getPayload();
    }

    static HttpRequestInitializer requestInitializer(Duration connectTimeout, Duration readTimeout) {
        int connectTimeoutMillis = toPositiveMillis(connectTimeout, "connectTimeout");
        int readTimeoutMillis = toPositiveMillis(readTimeout, "readTimeout");
        return request -> applyTimeouts(request, connectTimeoutMillis, readTimeoutMillis);
    }

    private static void applyTimeouts(HttpRequest request, int connectTimeoutMillis, int readTimeoutMillis) {
        request.setConnectTimeout(connectTimeoutMillis);
        request.setReadTimeout(readTimeoutMillis);
    }

    private static int toPositiveMillis(Duration duration, String name) {
        Assert.notNull(duration, name + " must not be null");
        if (duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        long millis = duration.toMillis();
        if (millis > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(name + " is too large");
        }
        return (int) millis;
    }
}
