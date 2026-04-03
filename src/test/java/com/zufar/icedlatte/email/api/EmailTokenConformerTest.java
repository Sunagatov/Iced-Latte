package com.zufar.icedlatte.email.api;

import com.zufar.icedlatte.email.api.token.TokenManager;
import com.zufar.icedlatte.email.api.token.TokenPurpose;
import com.zufar.icedlatte.openapi.dto.ConfirmEmailRequest;
import com.zufar.icedlatte.openapi.dto.UserAuthenticationResponse;
import com.zufar.icedlatte.openapi.dto.UserRegistrationRequest;
import com.zufar.icedlatte.security.api.UserRegistrationService;
import com.zufar.icedlatte.user.api.ChangeUserPasswordOperationPerformer;
import com.zufar.icedlatte.user.api.SingleUserProvider;
import com.zufar.icedlatte.user.entity.UserEntity;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailTokenConformer unit tests")
class EmailTokenConformerTest {

    @Mock private UserRegistrationService userRegistrationService;
    @Mock private TokenManager tokenManager;
    @Mock private SingleUserProvider singleUserProvider;
    @Mock private ChangeUserPasswordOperationPerformer changeUserPasswordOperationPerformer;
    @Mock private HttpServletRequest httpRequest;
    @InjectMocks private EmailTokenConformer conformer;

    @Test
    @DisplayName("confirmEmailByCode validates token and registers user")
    void confirmEmailByCodeValidatesAndRegisters() {
        ConfirmEmailRequest confirmRequest = new ConfirmEmailRequest("token123");
        UserRegistrationRequest regRequest = new UserRegistrationRequest("John", "Doe", "john@example.com", "pass!");
        UserAuthenticationResponse authResponse = new UserAuthenticationResponse();
        when(tokenManager.validateToken(confirmRequest, TokenPurpose.EMAIL_VERIFICATION)).thenReturn(regRequest);
        when(userRegistrationService.register(regRequest, httpRequest)).thenReturn(authResponse);

        UserAuthenticationResponse result = conformer.confirmEmailByCode(confirmRequest, httpRequest);

        assertThat(result).isEqualTo(authResponse);
        verify(userRegistrationService).register(regRequest, httpRequest);
    }

    @Test
    @DisplayName("confirmResetPasswordEmailByCode validates token and changes password")
    void confirmResetPasswordEmailByCodeChangesPassword() {
        ConfirmEmailRequest confirmRequest = new ConfirmEmailRequest("resetToken");
        UserRegistrationRequest regRequest = new UserRegistrationRequest();
        regRequest.setEmail("user@example.com");
        UUID userId = UUID.randomUUID();
        UserEntity user = UserEntity.builder().id(userId).build();
        when(tokenManager.validateToken(confirmRequest, TokenPurpose.PASSWORD_RESET)).thenReturn(regRequest);
        when(singleUserProvider.getUserEntityByEmail("user@example.com")).thenReturn(user);

        conformer.confirmResetPasswordEmailByCode(confirmRequest, "newPass123!");

        verify(changeUserPasswordOperationPerformer).changeUserPassword(userId, "newPass123!");
    }
}
