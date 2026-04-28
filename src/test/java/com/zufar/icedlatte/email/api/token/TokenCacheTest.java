package com.zufar.icedlatte.email.api.token;

import com.zufar.icedlatte.email.exception.IncorrectTokenException;
import com.zufar.icedlatte.openapi.dto.UserRegistrationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
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

    @Nested
    @DisplayName("getToken")
    class GetToken {

        @Test
        @DisplayName("returns stored request for matching token and purpose")
        void returnsStoredRequestForMatchingTokenAndPurpose() {
            tokenCache.addToken("123456789", request, TokenPurpose.EMAIL_VERIFICATION);

            UserRegistrationRequest result = tokenCache.getToken("123456789", TokenPurpose.EMAIL_VERIFICATION);

            assertThat(result).isSameAs(request);
        }

        @Test
        @DisplayName("throws when token key is unknown")
        void throwsWhenTokenKeyIsUnknown() {
            assertThatThrownBy(() -> tokenCache.getToken("000000000", TokenPurpose.EMAIL_VERIFICATION))
                    .isInstanceOf(IncorrectTokenException.class);
        }

        @Test
        @DisplayName("throws when purpose does not match stored token")
        void throwsWhenPurposeDoesNotMatchStoredToken() {
            tokenCache.addToken("123456789", request, TokenPurpose.EMAIL_VERIFICATION);

            assertThatThrownBy(() -> tokenCache.getToken("123456789", TokenPurpose.PASSWORD_RESET))
                    .isInstanceOf(IncorrectTokenException.class);
        }
    }

    @Nested
    @DisplayName("removeToken")
    class RemoveToken {

        @Test
        @DisplayName("makes token unavailable after removal")
        void makesTokenUnavailableAfterRemoval() {
            tokenCache.addToken("987654321", request, TokenPurpose.PASSWORD_RESET);

            tokenCache.removeToken("987654321");

            assertThatThrownBy(() -> tokenCache.getToken("987654321", TokenPurpose.PASSWORD_RESET))
                    .isInstanceOf(IncorrectTokenException.class);
        }

        @Test
        @DisplayName("is a no-op for missing token")
        void isANoOpForMissingToken() {
            assertThatCode(() -> tokenCache.removeToken("nonexistent")).doesNotThrowAnyException();
        }
    }
}
