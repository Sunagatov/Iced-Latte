package com.zufar.icedlatte.security.endpoint;

import com.zufar.icedlatte.email.api.EmailVerificationService;
import com.zufar.icedlatte.email.api.token.TokenPurpose;
import com.zufar.icedlatte.openapi.dto.UserRegistrationRequest;
import com.zufar.icedlatte.test.config.IntegrationTestBase;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;

import static io.restassured.RestAssured.given;

@DisplayName("Refresh Token Replay Tests")
class RefreshTokenReplayTest extends IntegrationTestBase {

    @LocalServerPort
    private Integer port;

    @Autowired
    private EmailVerificationService emailVerificationService;

    private RequestSpecification spec;

    @BeforeEach
    void setup() {
        spec = given()
                .port(port)
                .basePath("/api/v1/auth")
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON);
    }

    /**
     * Registers, confirms, and authenticates a unique user.
     * Returns the raw refresh token from the authenticate response.
     */
    private String registerAndGetRefreshToken(String email) {
        UserRegistrationRequest pending = new UserRegistrationRequest(
                "Replay", "Test", email, "!h2h3kKl22");
        String token = emailVerificationService.generateToken(pending, TokenPurpose.EMAIL_VERIFICATION);

        given(spec).body("{\"token\":\"" + token + "\"}").post("/confirm")
                .then().statusCode(HttpStatus.CREATED.value());

        return given(spec)
                .body("{\"email\":\"" + email + "\",\"password\":\"" + "!h2h3kKl22" + "\"}")
                .post("/authenticate")
                .then().statusCode(HttpStatus.OK.value())
                .extract().jsonPath().getString("refreshToken");
    }

    @Test
    @DisplayName("Rotated refresh token must return 401, not 500")
    void rotatedRefreshTokenReturns401() {
        String email = "replay." + System.currentTimeMillis() + "@example.com";
        String originalRefreshToken = registerAndGetRefreshToken(email);

        // First refresh — rotates the token; originalRefreshToken is now stale
        given(spec)
                .header("Authorization", "Bearer " + originalRefreshToken)
                .post("/refresh")
                .then().statusCode(HttpStatus.OK.value());

        // Second refresh with the now-rotated (stale) token — must be 401, not 500
        given(spec)
                .header("Authorization", "Bearer " + originalRefreshToken)
                .post("/refresh")
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    @DisplayName("Valid refresh token must return 200")
    void validRefreshTokenReturns200() {
        String email = "valid.refresh." + System.currentTimeMillis() + "@example.com";
        String refreshToken = registerAndGetRefreshToken(email);

        given(spec)
                .header("Authorization", "Bearer " + refreshToken)
                .post("/refresh")
                .then()
                .statusCode(HttpStatus.OK.value());
    }
}
