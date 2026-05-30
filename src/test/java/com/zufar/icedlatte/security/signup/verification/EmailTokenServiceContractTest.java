package com.zufar.icedlatte.security.signup.verification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zufar.icedlatte.common.exception.BadRequestException;
import com.zufar.icedlatte.openapi.dto.ConfirmEmailRequest;
import com.zufar.icedlatte.openapi.dto.UserRegistrationRequest;
import com.zufar.icedlatte.security.service.cache.InMemoryExpiringKeyValueStore;
import com.zufar.icedlatte.security.session.dto.TokenPurpose;
import com.zufar.icedlatte.security.signup.exception.TimeTokenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

@DisplayName("EmailTokenService contract tests")
class EmailTokenServiceContractTest {

    private EmailTokenService service;

    @BeforeEach
    void setUp() {
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        org.mockito.Mockito.when(passwordEncoder.encode(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn("encoded-password");
        service = new EmailTokenService(
                new InMemoryExpiringKeyValueStore(new com.zufar.icedlatte.common.config.CaffeineSizeProperties(
                        1_000, 5_000, 10_000, 1_000, 10_000)),
                new ObjectMapper(),
                passwordEncoder);
        ReflectionTestUtils.setField(service, "expireTimeMinutes", 5);
        ReflectionTestUtils.setField(service, "tokenLength", 43);
    }

    @Test
    @DisplayName("generated token can be validated once and only once")
    void generatedTokenCanBeValidatedOnceAndOnlyOnce() {
        UserRegistrationRequest request = new UserRegistrationRequest("John", "Doe", "john@example.com", "Password1!");

        String token = service.generate(request, TokenPurpose.EMAIL_VERIFICATION);
        UserRegistrationRequest consumed =
                service.consume(new ConfirmEmailRequest(token), TokenPurpose.EMAIL_VERIFICATION).request();

        assertThat(consumed.getEmail()).isEqualTo(request.getEmail());
        assertThat(consumed.getPassword()).isNull();
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
}
