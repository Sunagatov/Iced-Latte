package com.zufar.icedlatte.auth.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AuthorizationServerUrlCreator unit tests")
class AuthorizationServerUrlCreatorTest {

    private AuthorizationServerUrlCreator creator;

    @BeforeEach
    void setUp() {
        creator = new AuthorizationServerUrlCreator();
        creator.authorizationServerUrl = "https://accounts.google.com/o/oauth2/v2/auth";
        creator.clientId = "test-client-id";
        creator.scope = "email profile";
    }

    @Test
    @DisplayName("create returns URL containing all required OAuth2 parameters")
    void create_containsAllParams() {
        String url = creator.create();

        assertThat(url).startsWith("https://accounts.google.com/o/oauth2/v2/auth?");
        assertThat(url).contains("scope=email profile");
        assertThat(url).contains("access_type=offline");
        assertThat(url).contains("response_type=code");
        assertThat(url).contains("client_id=test-client-id");
        assertThat(url).contains("redirect_uri=https://iced-latte.uk/backend/api/v1/auth/google/callback");
        assertThat(url).contains("include_granted_scopes=true");
    }

    @Test
    @DisplayName("create URL changes when clientId changes")
    void create_differentClientId_differentUrl() {
        creator.clientId = "another-client";
        assertThat(creator.create()).contains("client_id=another-client");
    }
}
