package com.zufar.icedlatte.security.endpoint;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;

import com.zufar.icedlatte.test.config.AuthenticatedUserIntegrationSupport;

@TestPropertySource(properties = "jwt.refresh-expiration=PT1S")
@DisplayName("Refresh token expiry integration tests")
class RefreshTokenExpiryIntegrationTest extends AuthenticatedUserIntegrationSupport {

    private static final String AUTH_BASE_PATH = "/api/v1/auth";

    @Test
    @DisplayName("Should return 401 when refresh token has expired")
    void shouldReturn401WhenRefreshTokenHasExpired() throws InterruptedException {
        AuthenticatedUser user = registerAndAuthenticateUser();

        Thread.sleep(1500);

        given(jsonSpec(AUTH_BASE_PATH).header("Authorization", "Bearer " + user.refreshToken()))
                .post("/refresh")
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value())
                .body("title", equalTo("Session expired"));
    }
}
