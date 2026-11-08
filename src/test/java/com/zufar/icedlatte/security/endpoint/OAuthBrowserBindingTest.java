package com.zufar.icedlatte.security.endpoint;

import static io.restassured.RestAssured.given;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.zufar.icedlatte.security.oauth.config.OAuthProvider;
import com.zufar.icedlatte.security.oauth.dto.OAuthProfile;
import com.zufar.icedlatte.security.oauth.flow.OAuthTokenHandoffStore;
import com.zufar.icedlatte.security.oauth.login.OAuthProviderClient;
import com.zufar.icedlatte.security.oauth.login.OAuthProviderClientRegistry;
import com.zufar.icedlatte.test.config.AuthenticatedUserIntegrationSupport;

class OAuthBrowserBindingTest extends AuthenticatedUserIntegrationSupport {
    @MockitoBean
    OAuthProviderClientRegistry providers;

    @Test
    void onlyTheInitiatingBrowserCanRedeemTheHandoff() {
        String subject = UUID.randomUUID().toString();
        when(providers.findClient(OAuthProvider.GOOGLE)).thenReturn(Optional.of(new OAuthProviderClient() {
            public OAuthProvider provider() {
                return OAuthProvider.GOOGLE;
            }

            public URI buildAuthorizationUri(String state) {
                return URI.create("https://provider.example/authorize?state=" + state);
            }

            public OAuthProfile exchangeCode(String code) {
                return new OAuthProfile(subject, subject + "@example.com", true, "Test", "User");
            }
        }));
        String verifier = "B".repeat(43);
        var start = given(jsonSpec("/api/v1/auth"))
                .redirects()
                .follow(false)
                .queryParam("codeChallenge", OAuthTokenHandoffStore.challenge(verifier))
                .get("/oauth/google")
                .then()
                .statusCode(302)
                .extract()
                .response();
        String state = URI.create(start.header("Location")).getQuery().substring("state=".length());
        var callback = given(jsonSpec("/api/v1/auth"))
                .redirects()
                .follow(false)
                .cookies(start.cookies())
                .queryParam("state", state)
                .queryParam("code", "valid-provider-code")
                .get("/oauth/google/callback")
                .then()
                .statusCode(302)
                .extract()
                .response();
        String handoff = URI.create(callback.header("Location")).getFragment().substring("oauthCode=".length());
        given(jsonSpec("/api/v1/auth"))
                .queryParam("code", handoff)
                .post("/oauth/token")
                .then()
                .statusCode(400);
        given(jsonSpec("/api/v1/auth"))
                .queryParam("code", handoff)
                .header("X-OAuth-Verifier", "C".repeat(43))
                .post("/oauth/token")
                .then()
                .statusCode(400);
        var tokens = given(jsonSpec("/api/v1/auth"))
                .queryParam("code", handoff)
                .header("X-OAuth-Verifier", verifier)
                .post("/oauth/token")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath();
        given(authenticatedJsonSpec("/api/v1", tokens.getString("token")))
                .get("/users")
                .then()
                .statusCode(200);
        given(jsonSpec("/api/v1/auth"))
                .queryParam("code", handoff)
                .header("X-OAuth-Verifier", verifier)
                .post("/oauth/token")
                .then()
                .statusCode(400);
    }
}
