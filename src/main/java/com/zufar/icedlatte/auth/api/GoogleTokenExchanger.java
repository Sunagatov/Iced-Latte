package com.zufar.icedlatte.auth.api;

import com.zufar.icedlatte.common.exception.UnauthorizedException;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;

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

    public GoogleTokenExchanger(@Value("${google.client-id}") String clientId,
                                @Value("${google.client-secret}") String clientSecret,
                                @Value("${google.redirect-uri}") String redirectUri,
                                @Value("${google.auth.server.url}") String authServerUrl,
                                @Value("${google.scope}") String scope) {
        this.authServerUrl = authServerUrl;
        this.clientId = clientId;
        this.redirectUri = redirectUri;
        this.scope = scope;
        var transport = new NetHttpTransport.Builder().build();
        var json = GsonFactory.getDefaultInstance();
        this.verifier = new GoogleIdTokenVerifier.Builder(transport, json)
                .setAudience(Collections.singletonList(clientId))
                .build();
        this.flow = new GoogleAuthorizationCodeFlow.Builder(transport, json, clientId, clientSecret, List.of(scope.split("\\s+")))
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
                    (String) payload.get("family_name")
            );
        } catch (GeneralSecurityException | IOException e) {
            throw new UnauthorizedException("Google authentication failed.");
        }
    }

    GoogleIdToken.Payload exchange(String authorizationCode) throws GeneralSecurityException, IOException {
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
}
