package com.zufar.icedlatte.email.api.token;

import com.zufar.icedlatte.email.exception.TimeTokenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

@DisplayName("TokenTimeExpirationCache unit tests")
class TokenTimeExpirationCacheTest {

    private TokenTimeExpirationCache cache;

    @BeforeEach
    void setUp() {
        cache = new TokenTimeExpirationCache(5);
    }

    @Test
    @DisplayName("validateTimeToken passes when no entry exists for email")
    void validateTimeToken_noEntry_doesNotThrow() {
        assertThatCode(() -> cache.validateTimeToken("new@example.com"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("validateTimeToken throws TimeTokenException when email was recently used")
    void validateTimeToken_afterManage_throwsTimeTokenException() {
        String email = "user@example.com";
        cache.manageEmailSendingRate(email);
        assertThatThrownBy(() -> cache.validateTimeToken(email))
                .isInstanceOf(TimeTokenException.class);
    }

    @Test
    @DisplayName("removeToken clears rate limit so validation passes again")
    void removeToken_afterRemoval_validationPasses() {
        String email = "user2@example.com";
        cache.manageEmailSendingRate(email);
        cache.removeToken(email);
        assertThatCode(() -> cache.validateTimeToken(email))
                .doesNotThrowAnyException();
    }
}
