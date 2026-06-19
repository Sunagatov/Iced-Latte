package com.zufar.icedlatte.security.signup.verification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zufar.icedlatte.openapi.dto.UserRegistrationRequest;
import com.zufar.icedlatte.security.email.sender.AuthTokenEmailSender;
import com.zufar.icedlatte.security.jwt.config.JwtProperties;
import com.zufar.icedlatte.security.service.cache.ExpiringKeyValueStore;
import com.zufar.icedlatte.security.service.cache.InMemoryExpiringKeyValueStore;
import com.zufar.icedlatte.security.session.management.AuthSessionRequestMetadata;
import com.zufar.icedlatte.security.session.token.AuthenticationTokens;
import com.zufar.icedlatte.security.signin.exception.UserRegistrationException;
import com.zufar.icedlatte.security.signup.registration.UserRegistrationService;
import com.zufar.icedlatte.user.api.UserAccessControlApi;
import com.zufar.icedlatte.user.api.UserLookupApi;
import com.zufar.icedlatte.user.api.dto.UserLookupSnapshot;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailVerificationService unit tests")
class EmailVerificationServiceTest {

    @Mock
    private AuthTokenEmailSender emailConfirmation;

    @Mock
    private UserRegistrationService userRegistrationService;

    @Mock
    private UserLookupApi userLookupApi;

    @Mock
    private UserAccessControlApi userAccessControlApi;

    @Mock
    private PasswordEncoder passwordEncoder;

    private EmailVerificationService service;
    private EmailTokenService emailTokenService;
    private static final AuthSessionRequestMetadata REQUEST_METADATA =
            new AuthSessionRequestMetadata("TestAgent", "127.0.0.1");

    @BeforeEach
    void setUp() {
        emailTokenService = tokenService(new InMemoryExpiringKeyValueStore(
                new com.zufar.icedlatte.common.config.CaffeineSizeProperties(1_000, 5_000, 10_000, 1_000, 10_000)));
        service = new EmailVerificationService(
                emailConfirmation, emailTokenService, userRegistrationService, userLookupApi, userAccessControlApi);
        lenient().when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");
    }

    private EmailTokenService tokenService(ExpiringKeyValueStore store) {
        ObjectMapper objectMapper = new ObjectMapper();
        return new EmailTokenService(
                store,
                passwordEncoder,
                tokenPayloadProtector(objectMapper),
                new EmailTokenProperties(43, ""),
                new TemporaryTokenProperties(new TemporaryTokenProperties.Time(15)));
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

            verify(userRegistrationService).ensureRegistrationAllowed(request, null);
            verify(emailConfirmation)
                    .sendTemporaryCode(eq("john@example.com"), argThat(EmailVerificationServiceTest::isOpaqueToken));
        }

