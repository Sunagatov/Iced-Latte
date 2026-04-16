package com.zufar.icedlatte.user.endpoint;

import com.zufar.icedlatte.test.config.AuthenticatedUserIntegrationSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static io.restassured.RestAssured.given;

@DisplayName("User account deletion integration tests")
class UserAccountDeletionIntegrationTest extends AuthenticatedUserIntegrationSupport {

    private static final String USER_BASE_PATH = "/api/v1/users";
    private static final String AUTH_BASE_PATH = "/api/v1/auth";

    @Test
    @DisplayName("Should delete current user account and block further access")
    void shouldDeleteCurrentUserAccountAndBlockFurtherAccess() {
        AuthenticatedUser user = registerAndAuthenticateUser();

        given(authenticatedJsonSpec(USER_BASE_PATH, user.accessToken()))
                .delete()
                .then()
                .statusCode(HttpStatus.OK.value());

        given(authenticatedJsonSpec(USER_BASE_PATH, user.accessToken()))
                .get()
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value());

        given(jsonSpec(AUTH_BASE_PATH))
                .body("""
                        {
                          "email": "%s",
                          "password": "%s"
                        }
                        """.formatted(user.email(), user.password()))
                .post("/authenticate")
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value());
    }
}