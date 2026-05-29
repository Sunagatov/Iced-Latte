package com.zufar.icedlatte.security.oauth.flow;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zufar.icedlatte.common.config.CaffeineSizeProperties;
import com.zufar.icedlatte.openapi.dto.UserAuthenticationResponse;
import com.zufar.icedlatte.security.service.cache.InMemoryExpiringKeyValueStore;

@DisplayName("OAuthTokenHandoffStore unit tests")
class OAuthTokenHandoffStoreTest {

    private OAuthTokenHandoffStore store;

    @BeforeEach
    void setUp() {
        store = new OAuthTokenHandoffStore(
                new InMemoryExpiringKeyValueStore(new CaffeineSizeProperties(1_000, 5_000, 10_000, 1_000, 10_000)),
                new ObjectMapper());
        ReflectionTestUtils.setField(store, "ttl", Duration.ofMinutes(1));
    }

    @Test
    @DisplayName("stores tokens behind one-time opaque code")
    void storesTokensBehindOneTimeOpaqueCode() {
        UserAuthenticationResponse tokens = new UserAuthenticationResponse();
        tokens.setToken("access-token");
        tokens.setRefreshToken("refresh-token");

        String code = store.store(tokens);

        assertThat(code).matches("[A-Za-z0-9_-]{43}");
        assertThat(store.consume(code)).hasValueSatisfying(result -> {
            assertThat(result.getToken()).isEqualTo("access-token");
            assertThat(result.getRefreshToken()).isEqualTo("refresh-token");
        });
        assertThat(store.consume(code)).isEmpty();
    }
}
