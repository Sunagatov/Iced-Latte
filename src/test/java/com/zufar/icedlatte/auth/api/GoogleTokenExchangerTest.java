package com.zufar.icedlatte.auth.api;

import com.zufar.icedlatte.common.exception.UnauthorizedException;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.net.URI;
import java.security.GeneralSecurityException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GoogleTokenExchanger unit tests")
class GoogleTokenExchangerTest {

    @Mock private GoogleAuthorizationCodeFlow flow;
    @Mock private GoogleAuthorizationCodeTokenRequest tokenRequest;
    @Mock private GoogleIdTokenVerifier verifier;
    @Mock private GoogleTokenResponse tokenResponse;
    @Mock private GoogleIdToken idToken;

    @Test
    @DisplayName("buildAuthorizationUri includes Google OAuth parameters")
    void buildAuthorizationUriIncludesGoogleOauthParameters() {
        GoogleTokenExchanger exchanger = exchangerWithMocks();

        URI uri = exchanger.buildAuthorizationUri("state-token");

        assertThat(uri.toString())
                .contains("https://accounts.google.com/o/oauth2/v2/auth")
                .contains("scope=openid%20email%20profile")
                .contains("access_type=offline")
                .contains("response_type=code")
                .contains("redirect_uri=https://app.example.com/callback")
                .contains("client_id=client-id")
                .contains("state=state-token");
    }

    @Test
    @DisplayName("exchange sets the redirect URI, verifies the ID token, and returns its payload")
    void exchangeSetsRedirectUriVerifiesIdTokenAndReturnsPayload() throws GeneralSecurityException, IOException {
        GoogleTokenExchanger exchanger = exchangerWithMocks();
        GoogleIdToken.Payload payload = new GoogleIdToken.Payload();

        when(flow.newTokenRequest("auth-code")).thenReturn(tokenRequest);
        when(tokenRequest.setRedirectUri("https://app.example.com/callback")).thenReturn(tokenRequest);
        when(tokenRequest.execute()).thenReturn(tokenResponse);
        when(tokenResponse.get("id_token")).thenReturn("id-token");
        when(verifier.verify("id-token")).thenReturn(idToken);
        when(idToken.getPayload()).thenReturn(payload);

        GoogleIdToken.Payload result = exchanger.exchange("auth-code");

        assertThat(result).isSameAs(payload);
        verify(tokenRequest).setRedirectUri("https://app.example.com/callback");
        verify(verifier).verify("id-token");
    }

    @Test
    @DisplayName("exchangeCode maps Google payload to provider-neutral profile")
    void exchangeCodeMapsGooglePayloadToProviderNeutralProfile() throws GeneralSecurityException, IOException {
        GoogleTokenExchanger exchanger = exchangerWithMocks();
        GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
        payload.setSubject("google-subject");
        payload.setEmail("user@example.com");
        payload.setEmailVerified(true);
        payload.put("given_name", "Ada");
        payload.put("family_name", "Lovelace");

        when(flow.newTokenRequest("auth-code")).thenReturn(tokenRequest);
        when(tokenRequest.setRedirectUri("https://app.example.com/callback")).thenReturn(tokenRequest);
        when(tokenRequest.execute()).thenReturn(tokenResponse);
        when(tokenResponse.get("id_token")).thenReturn("id-token");
        when(verifier.verify("id-token")).thenReturn(idToken);
        when(idToken.getPayload()).thenReturn(payload);

        OAuthProfile profile = exchanger.exchangeCode("auth-code");

        assertThat(profile.providerSubject()).isEqualTo("google-subject");
        assertThat(profile.email()).isEqualTo("user@example.com");
        assertThat(profile.emailVerified()).isTrue();
        assertThat(profile.firstName()).isEqualTo("Ada");
        assertThat(profile.lastName()).isEqualTo("Lovelace");
    }

    @Test
    @DisplayName("exchange rejects unverifiable ID tokens")
    void exchangeRejectsUnverifiableIdTokens() throws GeneralSecurityException, IOException {
        GoogleTokenExchanger exchanger = exchangerWithMocks();

        when(flow.newTokenRequest("auth-code")).thenReturn(tokenRequest);
        when(tokenRequest.setRedirectUri("https://app.example.com/callback")).thenReturn(tokenRequest);
        when(tokenRequest.execute()).thenReturn(tokenResponse);
        when(tokenResponse.get("id_token")).thenReturn("id-token");
        when(verifier.verify("id-token")).thenReturn(null);

        assertThatThrownBy(() -> exchanger.exchange("auth-code"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Google authentication failed.");
    }

    private GoogleTokenExchanger exchangerWithMocks() {
        GoogleTokenExchanger exchanger = new GoogleTokenExchanger(
                "client-id",
                "client-secret",
                "https://app.example.com/callback",
                "https://accounts.google.com/o/oauth2/v2/auth",
                "openid email profile");
        ReflectionTestUtils.setField(exchanger, "flow", flow);
        ReflectionTestUtils.setField(exchanger, "verifier", verifier);
        return exchanger;
    }
}
