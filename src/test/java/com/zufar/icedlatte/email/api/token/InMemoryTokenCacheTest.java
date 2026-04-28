package com.zufar.icedlatte.email.api.token;

import com.zufar.icedlatte.email.exception.IncorrectTokenException;
import com.zufar.icedlatte.openapi.dto.UserRegistrationRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("InMemoryTokenCache")
class InMemoryTokenCacheTest {

    private final InMemoryTokenCache cache = new InMemoryTokenCache(15);

    @Test
    @DisplayName("returns stored request when token and purpose match")
    void getToken_returnsStoredRequest() {
        UserRegistrationRequest request = new UserRegistrationRequest();
        request.setEmail("alice@example.com");
        cache.addToken("123456", request, TokenPurpose.EMAIL_VERIFICATION);

        UserRegistrationRequest result = cache.getToken("123456", TokenPurpose.EMAIL_VERIFICATION);

        assertThat(result).isSameAs(request);
    }

    @Test
    @DisplayName("rejects unknown tokens")
    void getToken_rejectsUnknownToken() {
        assertThatThrownBy(() -> cache.getToken("missing", TokenPurpose.EMAIL_VERIFICATION))
                .isInstanceOf(IncorrectTokenException.class);
    }

    @Test
    @DisplayName("rejects tokens stored for a different purpose")
    void getToken_rejectsWrongPurpose() {
        cache.addToken("123456", new UserRegistrationRequest(), TokenPurpose.PASSWORD_RESET);

        assertThatThrownBy(() -> cache.getToken("123456", TokenPurpose.EMAIL_VERIFICATION))
                .isInstanceOf(IncorrectTokenException.class);
    }

    @Test
    @DisplayName("removes tokens from the cache")
    void removeToken_invalidatesStoredToken() {
        cache.addToken("123456", new UserRegistrationRequest(), TokenPurpose.EMAIL_VERIFICATION);

        cache.removeToken("123456");

        assertThatThrownBy(() -> cache.getToken("123456", TokenPurpose.EMAIL_VERIFICATION))
                .isInstanceOf(IncorrectTokenException.class);
    }
}
