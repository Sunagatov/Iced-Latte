package com.zufar.icedlatte.security.api;

import com.zufar.icedlatte.email.exception.TimeTokenException;
import com.zufar.icedlatte.user.api.SingleUserProvider;
import com.zufar.icedlatte.user.exception.UserNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("PasswordResetService unit tests")
class PasswordResetServiceTest {

    @Mock private SingleUserProvider singleUserProvider;
    @Mock private EmailVerificationService emailVerificationService;

    @InjectMocks private PasswordResetService service;

    @Nested
    @DisplayName("requestReset")
    class RequestReset {

        @Test
        @DisplayName("sends a reset code for a known user")
        void sendsResetCodeForKnownUser() {
            String email = "known@example.com";

            service.requestReset(email);

            verify(singleUserProvider).getUserEntityByEmail(email);
            verify(emailVerificationService).sendPasswordResetCode(email);
        }

        @Test
        @DisplayName("swallows unknown email lookups")
        void swallowsUnknownEmailLookups() {
            String email = "missing@example.com";
            doThrow(new UserNotFoundException(email))
                    .when(singleUserProvider).getUserEntityByEmail(email);

            service.requestReset(email);

            verify(singleUserProvider).getUserEntityByEmail(email);
            verifyNoInteractions(emailVerificationService);
        }

        @Test
        @DisplayName("swallows cooldown failures to avoid account enumeration")
        void swallowsCooldownFailures() {
            String email = "known@example.com";
            doThrow(new TimeTokenException(email, OffsetDateTime.now().plusMinutes(1)))
                    .when(emailVerificationService).sendPasswordResetCode(email);

            service.requestReset(email);

            verify(singleUserProvider).getUserEntityByEmail(email);
            verify(emailVerificationService).sendPasswordResetCode(email);
        }
    }

    @Test
    @DisplayName("confirmReset delegates with the provided token and password")
    void confirmResetDelegatesWithProvidedTokenAndPassword() {
        service.confirmReset("reset-token", "new-password");

        verify(emailVerificationService).confirmResetPasswordEmailByCode(
                argThat(request -> request != null && "reset-token".equals(request.getToken())),
                org.mockito.ArgumentMatchers.eq("new-password"));
    }
}
