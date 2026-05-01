package com.zufar.icedlatte.email.api.token;

import com.zufar.icedlatte.openapi.dto.ConfirmEmailRequest;
import com.zufar.icedlatte.openapi.dto.UserRegistrationRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TokenManager unit tests")
class TokenManagerTest {

    @Mock private EmailTokenCache tokenCache;
    @Mock private TokenGenerator tokenGenerator;
    @Mock private EmailTokenCooldownCache tokenTimeExpirationCache;

    @InjectMocks private TokenManager tokenManager;

    @Nested
    @DisplayName("generateToken")
    class GenerateToken {

        @Test
        @DisplayName("validates cooldown, stores the generated token, and records the send time")
        void validatesCooldownStoresGeneratedTokenAndRecordsSendTime() {
            UserRegistrationRequest request =
                    new UserRegistrationRequest("Alice", "Smith", "alice@example.com", "Password1!");
            when(tokenGenerator.nextToken()).thenReturn("123456789");

            String token = tokenManager.generateToken(request, TokenPurpose.EMAIL_VERIFICATION);

            assertThat(token).isEqualTo("123456789");
            var inOrder = inOrder(tokenTimeExpirationCache, tokenGenerator, tokenCache);
            inOrder.verify(tokenTimeExpirationCache).validateTimeToken("alice@example.com");
            inOrder.verify(tokenGenerator).nextToken();
            inOrder.verify(tokenCache).addToken("123456789", request, TokenPurpose.EMAIL_VERIFICATION);
            inOrder.verify(tokenTimeExpirationCache).manageEmailSendingRate("alice@example.com");
        }
    }

    @Nested
    @DisplayName("validateToken")
    class ValidateToken {

        @Test
        @DisplayName("validates token format before consuming the cached entry")
        void validatesTokenFormatBeforeConsumingCachedEntry() {
            ConfirmEmailRequest confirmRequest = new ConfirmEmailRequest("123456789");
            UserRegistrationRequest request =
                    new UserRegistrationRequest("Alice", "Smith", "alice@example.com", "Password1!");
            when(tokenCache.getToken("123456789", TokenPurpose.PASSWORD_RESET)).thenReturn(request);

            UserRegistrationRequest result = tokenManager.validateToken(confirmRequest, TokenPurpose.PASSWORD_RESET);

            assertThat(result).isSameAs(request);
            var inOrder = inOrder(tokenGenerator, tokenCache, tokenTimeExpirationCache);
            inOrder.verify(tokenGenerator).tokenIsValid("123456789");
            inOrder.verify(tokenCache).getToken("123456789", TokenPurpose.PASSWORD_RESET);
            inOrder.verify(tokenCache).removeToken("123456789");
            inOrder.verify(tokenTimeExpirationCache).removeToken("alice@example.com");
        }
    }

    @Test
    @DisplayName("deleteTokenFromCache removes both token and rate-limit state")
    void deleteTokenFromCacheRemovesBothTokenAndRateLimitState() {
        UserRegistrationRequest request =
                new UserRegistrationRequest("Alice", "Smith", "alice@example.com", "Password1!");
        when(tokenCache.getToken("123456789", TokenPurpose.EMAIL_VERIFICATION)).thenReturn(request);

        UserRegistrationRequest result = tokenManager.deleteTokenFromCache("123456789", TokenPurpose.EMAIL_VERIFICATION);

        assertThat(result).isSameAs(request);
        verify(tokenCache).removeToken("123456789");
        verify(tokenTimeExpirationCache).removeToken("alice@example.com");
    }
}
