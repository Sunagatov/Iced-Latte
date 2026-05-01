package com.zufar.icedlatte.email.api;

import com.zufar.icedlatte.email.api.token.TokenManager;
import com.zufar.icedlatte.email.api.token.TokenPurpose;
import com.zufar.icedlatte.openapi.dto.ConfirmEmailRequest;
import com.zufar.icedlatte.openapi.dto.UserAuthenticationResponse;
import com.zufar.icedlatte.openapi.dto.UserRegistrationRequest;
import com.zufar.icedlatte.security.api.UserRegistrationService;
import com.zufar.icedlatte.user.api.SingleUserProvider;
import com.zufar.icedlatte.user.api.UserProfileService;
import com.zufar.icedlatte.user.entity.UserEntity;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailTokenConformer unit tests")
class EmailTokenConformerTest {

    @Mock
    private UserRegistrationService userRegistrationService;

    @Mock
    private TokenManager tokenManager;

    @Mock
    private SingleUserProvider singleUserProvider;

    @Mock
    private UserProfileService userProfileService;

    @Mock
    private HttpServletRequest httpRequest;

    @InjectMocks
    private EmailTokenConformer conformer;

    @Nested
    @DisplayName("confirmEmailByCode")
    class ConfirmEmailByCode {

        @Test
        @DisplayName("validates email verification token and registers user")
        void validatesEmailVerificationTokenAndRegistersUser() {
            ConfirmEmailRequest confirmRequest = new ConfirmEmailRequest("token123");
            UserRegistrationRequest registrationRequest =
                    new UserRegistrationRequest("John", "Doe", "john@example.com", "pass!");
            UserAuthenticationResponse authResponse = new UserAuthenticationResponse();
            when(tokenManager.validateToken(confirmRequest, TokenPurpose.EMAIL_VERIFICATION))
                    .thenReturn(registrationRequest);
            when(userRegistrationService.register(registrationRequest, httpRequest)).thenReturn(authResponse);

            UserAuthenticationResponse result = conformer.confirmEmailByCode(confirmRequest, httpRequest);

            assertThat(result).isSameAs(authResponse);
            verify(tokenManager).validateToken(confirmRequest, TokenPurpose.EMAIL_VERIFICATION);
            verify(userRegistrationService).register(registrationRequest, httpRequest);
            verifyNoMoreInteractions(tokenManager, userRegistrationService, singleUserProvider, userProfileService);
        }
    }

    @Nested
    @DisplayName("confirmResetPasswordEmailByCode")
    class ConfirmResetPasswordEmailByCode {

        @Test
        @DisplayName("resolves user from validated password reset token and changes password")
        void resolvesUserFromValidatedPasswordResetTokenAndChangesPassword() {
            ConfirmEmailRequest confirmRequest = new ConfirmEmailRequest("resetToken");
            UserRegistrationRequest registrationRequest = new UserRegistrationRequest();
            registrationRequest.setEmail("user@example.com");
            UUID userId = UUID.randomUUID();
            UserEntity user = UserEntity.builder().id(userId).build();
            when(tokenManager.validateToken(confirmRequest, TokenPurpose.PASSWORD_RESET)).thenReturn(registrationRequest);
            when(singleUserProvider.getUserEntityByEmail("user@example.com")).thenReturn(user);

            conformer.confirmResetPasswordEmailByCode(confirmRequest, "newPass123!");

            verify(tokenManager).validateToken(confirmRequest, TokenPurpose.PASSWORD_RESET);
            verify(singleUserProvider).getUserEntityByEmail("user@example.com");
            verify(userProfileService).changePassword(userId, "newPass123!");
            verifyNoMoreInteractions(tokenManager, singleUserProvider, userProfileService, userRegistrationService);
        }
    }
}
