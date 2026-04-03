package com.zufar.icedlatte.email.api;

import com.zufar.icedlatte.email.api.token.TokenManager;
import com.zufar.icedlatte.email.api.token.TokenPurpose;
import com.zufar.icedlatte.email.sender.AuthTokenEmailConfirmation;
import com.zufar.icedlatte.openapi.dto.UserRegistrationRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailTokenSender unit tests")
class EmailTokenSenderTest {

    @Mock private AuthTokenEmailConfirmation emailConfirmation;
    @Mock private TokenManager tokenManager;
    @InjectMocks private EmailTokenSender sender;

    @Test
    @DisplayName("sendEmailVerificationCode generates token and sends email")
    void sendEmailVerificationCodeGeneratesAndSends() {
        UserRegistrationRequest request = new UserRegistrationRequest("John", "Doe", "john@example.com", "pass123!");
        when(tokenManager.generateToken(request, TokenPurpose.EMAIL_VERIFICATION)).thenReturn("token123");

        sender.sendEmailVerificationCode(request);

        verify(tokenManager).generateToken(request, TokenPurpose.EMAIL_VERIFICATION);
        verify(emailConfirmation).sendTemporaryCode("john@example.com", "token123");
    }

    @Test
    @DisplayName("sendPasswordResetCode generates token and sends email")
    void sendPasswordResetCodeGeneratesAndSends() {
        when(tokenManager.generateToken(any(UserRegistrationRequest.class), eq(TokenPurpose.PASSWORD_RESET)))
                .thenReturn("resetToken");

        sender.sendPasswordResetCode("user@example.com");

        verify(emailConfirmation).sendTemporaryCode("user@example.com", "resetToken");
    }
}
