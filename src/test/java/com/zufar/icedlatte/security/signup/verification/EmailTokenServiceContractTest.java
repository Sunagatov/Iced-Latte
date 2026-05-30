package com.zufar.icedlatte.security.signup.verification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zufar.icedlatte.common.exception.BadRequestException;
import com.zufar.icedlatte.openapi.dto.ConfirmEmailRequest;
import com.zufar.icedlatte.openapi.dto.UserRegistrationRequest;
import com.zufar.icedlatte.security.jwt.config.JwtProperties;
import com.zufar.icedlatte.security.service.cache.InMemoryExpiringKeyValueStore;
import com.zufar.icedlatte.security.session.dto.TokenPurpose;
import com.zufar.icedlatte.security.signup.exception.TimeTokenException;

@DisplayName("EmailTokenService contract tests")
class EmailTokenServiceContractTest {

    private EmailTokenService service;

    @BeforeEach
    void setUp() {
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        org.mockito.Mockito.when(passwordEncoder.encode(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn("encoded-password");
        ObjectMapper objectMapper = new ObjectMapper();
        service = new EmailTokenService(
                new InMemoryExpiringKeyValueStore(new com.zufar.icedlatte.common.config.CaffeineSizeProperties(
                        1_000, 5_000, 10_000, 1_000, 10_000)),
                passwordEncoder,
                tokenPayloadProtector(objectMapper));
        ReflectionTestUtils.setField(service, "expireTimeMinutes", 5);
        ReflectionTestUtils.setField(service, "tokenLength", 43);
    }

    @Test
    @DisplayName("generated token can be validated once and only once")
    void generatedTokenCanBeValidatedOnceAndOnlyOnce() {
        UserRegistrationRequest request = new UserRegistrationRequest("John", "Doe", "john@example.com", "Password1!");

        String token = service.generate(request, TokenPurpose.EMAIL_VERIFICATION);
        EmailTokenEntry consumed = service.consume(new ConfirmEmailRequest(token), TokenPurpose.EMAIL_VERIFICATION);

        assertThat(consumed.email()).isEqualTo(request.getEmail());
        assertThat(consumed.registration().email()).isEqualTo(request.getEmail());
        assertThat(consumed.encodedPassword()).isEqualTo("encoded-password");
        assertThatThrownBy(() -> service.consume(new ConfirmEmailRequest(token), TokenPurpose.EMAIL_VERIFICATION))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("wrong token purpose is rejected")
    void wrongTokenPurposeIsRejected() {
        UserRegistrationRequest request = new UserRegistrationRequest("John", "Doe", "john@example.com", "Password1!");
        String token = service.generate(request, TokenPurpose.EMAIL_VERIFICATION);

        assertThatThrownBy(() -> service.consume(new ConfirmEmailRequest(token), TokenPurpose.PASSWORD_RESET))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("cooldown blocks repeated token generation until consumed")
    void cooldownBlocksRepeatedTokenGenerationUntilConsumed() {
        UserRegistrationRequest request = new UserRegistrationRequest("John", "Doe", "john@example.com", "Password1!");
        String token = service.generate(request, TokenPurpose.EMAIL_VERIFICATION);

        assertThatThrownBy(() -> service.generate(request, TokenPurpose.EMAIL_VERIFICATION))
                .isInstanceOf(TimeTokenException.class);

        service.consume(new ConfirmEmailRequest(token), TokenPurpose.EMAIL_VERIFICATION);

        assertThatCode(() -> service.generate(request, TokenPurpose.EMAIL_VERIFICATION))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("cooldown uses normalized email casing")
    void cooldownUsesNormalizedEmailCasing() {
        UserRegistrationRequest request = new UserRegistrationRequest("John", "Doe", "John@Example.com", "Password1!");
        UserRegistrationRequest sameEmailDifferentCase =
                new UserRegistrationRequest("John", "Doe", " john@example.COM ", "Password1!");

        service.generate(request, TokenPurpose.EMAIL_VERIFICATION);

        assertThatThrownBy(() -> service.generate(sameEmailDifferentCase, TokenPurpose.EMAIL_VERIFICATION))
                .isInstanceOf(TimeTokenException.class);
    }

    private static EmailTokenPayloadProtector tokenPayloadProtector(ObjectMapper objectMapper) {
        return new EmailTokenPayloadProtector(objectMapper, jwtProperties(), "");
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
}
