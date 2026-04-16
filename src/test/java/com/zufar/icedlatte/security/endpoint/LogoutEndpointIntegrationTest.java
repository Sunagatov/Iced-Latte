package com.zufar.icedlatte.security.endpoint;

import com.zufar.icedlatte.test.config.AuthenticatedUserIntegrationSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasSize;

@DisplayName("Logout endpoint integration tests")
class LogoutEndpointIntegrationTest extends AuthenticatedUserIntegrationSupport {

    private static final String AUTH_BASE_PATH = "/api/v1/auth";

    @Test
    @DisplayName("Should blacklist access token and refresh token on logout")
    void shouldBlacklistAccessTokenAndRefreshTokenOnLogout() {
        AuthenticatedUser user = registerAndAuthenticateUser();

        given(authenticatedJsonSpec(AUTH_BASE_PATH, user.accessToken())
                .header("X-Refresh-Token", user.refreshToken()))
                .post("/logout")
                .then()
                .statusCode(HttpStatus.OK.value());

        given(jsonSpec(AUTH_BASE_PATH)
                .header("Authorization", "Bearer " + user.refreshToken()))
                .post("/refresh")
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value());

        given(authenticatedJsonSpec(AUTH_BASE_PATH, user.accessToken()))
                .get("/sessions")
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    @DisplayName("Should revoke all active sessions on logout-all")
    void shouldRevokeAllActiveSessionsOnLogoutAll() {
        AuthenticatedUser firstSession = registerAndAuthenticateUser();
        AuthenticatedUser secondSession = authenticate(firstSession.email(), firstSession.password());

        given(authenticatedJsonSpec(AUTH_BASE_PATH, firstSession.accessToken()))
                .post("/logout-all")
                .then()
                .statusCode(HttpStatus.OK.value());

        given(jsonSpec(AUTH_BASE_PATH)
                .header("Authorization", "Bearer " + firstSession.refreshToken()))
                .post("/refresh")
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value());

        given(jsonSpec(AUTH_BASE_PATH)
                .header("Authorization", "Bearer " + secondSession.refreshToken()))
                .post("/refresh")
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value());

        given(authenticatedJsonSpec(AUTH_BASE_PATH, firstSession.accessToken()))
                .get("/sessions")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("", hasSize(0));
    }

    private AuthenticatedUser authenticate(String email, String password) {
        var response = given(jsonSpec(AUTH_BASE_PATH))
                .body("""
                        {
                          "email": "%s",
                          "password": "%s"
                        }
                        """.formatted(email, password))
                .post("/authenticate")
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .jsonPath();

        return new AuthenticatedUser(
                response.getString("token"),
                response.getString("refreshToken"),
                email,
                password
        );
    }
}