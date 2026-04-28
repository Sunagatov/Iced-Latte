package com.zufar.icedlatte.security.configuration;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JwtProperties unit tests")
class JwtPropertiesTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    @DisplayName("accepts a fully populated configuration")
    void acceptsValidProperties() {
        JwtProperties properties = new JwtProperties(
                "Authorization",
                "secret",
                "refresh-secret",
                Duration.ofMinutes(15),
                Duration.ofDays(7),
                "iced-latte",
                "iced-latte-users"
        );

        assertThat(validator.validate(properties)).isEmpty();
        assertThat(properties.expiration()).isEqualTo(Duration.ofMinutes(15));
        assertThat(properties.refreshExpiration()).isEqualTo(Duration.ofDays(7));
    }

    @Test
    @DisplayName("rejects blank and null required fields")
    void rejectsBlankAndNullFields() {
        JwtProperties properties = new JwtProperties(
                "",
                " ",
                "",
                null,
                null,
                "",
                ""
        );

        var violations = validator.validate(properties);

        assertThat(violations).hasSize(7);
        assertThat(violations)
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactlyInAnyOrder(
                        "header",
                        "secret",
                        "refreshSecret",
                        "expiration",
                        "refreshExpiration",
                        "issuer",
                        "audience"
                );
    }
}
