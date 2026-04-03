package com.zufar.icedlatte.email.api.token;

import com.zufar.icedlatte.email.exception.IncorrectTokenException;
import com.zufar.icedlatte.openapi.dto.UserRegistrationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("TokenCache unit tests")
class TokenCacheTest {

    private TokenCache tokenCache;
    private UserRegistrationRequest request;

    @BeforeEach
    void setUp() {
        tokenCache = new InMemoryTokenCache(5);
        request = new UserRegistrationRequest("John", "Doe", "john@example.com", "Password1!");
    }

    @Test
    @DisplayName("addToken and getToken returns stored request for matching purpose")
    void addAndGet_validToken_returnsRequest() {
        tokenCache.addToken("123456789", request, TokenPurpose.EMAIL_VERIFICATION);
        assertThat(tokenCache.getToken("123456789", TokenPurpose.EMAIL_VERIFICATION)).isEqualTo(request);
    }

    @Test
    @DisplayName("getToken for unknown key throws IncorrectTokenException")
    void getToken_unknownKey_throwsIncorrectTokenException() {
        assertThatThrownBy(() -> tokenCache.getToken("000000000", TokenPurpose.EMAIL_VERIFICATION))
                .isInstanceOf(IncorrectTokenException.class);
    }

    @Test
    @DisplayName("getToken with wrong purpose throws IncorrectTokenException")
    void getToken_wrongPurpose_throwsIncorrectTokenException() {
        tokenCache.addToken("123456789", request, TokenPurpose.EMAIL_VERIFICATION);
        assertThatThrownBy(() -> tokenCache.getToken("123456789", TokenPurpose.PASSWORD_RESET))
                .isInstanceOf(IncorrectTokenException.class);
    }

    @Test
    @DisplayName("removeToken makes token unavailable")
    void removeToken_afterRemoval_throwsOnGet() {
        tokenCache.addToken("987654321", request, TokenPurpose.PASSWORD_RESET);
        tokenCache.removeToken("987654321");
        assertThatThrownBy(() -> tokenCache.getToken("987654321", TokenPurpose.PASSWORD_RESET))
                .isInstanceOf(IncorrectTokenException.class);
    }

    @Test
    @DisplayName("removeToken on non-existent key does not throw")
    void removeToken_nonExistentKey_doesNotThrow() {
        tokenCache.removeToken("nonexistent");
    }
}
