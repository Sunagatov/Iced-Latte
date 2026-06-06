package com.zufar.icedlatte.user.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.zufar.icedlatte.common.exception.BadRequestException;

@DisplayName("PutUsersRequestValidator additional branch tests")
class PutUsersRequestValidatorBranchTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 6, 6);

    @Test
    @DisplayName("Future birth date throws BadRequestException")
    void validate_futureBirthDate_throws() {
        LocalDate futureDate = TODAY.plusDays(1);
        assertThatThrownBy(() -> PutUsersRequestValidator.validate("John", "Doe", null, futureDate, null, TODAY))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("must be in the past");
    }

    @Test
    @DisplayName("Birth date less than 13 years ago throws BadRequestException")
    void validate_under13BirthDate_throws() {
        LocalDate recentDate = TODAY.minusYears(10);
        assertThatThrownBy(() -> PutUsersRequestValidator.validate("John", "Doe", null, recentDate, null, TODAY))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("at least 13 years old");
    }

    @Test
    @DisplayName("Blank first name throws BadRequestException")
    void validate_blankFirstName_throws() {
        assertThatThrownBy(() -> PutUsersRequestValidator.validate("   ", "Doe", null, null, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("must not be blank");
    }

    @Test
    @DisplayName("Name with digits throws BadRequestException")
    void validate_nameWithDigits_throws() {
        assertThatThrownBy(() -> PutUsersRequestValidator.validate("John123", "Doe", null, null, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("can only contain letters");
    }

    @Test
    @DisplayName("Valid birth date exactly 13 years ago passes validation")
    void validate_exactly13YearsAgo_passes() {
        LocalDate date = TODAY.minusYears(13).minusDays(1);
        assertThatCode(() -> PutUsersRequestValidator.validate("John", "Doe", null, date, null, TODAY))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Phone number without leading plus throws BadRequestException")
    void validate_phoneWithoutPlus_throws() {
        assertThatThrownBy(() -> PutUsersRequestValidator.validate("John", "Doe", "12025550123", null, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("E.164 format");
    }
}
