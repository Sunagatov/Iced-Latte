package com.zufar.icedlatte.security.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zufar.icedlatte.common.temporarycache.InMemoryExpiringKeyValueStore;
import com.zufar.icedlatte.email.sender.AuthTokenEmailSender;
import com.zufar.icedlatte.openapi.dto.ConfirmEmailRequest;
import com.zufar.icedlatte.openapi.dto.UserAuthenticationResponse;
import com.zufar.icedlatte.openapi.dto.UserRegistrationRequest;
import com.zufar.icedlatte.security.exception.UserRegistrationException;
import com.zufar.icedlatte.user.api.SingleUserProvider;
import com.zufar.icedlatte.user.api.UserProfileService;
import com.zufar.icedlatte.user.entity.UserEntity;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailVerificationService unit tests")
class EmailVerificationServiceTest {

    @Mock private AuthTokenEmailSender emailConfirmation;
    @Mock private UserRegistrationService userRegistrationService;
    @Mock private SingleUserProvider singleUserProvider;
    @Mock private UserProfileService userProfileService;
    @Mock private HttpServletRequest httpRequest;

    private EmailVerificationService service;

    @BeforeEach
    void setUp() {
        service = new EmailVerificationService(
                new InMemoryExpiringKeyValueStore(new com.zufar.icedlatte.common.config.CaffeineSizeProperties(1_000, 5_000, 10_000, 1_000, 10_000)),
                new ObjectMapper(),
                emailConfirmation,
                userRegistrationService,
                singleUserProvider,
                userProfileService
        );
        ReflectionTestUtils.setField(service, "expireTimeMinutes", 15);
        ReflectionTestUtils.setField(service, "tokenLength", 9);
    }

    @Nested
    @DisplayName("sendEmailVerificationCode")
    class SendEmailVerificationCode {

        @Test
        @DisplayName("stores verification token and sends it to request email")
        void storesVerificationTokenAndSendsItToRequestEmail() {
            UserRegistrationRequest request =
                    new UserRegistrationRequest("John", "Doe", "john@example.com", "pass123!");

            service.sendEmailVerificationCode(request);

            verify(userRegistrationService).ensureEmailAvailable(request);
            verify(emailConfirmation).sendTemporaryCode(eq("john@example.com"), argThat(token -> token.matches("\\d{9}")));
        }

        @Test
        @DisplayName("does not send token when email is already registered")
        void doesNotSendTokenWhenEmailIsAlreadyRegistered() {
            UserRegistrationRequest request =
                    new UserRegistrationRequest("John", "Doe", "john@example.com", "pass123!");
            doThrow(new UserRegistrationException("duplicate"))
                    .when(userRegistrationService).ensureEmailAvailable(request);

            assertThatThrownBy(() -> service.sendEmailVerificationCode(request))
                    .isInstanceOf(UserRegistrationException.class);

            verifyNoInteractions(emailConfirmation);
        }
    }

    @Nested
    @DisplayName("sendPasswordResetCode")
    class SendPasswordResetCode {

        @Test
        @DisplayName("creates password reset request with email and sends generated token")
        void createsPasswordResetRequestWithEmailAndSendsGeneratedToken() {
            service.sendPasswordResetCode("user@example.com");

            verify(emailConfirmation).sendTemporaryCode(eq("user@example.com"), argThat(token -> token.matches("\\d{9}")));
        }
    }

    @Nested
    @DisplayName("confirmEmailByCode")
    class ConfirmEmailByCode {

        @Test
        @DisplayName("consumes email verification token and registers user")
        void consumesEmailVerificationTokenAndRegistersUser() {
            UserRegistrationRequest registrationRequest =
                    new UserRegistrationRequest("John", "Doe", "john@example.com", "pass!");
            UserAuthenticationResponse authResponse = new UserAuthenticationResponse();
            String token = service.generateToken(registrationRequest, TokenPurpose.EMAIL_VERIFICATION);
            when(userRegistrationService.register(registrationRequest, httpRequest)).thenReturn(authResponse);

            UserAuthenticationResponse result = service.confirmEmailByCode(new ConfirmEmailRequest(token), httpRequest);

            assertThat(result).isSameAs(authResponse);
            verify(userRegistrationService).register(registrationRequest, httpRequest);
        }
    }

    @Nested
    @DisplayName("confirmResetPasswordEmailByCode")
    class ConfirmResetPasswordEmailByCode {

        @Test
        @DisplayName("resolves user from password reset token and changes password")
        void resolvesUserFromPasswordResetTokenAndChangesPassword() {
            UserRegistrationRequest registrationRequest = new UserRegistrationRequest();
            registrationRequest.setEmail("user@example.com");
            UUID userId = UUID.randomUUID();
            UserEntity user = UserEntity.builder().id(userId).build();
            String token = service.generateToken(registrationRequest, TokenPurpose.PASSWORD_RESET);
            when(singleUserProvider.getUserEntityByEmail("user@example.com")).thenReturn(user);

            service.confirmResetPasswordEmailByCode(new ConfirmEmailRequest(token), "newPass123!");

            verify(singleUserProvider).getUserEntityByEmail("user@example.com");
            verify(userProfileService).changePassword(userId, "newPass123!");
        }
    }

    @Test
    @DisplayName("generateToken returns a 9 digit token")
    void generateTokenReturnsNineDigitToken() {
        UserRegistrationRequest request = new UserRegistrationRequest("Alice", "Smith", "alice@example.com", "Password1!");

        String token = service.generateToken(request, TokenPurpose.EMAIL_VERIFICATION);

        assertThat(token).hasSize(9).matches("\\d{9}");
    }

    @Test
    @DisplayName("validateToken rejects invalid token format")
    void validateTokenRejectsInvalidTokenFormat() {
        assertThatThrownBy(() -> service.validateToken(new ConfirmEmailRequest("12345"), TokenPurpose.EMAIL_VERIFICATION))
                .isInstanceOf(com.zufar.icedlatte.common.exception.BadRequestException.class);
    }
}
