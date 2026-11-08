package com.zufar.icedlatte.security.endpoint;

import static io.restassured.RestAssured.given;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.zufar.icedlatte.test.config.AuthenticatedUserIntegrationSupport;

class RevokedRefreshIsolationTest extends AuthenticatedUserIntegrationSupport {
    @Test
    void oldTabAfterPasswordChangeCannotInvalidateAFreshLogin() {
        var user = registerAndAuthenticateUser();
        String newPassword = "another quiet river morning";
        given(authenticatedJsonSpec("/api/v1", user.accessToken()))
                .body(Map.of("oldPassword", user.password(), "newPassword", newPassword))
                .patch("/users")
                .then()
                .statusCode(200);
        var fresh = given(jsonSpec("/api/v1/auth"))
                .body(Map.of("email", user.email(), "password", newPassword))
                .post("/authenticate")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath();
        given(authenticatedJsonSpec("/api/v1/auth", user.refreshToken()))
                .post("/refresh")
                .then()
                .statusCode(401);
        given(authenticatedJsonSpec("/api/v1", fresh.getString("token")))
                .get("/users")
                .then()
                .statusCode(200);
        given(authenticatedJsonSpec("/api/v1/auth", fresh.getString("refreshToken")))
                .post("/refresh")
                .then()
                .statusCode(200);
    }
}
