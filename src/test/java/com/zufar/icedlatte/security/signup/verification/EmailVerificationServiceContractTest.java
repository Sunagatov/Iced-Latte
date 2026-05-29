package com.zufar.icedlatte.security.signup.verification;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zufar.icedlatte.common.exception.BadRequestException;
import com.zufar.icedlatte.openapi.dto.ConfirmEmailRequest;
import com.zufar.icedlatte.openapi.dto.UserRegistrationRequest;
import com.zufar.icedlatte.security.service.cache.InMemoryExpiringKeyValueStore;
import com.zufar.icedlatte.security.session.dto.TokenPurpose;
import com.zufar.icedlatte.security.signup.exception.TimeTokenException;
import com.zufar.icedlatte.security.signup.registration.UserRegistrationService;
import com.zufar.icedlatte.user.api.UserAccessControlApi;
import com.zufar.icedlatte.user.api.UserLookupApi;

@DisplayName("EmailVerificationService contract tests")
class EmailVerificationServiceContractTest {

    private EmailVerificationService service;

    @BeforeEach
    void setUp() {
        service = new EmailVerificationService(
                new InMemoryExpiringKeyValueStore(new com.zufar.icedlatte.common.config.CaffeineSizeProperties(
                        1_000, 5_000, 10_000, 1_000, 10_000)),
                new ObjectMapper(),
                mock(com.zufar.icedlatte.security.email.sender.AuthTokenEmailSender.class),
                mock(UserRegistrationService.class),
                mock(UserLookupApi.class),
                mock(UserAccessControlApi.class));
        ReflectionTestUtils.setField(service, "expireTimeMinutes", 5);
        ReflectionTestUtils.setField(service, "tokenLength", 9);
    }

    @Test
    @DisplayName("generated token can be validated once and only once")
    void generatedTokenCanBeValidatedOnceAndOnlyOnce() {
        UserRegistrationRequest request = new UserRegistrationRequest("John", "Doe", "john@example.com", "Password1!");

        String token = service.generateToken(request, TokenPurpose.EMAIL_VERIFICATION);
        UserRegistrationRequest consumed =
                service.validateToken(new ConfirmEmailRequest(token), TokenPurpose.EMAIL_VERIFICATION);

        assertThat(consumed).usingRecursiveComparison().isEqualTo(request);
        assertThatThrownBy(() -> service.validateToken(new ConfirmEmailRequest(token), TokenPurpose.EMAIL_VERIFICATION))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("wrong token purpose is rejected")
    void wrongTokenPurposeIsRejected() {
        UserRegistrationRequest request = new UserRegistrationRequest("John", "Doe", "john@example.com", "Password1!");
        String token = service.generateToken(request, TokenPurpose.EMAIL_VERIFICATION);

        assertThatThrownBy(() -> service.validateToken(new ConfirmEmailRequest(token), TokenPurpose.PASSWORD_RESET))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("cooldown blocks repeated token generation until consumed")
    void cooldownBlocksRepeatedTokenGenerationUntilConsumed() {
        UserRegistrationRequest request = new UserRegistrationRequest("John", "Doe", "john@example.com", "Password1!");
        String token = service.generateToken(request, TokenPurpose.EMAIL_VERIFICATION);

        assertThatThrownBy(() -> service.generateToken(request, TokenPurpose.EMAIL_VERIFICATION))
                .isInstanceOf(TimeTokenException.class);

        service.validateToken(new ConfirmEmailRequest(token), TokenPurpose.EMAIL_VERIFICATION);

        assertThatCode(() -> service.generateToken(request, TokenPurpose.EMAIL_VERIFICATION))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("cooldown uses normalized email casing")
    void cooldownUsesNormalizedEmailCasing() {
        UserRegistrationRequest request = new UserRegistrationRequest("John", "Doe", "John@Example.com", "Password1!");
        UserRegistrationRequest sameEmailDifferentCase =
                new UserRegistrationRequest("John", "Doe", " john@example.COM ", "Password1!");

        service.generateToken(request, TokenPurpose.EMAIL_VERIFICATION);

        assertThatThrownBy(() -> service.generateToken(sameEmailDifferentCase, TokenPurpose.EMAIL_VERIFICATION))
                .isInstanceOf(TimeTokenException.class);
    }
}
