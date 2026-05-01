package com.zufar.icedlatte.security.jwt;

import com.zufar.icedlatte.common.temporarycache.ExpiringKeyValueStore;
import com.zufar.icedlatte.security.configuration.JwtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtBlacklistStore unit tests")
class JwtBlacklistStoreTest {

    @Mock private ExpiringKeyValueStore temporaryStore;
    @Mock private JwtProperties jwtProperties;

    private JwtBlacklistStore service;

    private static final String TOKEN = "test.jwt.token";
    private static final Duration TTL = Duration.ofHours(1);

    @BeforeEach
    void setUp() {
        service = new JwtBlacklistStore(temporaryStore, jwtProperties);
    }

    @Test
    @DisplayName("blacklistToken stores the hashed token with JWT TTL")
    void blacklistTokenStoresHashedTokenWithJwtTtl() {
        when(jwtProperties.expiration()).thenReturn(TTL);

        service.blacklistToken(TOKEN);

        verify(temporaryStore).put(eq("jwt:blacklist:" + service.sha256(TOKEN)), eq(Boolean.TRUE), eq(TTL));
    }

    @Test
    @DisplayName("blacklistToken ignores blank tokens")
    void blacklistTokenIgnoresBlankTokens() {
        service.blacklistToken("   ");

        verifyNoInteractions(temporaryStore, jwtProperties);
    }

    @Test
    @DisplayName("isBlacklisted returns true for blank tokens")
    void isBlacklistedReturnsTrueForBlankTokens() {
        assertThat(service.isBlacklisted("")).isTrue();
        verifyNoInteractions(temporaryStore);
    }

    @Test
    @DisplayName("isBlacklisted delegates to the temporary store")
    void isBlacklistedDelegatesToTemporaryStore() {
        when(temporaryStore.contains("jwt:blacklist:" + service.sha256(TOKEN))).thenReturn(true);

        assertThat(service.isBlacklisted(TOKEN)).isTrue();
    }

    @Test
    @DisplayName("isBlacklisted fails closed when the store errors")
    void isBlacklistedFailsClosedWhenStoreErrors() {
        doThrow(new RuntimeException("store down"))
                .when(temporaryStore).contains(any());

        assertThat(service.isBlacklisted(TOKEN)).isTrue();
    }
}
