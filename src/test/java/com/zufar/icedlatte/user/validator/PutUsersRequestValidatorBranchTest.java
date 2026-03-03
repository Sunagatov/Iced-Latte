package com.zufar.icedlatte.user.validator;

import com.zufar.icedlatte.user.exception.PutUsersBadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

@DisplayName("PutUsersRequestValidator additional branch tests")
class PutUsersRequestValidatorBranchTest {

    private PutUsersRequestValidator validator;

    @BeforeEach
    void setUp() {
        validator = new PutUsersRequestValidator();
    }

    @Test
    @DisplayName("Future birth date throws PutUsersBadRequestException")
    void validate_futureBirthDate_throws() {
        String futureDate = LocalDate.now().plusDays(1).toString();
        assertThatThrownBy(() -> validator.validate("John", "Doe", null, futureDate, null))
                .isInstanceOf(PutUsersBadRequestException.class)
                .hasMessageContaining("must be in the past");
    }

    @Test
    @DisplayName("Birth date less than 13 years ago throws PutUsersBadRequestException")
    void validate_under13BirthDate_throws() {
        String recentDate = LocalDate.now().minusYears(10).toString();
        assertThatThrownBy(() -> validator.validate("John", "Doe", null, recentDate, null))
                .isInstanceOf(PutUsersBadRequestException.class)
                .hasMessageContaining("at least 13 years old");
    }

    @Test
    @DisplayName("Blank first name throws PutUsersBadRequestException")
    void validate_blankFirstName_throws() {
        assertThatThrownBy(() -> validator.validate("   ", "Doe", null, null, null))
                .isInstanceOf(PutUsersBadRequestException.class)
                .hasMessageContaining("must not be blank");
    }

    @Test
    @DisplayName("Name with digits throws PutUsersBadRequestException")
    void validate_nameWithDigits_throws() {
        assertThatThrownBy(() -> validator.validate("John123", "Doe", null, null, null))
                .isInstanceOf(PutUsersBadRequestException.class)
                .hasMessageContaining("can only contain letters");
    }

    @Test
    @DisplayName("Valid birth date exactly 13 years ago passes validation")
    void validate_exactly13YearsAgo_passes() {
        String date = LocalDate.now().minusYears(13).minusDays(1).toString();
        assertThatCode(() -> validator.validate("John", "Doe", null, date, null))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Phone number without leading plus throws PutUsersBadRequestException")
    void validate_phoneWithoutPlus_throws() {
        assertThatThrownBy(() -> validator.validate("John", "Doe", "12025550123", null, null))
                .isInstanceOf(PutUsersBadRequestException.class)
                .hasMessageContaining("E.164 format");
    }
}
