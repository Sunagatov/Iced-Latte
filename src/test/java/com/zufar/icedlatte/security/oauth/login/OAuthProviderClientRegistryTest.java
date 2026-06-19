package com.zufar.icedlatte.security.oauth.login;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.zufar.icedlatte.security.oauth.config.OAuthProvider;
import com.zufar.icedlatte.security.oauth.dto.OAuthProfile;

class OAuthProviderClientRegistryTest {

    @Test
    void findsRegisteredProviderClient() {
        OAuthProviderClient googleClient = client(OAuthProvider.GOOGLE);
        OAuthProviderClientRegistry registry = new OAuthProviderClientRegistry(List.of(googleClient));

        assertThat(registry.findClient(OAuthProvider.GOOGLE)).containsSame(googleClient);
        assertThat(registry.findClient(OAuthProvider.GITHUB)).isEmpty();
    }

    @Test
    void allowsNoConfiguredProviderClients() {
        OAuthProviderClientRegistry registry = new OAuthProviderClientRegistry(List.of());

        assertThat(registry.findClient(OAuthProvider.GOOGLE)).isEmpty();
    }

    @Test
    void returnsEmptyWhenProviderIsNull() {
        OAuthProviderClientRegistry registry = new OAuthProviderClientRegistry(List.of(client(OAuthProvider.GOOGLE)));

        assertThat(registry.findClient(null)).isEmpty();
    }

    @Test
    void rejectsDuplicateProviderClients() {
        OAuthProviderClient firstGoogleClient = client(OAuthProvider.GOOGLE);
        OAuthProviderClient secondGoogleClient = client(OAuthProvider.GOOGLE);

        assertThatThrownBy(() -> new OAuthProviderClientRegistry(List.of(firstGoogleClient, secondGoogleClient)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Multiple OAuth clients registered for provider: google");
    }

    private static OAuthProviderClient client(OAuthProvider provider) {
        return new OAuthProviderClient() {
            @Override
            public OAuthProvider provider() {
                return provider;
            }

            @Override
            public URI buildAuthorizationUri(String state) {
                return URI.create("https://oauth.test/" + provider.id() + "?state=" + state);
            }

            @Override
            public OAuthProfile exchangeCode(String authorizationCode) {
                return new OAuthProfile("subject", "user@example.com", true, "OAuth", "User");
            }
        };
    }
}
