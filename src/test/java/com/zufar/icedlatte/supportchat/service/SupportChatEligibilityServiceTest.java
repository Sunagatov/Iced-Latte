package com.zufar.icedlatte.supportchat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.zufar.icedlatte.supportchat.config.SupportChatProperties;
import com.zufar.icedlatte.supportchat.config.SupportChatProperties.OwnerMessageMode;
import com.zufar.icedlatte.supportchat.config.SupportChatProperties.Telegram;
import com.zufar.icedlatte.supportchat.config.SupportChatProperties.Turnstile;
import com.zufar.icedlatte.user.api.UserAuthenticationApi;
import com.zufar.icedlatte.user.api.UserAuthenticationSnapshot;

@DisplayName("SupportChatEligibilityService unit tests")
class SupportChatEligibilityServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();

    private final UserAuthenticationApi userAuthenticationApi = mock(UserAuthenticationApi.class);

    @Test
    @DisplayName("Allows verified user when email allow-list is empty")
    void eligibilityFor_emptyAllowedEmails_allowsVerifiedUser() {
        when(userAuthenticationApi.findUserAuthenticationById(USER_ID))
                .thenReturn(Optional.of(user("customer@example.com")));

        SupportChatEligibility eligibility = service(Set.of()).eligibilityFor(USER_ID);

        assertThat(eligibility.eligible()).isTrue();
        assertThat(eligibility.reason()).isNull();
    }

    @Test
    @DisplayName("Allows verified user whose email is allow-listed")
    void eligibilityFor_allowedEmail_allowsUser() {
        when(userAuthenticationApi.findUserAuthenticationById(USER_ID))
                .thenReturn(Optional.of(user("Owner@Example.com")));

        SupportChatEligibility eligibility =
                service(Set.of("owner@example.com")).eligibilityFor(USER_ID);

        assertThat(eligibility.eligible()).isTrue();
        assertThat(eligibility.reason()).isNull();
    }

    @Test
    @DisplayName("Rejects verified user whose email is not allow-listed")
    void eligibilityFor_emailNotAllowed_restrictsUser() {
        when(userAuthenticationApi.findUserAuthenticationById(USER_ID))
                .thenReturn(Optional.of(user("customer@example.com")));

        SupportChatEligibility eligibility =
                service(Set.of("owner@example.com")).eligibilityFor(USER_ID);

        assertThat(eligibility.eligible()).isFalse();
        assertThat(eligibility.reason()).isEqualTo("ACCESS_RESTRICTED");
    }

    @Test
    @DisplayName("Rejects unverified user before allow-list result")
    void eligibilityFor_unverifiedUser_requiresEmailVerification() {
        when(userAuthenticationApi.findUserAuthenticationById(USER_ID))
                .thenReturn(Optional.of(new UserAuthenticationSnapshot(
                        USER_ID, "owner@example.com", "encoded", List.of(), true, true, true, false)));

        SupportChatEligibility eligibility =
                service(Set.of("owner@example.com")).eligibilityFor(USER_ID);

        assertThat(eligibility.eligible()).isFalse();
        assertThat(eligibility.reason()).isEqualTo("EMAIL_VERIFICATION_REQUIRED");
    }

    private SupportChatEligibilityService service(Set<String> allowedEmails) {
        return new SupportChatEligibilityService(properties(allowedEmails), userAuthenticationApi);
    }

    private static SupportChatProperties properties(Set<String> allowedEmails) {
        return new SupportChatProperties(
                true,
                4000,
                90,
                OwnerMessageMode.FAKE,
                new Telegram("", "", 0L, "", true, Duration.ofSeconds(3), Duration.ofSeconds(5)),
                new Turnstile(false, Duration.ofHours(24), Duration.ofMinutes(5)),
                null,
                allowedEmails);
    }

    private static UserAuthenticationSnapshot user(String email) {
        return new UserAuthenticationSnapshot(USER_ID, email, "encoded", List.of(), true, true, true, true);
    }
}
