package com.zufar.icedlatte.security.config;

import static io.restassured.RestAssured.given;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;

import com.zufar.icedlatte.test.config.IntegrationTestBase;

import io.restassured.http.ContentType;

@DisplayName("Security default-deny integration tests")
class SecurityDefaultDenyIntegrationTest extends IntegrationTestBase {

    @LocalServerPort
    private Integer port;

    @Test
    @DisplayName("unknown API paths require authentication by default")
    void unknownApiPathsRequireAuthenticationByDefault() {
        given().port(port)
                .accept(ContentType.JSON)
                .get("/api/v1/internal/recalculate-prices")
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    @DisplayName("non API paths are not public by default")
    void nonApiPathsAreDeniedByDefault() {
        given().port(port)
                .accept(ContentType.JSON)
                .get("/internal/recalculate-prices")
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value());
    }
}
