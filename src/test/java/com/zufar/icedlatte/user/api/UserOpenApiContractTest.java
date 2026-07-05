package com.zufar.icedlatte.user.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("User OpenAPI contract tests")
class UserOpenApiContractTest {

    private static final Path USER_OPENAPI_PATH = Path.of("src/main/resources/api-specs/user-openapi.yaml");

    @Test
    @DisplayName("password validation example matches PasswordField requirements")
    void passwordValidationExampleMatchesPasswordFieldRequirements() throws IOException {
        String yaml = Files.readString(USER_OPENAPI_PATH);

        assertThat(yaml)
                .contains("PasswordField:")
                .contains("(?=.*[a-z])")
                .contains("(?=.*[A-Z])")
                .contains("(?=.*\\\\d)")
                .contains("at least one lowercase letter")
                .contains("one uppercase letter")
                .contains("one digit")
                .contains(
                        "Password must be at least 8 characters long and contain at least one lowercase letter, one uppercase letter, and one digit.");
    }
}
