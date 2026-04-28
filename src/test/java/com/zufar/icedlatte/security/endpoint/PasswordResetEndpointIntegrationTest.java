package com.zufar.icedlatte.security.endpoint;

import com.zufar.icedlatte.email.api.token.TokenManager;
import com.zufar.icedlatte.email.api.token.TokenPurpose;
import com.zufar.icedlatte.openapi.dto.UserRegistrationRequest;
import com.zufar.icedlatte.test.config.AuthenticatedUserIntegrationSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;

@DisplayName("Password reset integration tests")
class PasswordResetEndpointIntegrationTest extends AuthenticatedUserIntegrationSupport {

    private static final String AUTH_BASE_PATH = "/api/v1/auth";

    @Autowired
    private TokenManager tokenManager;

    @Test
    @DisplayName("Should return OK for forgot-password for both known and unknown email")
    void shouldReturnOkForForgotPasswordForBothKnownAndUnknownEmail() {
        AuthenticatedUser user = registerAndAuthenticateUser();

        given(jsonSpec(AUTH_BASE_PATH))
                .body("""
                        {
                          "email": "%s"
                        }
                        """.formatted(user.email()))
                .post("/password/forgot")
                .then()
                .statusCode(HttpStatus.OK.value());

        given(jsonSpec(AUTH_BASE_PATH))
                .body("""
                        {
                          "email": "unknown.%d@example.com"
                        }
                        """.formatted(System.nanoTime()))
                .post("/password/forgot")
                .then()
                .statusCode(HttpStatus.OK.value());
    }

    @Test
    @DisplayName("Should reset password using valid reset token and authenticate with new password")
    void shouldResetPasswordUsingValidResetTokenAndAuthenticateWithNewPassword() {
        AuthenticatedUser user = registerAndAuthenticateUser();
        String newPassword = "ResetPass123!";

        UserRegistrationRequest resetRequest = new UserRegistrationRequest();
        resetRequest.setEmail(user.email());

        String resetToken = tokenManager.generateToken(resetRequest, TokenPurpose.PASSWORD_RESET);

        given(jsonSpec(AUTH_BASE_PATH))
                .body("""
                        {
                          "code": "%s",
                          "password": "%s"
                        }
                        """.formatted(resetToken, newPassword))
                .post("/password/change")
                .then()
                .statusCode(HttpStatus.OK.value());

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

        given(jsonSpec(AUTH_BASE_PATH))
                .body("""
                        {
                          "email": "%s",
                          "password": "%s"
                        }
                        """.formatted(user.email(), newPassword))
                .post("/authenticate")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("token", notNullValue())
                .body("refreshToken", notNullValue());
    }

    @Test
    @DisplayName("Should reject password change with invalid reset token")
    void shouldRejectPasswordChangeWithInvalidResetToken() {
        given(jsonSpec(AUTH_BASE_PATH))
                .body("""
                        {
                          "code": "123456789",
                          "password": "ResetPass123!"
                        }
                        """)
                .post("/password/change")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    @DisplayName("Should reject reused reset token after successful password change")
    void shouldRejectReusedResetTokenAfterSuccessfulPasswordChange() {
        AuthenticatedUser user = registerAndAuthenticateUser();
        String newPassword = "ResetPass123!";

        UserRegistrationRequest resetRequest = new UserRegistrationRequest();
        resetRequest.setEmail(user.email());

        String resetToken = tokenManager.generateToken(resetRequest, TokenPurpose.PASSWORD_RESET);

        given(jsonSpec(AUTH_BASE_PATH))
                .body("""
                        {
                          "code": "%s",
                          "password": "%s"
                        }
                        """.formatted(resetToken, newPassword))
                .post("/password/change")
                .then()
                .statusCode(HttpStatus.OK.value());

        given(jsonSpec(AUTH_BASE_PATH))
                .body("""
                        {
                          "code": "%s",
                          "password": "AnotherPass123!"
                        }
                        """.formatted(resetToken))
                .post("/password/change")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value());
    }
}
