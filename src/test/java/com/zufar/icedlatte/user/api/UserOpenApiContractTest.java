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
                .contains("minLength: 15")
                .contains("long passphrases are allowed")
                .contains("Use a password between 15 and 128 characters.")
                .contains("Existing password; legacy creation rules do not apply.")
                .doesNotContain("(?=.*[A-Z])");
    }
}
