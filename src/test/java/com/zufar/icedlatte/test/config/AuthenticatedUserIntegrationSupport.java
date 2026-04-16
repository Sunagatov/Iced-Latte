package com.zufar.icedlatte.test.config;

import com.zufar.icedlatte.email.api.token.TokenManager;
import com.zufar.icedlatte.email.api.token.TokenPurpose;
import com.zufar.icedlatte.openapi.dto.UserRegistrationRequest;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;

import static io.restassured.RestAssured.given;

public abstract class AuthenticatedUserIntegrationSupport extends IntegrationTestBase {

    private static final String AUTH_BASE_PATH = "/api/v1/auth";

    @LocalServerPort
    protected Integer port;

    @Autowired
    private TokenManager tokenManager;

    protected RequestSpecification jsonSpec(String basePath) {
        return given()
                .port(port)
                .basePath(basePath)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON);
    }

    protected RequestSpecification authenticatedJsonSpec(String basePath, String accessToken) {
        return jsonSpec(basePath)
                .header("Authorization", "Bearer " + accessToken);
    }

    protected AuthenticatedUser registerAndAuthenticateUser() {
        long suffix = System.nanoTime();
        String email = "integration." + suffix + "@example.com";
        String password = "Password123!";

        return registerAndAuthenticateUser("Integration", "User", email, password);
    }

    protected AuthenticatedUser registerAndAuthenticateUser(
            String firstName,
            String lastName,
            String email,
            String password
    ) {
        UserRegistrationRequest pending = new UserRegistrationRequest(firstName, lastName, email, password);
        String confirmationToken = tokenManager.generateToken(pending, TokenPurpose.EMAIL_VERIFICATION);

        var response = given(jsonSpec(AUTH_BASE_PATH))
                .body("""
                        {"token":"%s"}
                        """.formatted(confirmationToken))
                .post("/confirm")
                .then()
                .statusCode(HttpStatus.CREATED.value())
                .extract()
                .jsonPath();

        return new AuthenticatedUser(
                response.getString("token"),
                response.getString("refreshToken"),
                email,
                password
        );
    }

    public record AuthenticatedUser(
            String accessToken,
            String refreshToken,
            String email,
            String password
    ) {
    }
}