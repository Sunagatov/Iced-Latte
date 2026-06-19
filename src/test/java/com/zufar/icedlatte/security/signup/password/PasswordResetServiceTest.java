package com.zufar.icedlatte.security.signup.password;

import static org.mockito.Mockito.*;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.zufar.icedlatte.common.turnstile.TurnstileVerificationRequest;
import com.zufar.icedlatte.common.turnstile.TurnstileVerifier;
import com.zufar.icedlatte.security.signup.exception.TimeTokenException;
import com.zufar.icedlatte.security.signup.verification.EmailVerificationService;
import com.zufar.icedlatte.user.api.UserLookupApi;
import com.zufar.icedlatte.user.api.dto.UserLookupSnapshot;

@ExtendWith(MockitoExtension.class)
@DisplayName("PasswordResetService unit tests")
class PasswordResetServiceTest {

    @Mock
    private UserLookupApi userLookupApi;

    @Mock
    private EmailVerificationService emailVerificationService;

    @Mock
    private TurnstileVerifier turnstileVerifier;

    @InjectMocks
    private PasswordResetService service;

    @Nested
    @DisplayName("requestReset")
    class RequestReset {

        @Test
        @DisplayName("sends a reset code for a known user")
        void sendsResetCodeForKnownUser() {
            String email = "known@example.com";
            when(userLookupApi.findUserByEmail(email))
                    .thenReturn(Optional.of(new UserLookupSnapshot(UUID.randomUUID(), "Known", "User", email)));

            service.requestReset(email, "turnstile-token");

            verify(turnstileVerifier)
                    .verify(new TurnstileVerificationRequest(
                            "turnstile-token", null, "forgot_password", "forgot_password"));
            verify(userLookupApi).findUserByEmail(email);
            verify(emailVerificationService).sendPasswordResetCode(email);
        }

        @Test
        @DisplayName("normalizes email before lookup and reset code generation")
        void normalizesEmailBeforeLookupAndResetCodeGeneration() {
            String normalizedEmail = "known@example.com";
            when(userLookupApi.findUserByEmail(normalizedEmail))
                    .thenReturn(
                            Optional.of(new UserLookupSnapshot(UUID.randomUUID(), "Known", "User", normalizedEmail)));

            service.requestReset("  Known@Example.com ", "turnstile-token");

            verify(turnstileVerifier)
                    .verify(new TurnstileVerificationRequest(
                            "turnstile-token", null, "forgot_password", "forgot_password"));
            verify(userLookupApi).findUserByEmail(normalizedEmail);
            verify(emailVerificationService).sendPasswordResetCode(normalizedEmail);
        }

        @Test
        @DisplayName("swallows unknown email lookups")
        void swallowsUnknownEmailLookups() {
            String email = "missing@example.com";
            when(userLookupApi.findUserByEmail(email)).thenReturn(Optional.empty());

            service.requestReset(email, "turnstile-token");

            verify(turnstileVerifier)
                    .verify(new TurnstileVerificationRequest(
                            "turnstile-token", null, "forgot_password", "forgot_password"));
            verify(userLookupApi).findUserByEmail(email);
            verifyNoInteractions(emailVerificationService);
        }

        @Test
        @DisplayName("swallows cooldown failures to avoid account enumeration")
        void swallowsCooldownFailures() {
            String email = "known@example.com";
            when(userLookupApi.findUserByEmail(email))
                    .thenReturn(Optional.of(new UserLookupSnapshot(UUID.randomUUID(), "Known", "User", email)));
            doThrow(new TimeTokenException(OffsetDateTime.now().plusMinutes(1)))
                    .when(emailVerificationService)
                    .sendPasswordResetCode(email);

            service.requestReset(email, "turnstile-token");

            verify(userLookupApi).findUserByEmail(email);
            verify(turnstileVerifier)
                    .verify(new TurnstileVerificationRequest(
                            "turnstile-token", null, "forgot_password", "forgot_password"));
            verify(emailVerificationService).sendPasswordResetCode(email);
        }
    }

    @Test
    @DisplayName("confirmReset delegates with the provided token and password")
    void confirmResetDelegatesWithProvidedTokenAndPassword() {
        service.confirmReset("reset-token", "new-password", "turnstile-token");

        verify(turnstileVerifier)
                .verify(new TurnstileVerificationRequest(
                        "turnstile-token", null, "change_password", "change_password"));
        verify(emailVerificationService).confirmResetPasswordEmailByCode("reset-token", "new-password");
    }
}