        @Test
        @DisplayName("does not send token when email is already registered")
        void doesNotSendTokenWhenEmailIsAlreadyRegistered() {
            UserRegistrationRequest request =
                    new UserRegistrationRequest("John", "Doe", "john@example.com", "pass123!");
            doThrow(new UserRegistrationException("duplicate"))
                    .when(userRegistrationService)
                    .ensureRegistrationAllowed(request, null);

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

            verify(emailConfirmation)
                    .sendTemporaryCode(eq("user@example.com"), argThat(EmailVerificationServiceTest::isOpaqueToken));
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
            AuthenticationTokens authResponse = new AuthenticationTokens("access-token", "refresh-token");
            String token = emailTokenService.generateEmailVerificationToken(registrationRequest);
            when(userRegistrationService.completeEmailVerifiedRegistration(
                            argThat(request ->
                                    request.getEmail().equals("john@example.com") && request.getPassword() == null),
                            eq("encoded-password"),
                            eq(REQUEST_METADATA)))
                    .thenReturn(authResponse);

            AuthenticationTokens result = service.confirmEmailByCode(token, REQUEST_METADATA);

            assertThat(result).isSameAs(authResponse);
            verify(userRegistrationService)
                    .completeEmailVerifiedRegistration(
                            argThat(request ->
                                    request.getEmail().equals("john@example.com") && request.getPassword() == null),
                            eq("encoded-password"),
                            eq(REQUEST_METADATA));
        }
    }

    @Nested
    @DisplayName("confirmResetPasswordEmailByCode")
    class ConfirmResetPasswordEmailByCode {

        @Test
        @DisplayName("resolves user from password reset token and changes password")
        void resolvesUserFromPasswordResetTokenAndChangesPassword() {
            UUID userId = UUID.randomUUID();
            var user = new UserLookupSnapshot(userId, "Ada", "Lovelace", "user@example.com");
            String token = emailTokenService.generatePasswordResetToken("user@example.com");
            when(userLookupApi.getUserByEmail("user@example.com")).thenReturn(user);

            service.confirmResetPasswordEmailByCode(token, "newPass123!");

            verify(userLookupApi).getUserByEmail("user@example.com");
            verify(userAccessControlApi).changePassword(userId, "newPass123!");
        }
    }

    @Test
    @DisplayName("generateToken returns an opaque URL-safe token")
    void generateTokenReturnsOpaqueUrlSafeToken() {
        UserRegistrationRequest request =
                new UserRegistrationRequest("Alice", "Smith", "alice@example.com", "Password1!");

        String token = emailTokenService.generateEmailVerificationToken(request);

        assertThat(token).hasSize(43).matches("[A-Za-z0-9_-]{43}");
    }

    @Test
    @DisplayName("validateToken rejects invalid token format")
    void validateTokenRejectsInvalidTokenFormat() {
        assertThatThrownBy(() -> emailTokenService.consumeEmailVerificationToken("12345"))
                .isInstanceOf(com.zufar.icedlatte.common.exception.BadRequestException.class);
    }

    @Test
    @DisplayName("generateToken rejects weak token length configuration")
    void generateTokenRejectsWeakTokenLengthConfiguration() {
        assertThatThrownBy(() -> new EmailTokenProperties(9, ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be at least 32");
    }

    @Test
    @DisplayName("email token properties reject missing token length")
    void emailTokenPropertiesRejectMissingTokenLength() {
        assertThatThrownBy(() -> new EmailTokenProperties(null, ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("email.verification-token-length must not be null");
    }

    @Test
    @DisplayName("temporary token properties reject missing time configuration")
    void temporaryTokenPropertiesRejectMissingTimeConfiguration() {
        assertThatThrownBy(() -> new TemporaryTokenProperties(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("temporary-cache.time must not be null");
    }

    @Test
    @DisplayName("temporary token properties reject missing token ttl")
    void temporaryTokenPropertiesRejectMissingTokenTtl() {
        assertThatThrownBy(() -> new TemporaryTokenProperties(new TemporaryTokenProperties.Time(null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("temporary-cache.time.token must not be null");
    }

    @Test
    @DisplayName("generateToken normalizes email and scopes hashed token key by purpose")
    void generateTokenNormalizesEmailAndScopesHashedTokenKeyByPurpose() {
        ExpiringKeyValueStore store = mock(ExpiringKeyValueStore.class);
        EmailTokenService serviceWithMockStore = tokenServiceWithStore(store);
        UserRegistrationRequest request =
                new UserRegistrationRequest("Ada", "Lovelace", " User@Example.COM ", "Password1!");
        when(store.get(argThat(key -> key.startsWith("email:rate:") && !key.endsWith("user@example.com"))))
                .thenReturn(Optional.empty());
        when(store.putIfAbsent(
                        argThat(key -> key.startsWith("email:token:email_verification:")),
                        argThat(value -> !value.contains("user@example.com")
                                && !value.contains("Ada")
                                && !value.contains("Lovelace")
                                && !value.contains("encoded-password")
                                && !value.contains("Password1!")),
                        eq(Duration.ofMinutes(15))))
                .thenReturn(true);

        String token = serviceWithMockStore.generateEmailVerificationToken(request);

        assertThat(request.getEmail()).isEqualTo(" User@Example.COM ");
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(store).putIfAbsent(keyCaptor.capture(), any(), eq(Duration.ofMinutes(15)));
        assertThat(keyCaptor.getValue())
                .startsWith("email:token:email_verification:")
                .doesNotEndWith(token);
        verify(store)
                .put(
                        argThat(key -> key.startsWith("email:rate:") && !key.endsWith("user@example.com")),
                        any(),
                        eq(Duration.ofMinutes(15)));
    }

    @Test
    @DisplayName("generateToken retries instead of overwriting an existing same-purpose token")
    void generateTokenRetriesInsteadOfOverwritingExistingSamePurposeToken() {
        ExpiringKeyValueStore store = mock(ExpiringKeyValueStore.class);
        EmailTokenService serviceWithMockStore = tokenServiceWithStore(store);
        UserRegistrationRequest request =
                new UserRegistrationRequest("Ada", "Lovelace", "user@example.com", "Password1!");
        when(store.get(argThat(key -> key.startsWith("email:rate:")))).thenReturn(Optional.empty());
        when(store.putIfAbsent(
                        argThat(key -> key.startsWith("email:token:password_reset:")),
                        any(),
                        eq(Duration.ofMinutes(15))))
                .thenReturn(false)
                .thenReturn(true);

        serviceWithMockStore.generatePasswordResetToken(request.getEmail());

        verify(store, times(2))
                .putIfAbsent(
                        argThat(key -> key.startsWith("email:token:password_reset:")),
                        any(),
                        eq(Duration.ofMinutes(15)));
        verify(store)
                .put(
                        argThat(key -> key.startsWith("email:rate:") && !key.endsWith("user@example.com")),
                        any(),
                        eq(Duration.ofMinutes(15)));
    }

    private EmailTokenService tokenServiceWithStore(ExpiringKeyValueStore store) {
        ObjectMapper objectMapper = new ObjectMapper();
        return new EmailTokenService(
                store,
                passwordEncoder,
                tokenPayloadProtector(objectMapper),
                new EmailTokenProperties(43, ""),
                new TemporaryTokenProperties(new TemporaryTokenProperties.Time(15)));
    }

    private static EmailTokenPayloadProtector tokenPayloadProtector(ObjectMapper objectMapper) {
        return new EmailTokenPayloadProtector(objectMapper, jwtProperties(), new EmailTokenProperties(43, ""));
    }

    private static JwtProperties jwtProperties() {
        return new JwtProperties(
                "Authorization",
                "NDA0RTYzNTI2NjU1NkE1ODZFMzI3MjM1NzUzODc4MkY0MTNBNDQ0Mjg0NzJCNEI2MjUwNjQ1MzY3NTY2QjU5NzA=",
                "NDA0RTYzNTI2NjU1NkE1ODZFMzI3MjM1NzUzODc4MkY0MTNBNDQ0Mjg0NzJCNEI2MjUwNjQ1MzY3NTY2QjU5NzA0MDRFNTM1MjY2NTU2QTU4NkUzMjcyMzU3NTM4NzgyRjQxM0E0NDQyODQ3MkI0QjYyNTA2NDUzNjc1NjZCNTk3MA==",
                Duration.ofMinutes(30),
                Duration.ofHours(24),
                "iced-latte",
                "iced-latte-users");
    }

    private static boolean isOpaqueToken(String token) {
        return token != null && token.matches("[A-Za-z0-9_-]{43}");
    }
}
