package com.zufar.icedlatte.email.api;

import com.zufar.icedlatte.email.api.token.TokenManager;
import com.zufar.icedlatte.email.api.token.TokenPurpose;
import com.zufar.icedlatte.email.sender.AuthTokenEmailConfirmation;
import com.zufar.icedlatte.openapi.dto.UserRegistrationRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailTokenSender unit tests")
class EmailTokenSenderTest {

    @Mock
    private AuthTokenEmailConfirmation emailConfirmation;

    @Mock
    private TokenManager tokenManager;

    @InjectMocks
    private EmailTokenSender sender;

    @Nested
    @DisplayName("sendEmailVerificationCode")
    class SendEmailVerificationCode {

        @Test
        @DisplayName("generates verification token and sends it to request email")
        void generatesVerificationTokenAndSendsItToRequestEmail() {
            UserRegistrationRequest request =
                    new UserRegistrationRequest("John", "Doe", "john@example.com", "pass123!");
            when(tokenManager.generateToken(request, TokenPurpose.EMAIL_VERIFICATION)).thenReturn("token123");

            sender.sendEmailVerificationCode(request);

            verify(tokenManager).generateToken(request, TokenPurpose.EMAIL_VERIFICATION);
            verify(emailConfirmation).sendTemporaryCode("john@example.com", "token123");
            verifyNoMoreInteractions(tokenManager, emailConfirmation);
        }
    }

    @Nested
    @DisplayName("sendPasswordResetCode")
    class SendPasswordResetCode {

        @Test
        @DisplayName("creates password reset request with email and sends generated token")
        void createsPasswordResetRequestWithEmailAndSendsGeneratedToken() {
            ArgumentCaptor<UserRegistrationRequest> captor = ArgumentCaptor.forClass(UserRegistrationRequest.class);
            when(tokenManager.generateToken(any(UserRegistrationRequest.class), eq(TokenPurpose.PASSWORD_RESET)))
                    .thenReturn("resetToken");

            sender.sendPasswordResetCode("user@example.com");

            verify(tokenManager).generateToken(captor.capture(), eq(TokenPurpose.PASSWORD_RESET));
            verify(emailConfirmation).sendTemporaryCode("user@example.com", "resetToken");
            verifyNoMoreInteractions(tokenManager, emailConfirmation);

            assertThat(captor.getValue().getEmail()).isEqualTo("user@example.com");
        }
    }
}
