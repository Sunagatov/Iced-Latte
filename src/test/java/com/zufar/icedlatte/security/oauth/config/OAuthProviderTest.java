package com.zufar.icedlatte.security.oauth.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class OAuthProviderTest {

    @Test
    void resolvesGithubFromProviderId() {
        assertThat(OAuthProvider.fromId("github")).contains(OAuthProvider.GITHUB);
        assertThat(OAuthProvider.GITHUB.callbackPath()).isEqualTo("/auth/github/callback");
    }
}
