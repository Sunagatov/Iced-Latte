package com.zufar.icedlatte.security.oauth.flow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zufar.icedlatte.common.config.CaffeineSizeProperties;
import com.zufar.icedlatte.security.jwt.config.JwtProperties;
import com.zufar.icedlatte.security.service.cache.ExpiringKeyValueStore;
import com.zufar.icedlatte.security.service.cache.InMemoryExpiringKeyValueStore;
import com.zufar.icedlatte.security.session.token.AuthenticationTokens;

@DisplayName("OAuthTokenHandoffStore unit tests")
class OAuthTokenHandoffStoreTest {

    private OAuthTokenHandoffStore store;

    @BeforeEach
    void setUp() {
        store = new OAuthTokenHandoffStore(
                new InMemoryExpiringKeyValueStore(new CaffeineSizeProperties(1_000, 5_000, 10_000, 1_000, 10_000)),
                new ObjectMapper(),
                jwtProperties(),
                properties(handoffEncryptionKey()));
    }

    @Test
    @DisplayName("stores tokens behind one-time opaque code")
    void storesTokensBehindOneTimeOpaqueCode() {
        AuthenticationTokens tokens = new AuthenticationTokens("access-token", "refresh-token");

        String code = store.store(tokens);

        assertThat(code).matches("[A-Za-z0-9_-]{43}");
        assertThat(store.consume(code)).hasValueSatisfying(result -> {
            assertThat(result.accessToken()).isEqualTo("access-token");
            assertThat(result.refreshToken()).isEqualTo("refresh-token");
        });
        assertThat(store.consume(code)).isEmpty();
    }

    @Test
    @DisplayName("does not store bearer tokens as plaintext")
    void doesNotStoreBearerTokensAsPlaintext() {
        ExpiringKeyValueStore temporaryStore = mock(ExpiringKeyValueStore.class);
        OAuthTokenHandoffStore encryptedStore = new OAuthTokenHandoffStore(
                temporaryStore, new ObjectMapper(), jwtProperties(), properties(handoffEncryptionKey()));
        AuthenticationTokens tokens = new AuthenticationTokens("access-token", "refresh-token");

        encryptedStore.store(tokens);

        verify(temporaryStore)
                .put(
                        startsWith("oauth:handoff:"),
                        argThat(value -> !value.contains("access-token") && !value.contains("refresh-token")),
                        eq(Duration.ofMinutes(1)));
    }

    private static JwtProperties jwtProperties() {
        return new JwtProperties(
                "Authorization",
                "NDA0RTYzNTI2NjU1NkE1ODZFMzI3MjM1NzUzODc4MkY0MTNBNDQ0Mjg0NzJCNEI2MjUwNjQ1MzY3NTY2QjU5NzA=",
                "NDA0RTYzNTI2NjU1NkE1ODZFMzI3MjM1NzUzODc4MkY0MTNBNDQ0Mjg0NzJCNEI2MjUwNjQ1MzY3NTY2QjU5NzA0MDRFNTM1MjY2NTU2QTU4NkUzMjcyMzU3NTM4NzgyRjQxM0E0NDQyODQ3MkI0QjYyNTA2NDUzNjc1NjZCNTk3MA==",
                Duration.ofMinutes(15),
                Duration.ofHours(24),
                "iced-latte",
                "iced-latte-client");
    }

    private static String handoffEncryptionKey() {
        return "NDA0RTYzNTI2NjU1NkE1ODZFMzI3MjM1NzUzODc4MkY0MTNBNDQ0Mjg0NzJCNEI2MjUwNjQ1MzY3NTY2QjU5NzA=";
    }

    private static OAuthFlowProperties properties(String handoffEncryptionKey) {
        return new OAuthFlowProperties(10, Duration.ofMinutes(1), handoffEncryptionKey);
    }
}
