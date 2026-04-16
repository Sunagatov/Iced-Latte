package com.zufar.icedlatte.user.endpoint;

import com.zufar.icedlatte.test.config.AuthenticatedUserIntegrationSupport;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static io.restassured.RestAssured.given;

@DisplayName("User avatar endpoint integration tests")
class UserAvatarEndpointIntegrationTest extends AuthenticatedUserIntegrationSupport {

    private static final String BASE_PATH = "/api/v1/users";

    @Test
    @DisplayName("Should return not found when avatar does not exist")
    void shouldReturnNotFoundWhenAvatarDoesNotExist() {
        AuthenticatedUser user = registerAndAuthenticateUser();

        given()
                .port(port)
                .basePath(BASE_PATH)
                .header("Authorization", "Bearer " + user.accessToken())
                .accept(ContentType.JSON)
                .get("/avatar")
                .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
    }

    @Test
    @DisplayName("Should delete avatar successfully when avatar does not exist")
    void shouldDeleteAvatarSuccessfullyWhenAvatarDoesNotExist() {
        AuthenticatedUser user = registerAndAuthenticateUser();

        given()
                .port(port)
                .basePath(BASE_PATH)
                .header("Authorization", "Bearer " + user.accessToken())
                .accept(ContentType.JSON)
                .delete("/avatar")
                .then()
                .statusCode(HttpStatus.OK.value());

        given()
                .port(port)
                .basePath(BASE_PATH)
                .header("Authorization", "Bearer " + user.accessToken())
                .accept(ContentType.JSON)
                .get("/avatar")
                .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
    }

    @Test
    @DisplayName("Should return bad request for invalid avatar content type")
    void shouldReturnBadRequestForInvalidAvatarContentType() {
        AuthenticatedUser user = registerAndAuthenticateUser();

        given()
                .port(port)
                .basePath(BASE_PATH)
                .header("Authorization", "Bearer " + user.accessToken())
                .accept(ContentType.JSON)
                .multiPart("file", "avatar.txt", "not-an-image".getBytes(), "text/plain")
                .post("/avatar")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    @DisplayName("Should return bad request for invalid avatar magic bytes")
    void shouldReturnBadRequestForInvalidAvatarMagicBytes() {
        AuthenticatedUser user = registerAndAuthenticateUser();

        given()
                .port(port)
                .basePath(BASE_PATH)
                .header("Authorization", "Bearer " + user.accessToken())
                .accept(ContentType.JSON)
                .multiPart("file", "avatar.jpg", "fake-jpeg-content".getBytes(), "image/jpeg")
                .post("/avatar")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value());
    }
}