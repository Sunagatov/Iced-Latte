package com.zufar.icedlatte.auth.api;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@ConditionalOnProperty(name = "google.enabled", havingValue = "true")
public class GoogleTokenExchanger {

    private final String redirectUri;
    private final GoogleIdTokenVerifier verifier;
    private final GoogleAuthorizationCodeFlow flow;

    public GoogleTokenExchanger(
            @Value("${google.client-id}") String clientId,
            @Value("${google.client-secret}") String clientSecret,
            @Value("${google.redirect-uri}") String redirectUri,
            @Value("${google.scope}") String scope) throws IOException {
        this.redirectUri = redirectUri;
        var transport = new NetHttpTransport();
        var json = GsonFactory.getDefaultInstance();
        this.verifier = new GoogleIdTokenVerifier.Builder(transport, json)
                .setAudience(Collections.singletonList(clientId))
                .build();
        this.flow = new GoogleAuthorizationCodeFlow.Builder(transport, json, clientId, clientSecret, List.of(scope))
                .setAccessType("offline")
                .build();
    }

    public GoogleIdToken.Payload exchange(String authorizationCode) throws GeneralSecurityException, IOException {
        var tokenResponse = flow.newTokenRequest(authorizationCode)
                .setRedirectUri(redirectUri)
                .execute();
        GoogleIdToken idToken = verifier.verify((String) tokenResponse.get("id_token"));
        if (idToken == null) {
            throw new IllegalStateException("Google ID token verification failed");
        }
        return idToken.getPayload();
    }
}
