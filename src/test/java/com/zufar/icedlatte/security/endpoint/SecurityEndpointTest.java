package com.zufar.icedlatte.security.endpoint;

import com.github.fge.jackson.JsonLoader;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;

import static com.zufar.icedlatte.test.config.RestAssertion.assertRestApiBadRequestResponse;
import static com.zufar.icedlatte.test.config.RestAssertion.assertRestApiCreateResponse;
import static com.zufar.icedlatte.test.config.RestAssertion.assertRestApiOkResponse;
import static com.zufar.icedlatte.test.config.RestAssertion.assertRestApiUnAuthorizedResponse;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Testcontainers
@DisplayName("SecurityEndpoint Tests")
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SecurityEndpointTest {
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:13.11-bullseye");

    @LocalServerPort
    protected Integer port;

    private static final String SECURITY_SCHEMA = "security/model/schema/security-schema.json";
    private static final String SECURITY_SCHEMA_FAILED = "security/model/schema/security-schema-failed.json";
    private static final String SECURITY_REGISTRATION = "/security/model/security-registration.json";
    private static final String SECURITY_REGISTRATION_FOR_AUTH = "/security/model/security-registration-for-auth.json";
    private static final String SECURITY_AUTHENTICATE = "/security/model/security-authenticate.json";
    private static final String SECURITY_AUTHENTICATE_INCORRECT_PASSWORD = "/security/model/security-authenticate-incorrect-password.json";
    private static final String SECURITY_AUTHENTICATE_USER_NOT_FOUND = "/security/model/security-authenticate-not-found-user.json";

    private static final String SECURITY_REGISTRATION_WITHOUT_NAME = "/security/model/security-registration-without-name.json";
    private static final String SECURITY_REGISTRATION_LENGTH_NAME_LESS_TWO_WORD = "/security/model/security-registration-length-name-less-two-word.json";
    private static final String SECURITY_REGISTRATION_LENGTH_NAME_MORE_128_WORD = "/security/model/security-registration-length-name-more-128-word.json";
    private static final String SECURITY_REGISTRATION_WITHOUT_LAST_NAME = "/security/model/security-registration-without-last-name.json";
    private static final String SECURITY_REGISTRATION_LENGTH_LAST_NAME_LESS_TWO_WORD = "/security/model/security-registration-length-last-name-less-two-word.json";
    private static final String SECURITY_REGISTRATION_LENGTH_LAST_NAME_MORE_128_WORD = "/security/model/security-registration-length-last-name-more-128-word.json";
    private static final String SECURITY_REGISTRATION_NOT_UNIQUE_EMAIL = "/security/model/security-registration-not-unique-email.json";
    private static final String SECURITY_REGISTRATION_EMPTY_EMAIL = "/security/model/security-registration-empty-email.json";
    private static final String SECURITY_REGISTRATION_LENGTH_EMAIL_LESS_EIGHT_WORD = "/security/model/security-registration-length-email-less-eight-word.json";
    private static final String SECURITY_REGISTRATION_LENGTH_EMAIL_MORE_128_WORD = "/security/model/security-registration-length-email-more-128-word.json";
    private static final String SECURITY_REGISTRATION_EMPTY_PASSWORD = "/security/model/security-registration-empty-password.json";
    private static final String SECURITY_REGISTRATION_LENGTH_PASSWORD_LESS_EIGHT_CHARACTERS = "/security/model/security-registration-length-password-less-eight-characters.json";
    private static final String SECURITY_REGISTRATION_LENGTH_PASSWORD_MORE_128_CHARACTERS = "/security/model/security-registration-length-password-more-128-characters.json";
    private static final String SECURITY_REGISTRATION_PASSWORD_WITHOUT_WORD = "/security/model/security-registration-password-without-word.json";


    protected static RequestSpecification specification;

    protected String getRequestBody(String resourcePath) {
        try {
            JsonNode json = JsonLoader.fromResource(resourcePath);
            return json.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @Disabled("This is temporary to unblock SQ")
    @DisplayName("Should registration new user")
    void shouldRegistrationNewUser() {
        String body = getRequestBody(SECURITY_REGISTRATION);

        specification = given()
                .log().all(true)
                .port(port)
                .basePath("/api/v1/auth")
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON);

        Response response = given(specification)
                .body(body)
                .post("/register");

        assertRestApiCreateResponse(response, SECURITY_SCHEMA);

        assertNotNull(response.getBody().path("token"), "Token should not be null");
    }

    @Test
    @DisplayName("Should registration new user Failed without name")
    void shouldRegistrationNewUserFailedWithoutName() {
        String body = getRequestBody(SECURITY_REGISTRATION_WITHOUT_NAME);

        specification = given()
                .log().all(true)
                .port(port)
                .basePath("/api/v1/auth")
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON);

        Response response = given(specification)
                .body(body)
                .post("/register");

        assertRestApiBadRequestResponse(response, SECURITY_SCHEMA_FAILED);
    }

    @Test
    @DisplayName("Should registration new user Failed length name less two word")
    void shouldRegistrationNewUserFailedLengthNameLessTwoWord() {
        String body = getRequestBody(SECURITY_REGISTRATION_LENGTH_NAME_LESS_TWO_WORD);

        specification = given()
                .log().all(true)
                .port(port)
                .basePath("/api/v1/auth")
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON);

        Response response = given(specification)
                .body(body)
                .post("/register");

        assertRestApiBadRequestResponse(response, SECURITY_SCHEMA_FAILED);
    }

    @Test
    @DisplayName("Should registration new user Failed length name more one hundred twenty-eight word")
    void shouldRegistrationNewUserFailedLengthNameMoreOneHundredTwentyEightWord() {
        String body = getRequestBody(SECURITY_REGISTRATION_LENGTH_NAME_MORE_128_WORD);

        specification = given()
                .log().all(true)
                .port(port)
                .basePath("/api/v1/auth")
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON);

        Response response = given(specification)
                .body(body)
                .post("/register");

        assertRestApiBadRequestResponse(response, SECURITY_SCHEMA_FAILED);
    }

    @Test
    @DisplayName("Should registration new user Failed length last name less two word")
    void shouldRegistrationNewUserFailedLengthLastNameLessTwoWord() {
        String body = getRequestBody(SECURITY_REGISTRATION_LENGTH_LAST_NAME_LESS_TWO_WORD);

        specification = given()
                .log().all(true)
                .port(port)
                .basePath("/api/v1/auth")
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON);

        Response response = given(specification)
                .body(body)
                .post("/register");

        assertRestApiBadRequestResponse(response, SECURITY_SCHEMA_FAILED);
    }

    @Test
    @DisplayName("Should registration new user Failed length last name more one hundred twenty-eight word")
    void shouldRegistrationNewUserFailedLengthLastNameMoreOneHundredTwentyEightWord() {
        String body = getRequestBody(SECURITY_REGISTRATION_LENGTH_LAST_NAME_MORE_128_WORD);

        specification = given()
                .log().all(true)
                .port(port)
                .basePath("/api/v1/auth")
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON);

        Response response = given(specification)
                .body(body)
                .post("/register");

        assertRestApiBadRequestResponse(response, SECURITY_SCHEMA_FAILED);
    }

    @Test
    @DisplayName("Should registration new user Failed without last name")
    void shouldRegistrationNewUserFailedWithoutLastName() {
        String body = getRequestBody(SECURITY_REGISTRATION_WITHOUT_LAST_NAME);

        specification = given()
                .log().all(true)
                .port(port)
                .basePath("/api/v1/auth")
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON);

        Response response = given(specification)
                .body(body)
                .post("/register");

        assertRestApiBadRequestResponse(response, SECURITY_SCHEMA_FAILED);
    }

    @Test
    @DisplayName("Should registration new user Failed Email empty")
    void shouldRegistrationNewUserFailedEmailEmpty() {
        String body = getRequestBody(SECURITY_REGISTRATION_EMPTY_EMAIL);

        specification = given()
                .log().all(true)
                .port(port)
                .basePath("/api/v1/auth")
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON);

        Response response = given(specification)
                .body(body)
                .post("/register");

        assertRestApiBadRequestResponse(response, SECURITY_SCHEMA_FAILED);
    }

    @Test
    @Disabled("This is temporary to unblock SQ")
    @DisplayName("Should registration new user Failed email not unique")
    void shouldRegistrationNewUserFailedEmailNotUnique() {
        String body = getRequestBody(SECURITY_REGISTRATION_NOT_UNIQUE_EMAIL);

        specification = given()
                .log().all(true)
                .port(port)
                .basePath("/api/v1/auth")
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON);

        Response response = given(specification)
                .body(body)
                .post("/register");

        assertRestApiBadRequestResponse(response, SECURITY_SCHEMA_FAILED);
    }

    @Test
    @DisplayName("Should registration new user Failed empty password")
    void shouldRegistrationNewUserFailedPasswordEmpty() {
        String body = getRequestBody(SECURITY_REGISTRATION_EMPTY_PASSWORD);

        specification = given()
                .log().all(true)
                .port(port)
                .basePath("/api/v1/auth")
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON);

        Response response = given(specification)
                .body(body)
                .post("/register");

        assertRestApiBadRequestResponse(response, SECURITY_SCHEMA_FAILED);
    }

    @Test
    @DisplayName("Should registration new user Failed length email less eight word")
    void shouldRegistrationNewUserFailedLengthEmailLessEightWord() {
        String body = getRequestBody(SECURITY_REGISTRATION_LENGTH_EMAIL_LESS_EIGHT_WORD);

        specification = given()
                .log().all(true)
                .port(port)
                .basePath("/api/v1/auth")
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON);

        Response response = given(specification)
                .body(body)
                .post("/register");

        assertRestApiBadRequestResponse(response, SECURITY_SCHEMA_FAILED);
    }

    @Test
    @DisplayName("Should registration new user Failed length email more one hundred-eight word")
    void shouldRegistrationNewUserFailedLengthEmailMoreOneHundredEightWord() {
        String body = getRequestBody(SECURITY_REGISTRATION_LENGTH_EMAIL_MORE_128_WORD);

        specification = given()
                .log().all(true)
                .port(port)
                .basePath("/api/v1/auth")
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON);

        Response response = given(specification)
                .body(body)
                .post("/register");

        assertRestApiBadRequestResponse(response, SECURITY_SCHEMA_FAILED);
    }

    @Test
    @DisplayName("Should registration new user Failed length password less eight characters")
    void shouldRegistrationNewUserFailedLengthPasswordLessEightCharacters() {
        String body = getRequestBody(SECURITY_REGISTRATION_LENGTH_PASSWORD_LESS_EIGHT_CHARACTERS);

        specification = given()
                .log().all(true)
                .port(port)
                .basePath("/api/v1/auth")
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON);

        Response response = given(specification)
                .body(body)
                .post("/register");

        assertRestApiBadRequestResponse(response, SECURITY_SCHEMA_FAILED);
    }

    @Test
    @DisplayName("Should registration new user Failed length password more one hundred characters")
    void shouldRegistrationNewUserFailedLengthPasswordMoreOneHundredEightCharacters() {
        String body = getRequestBody(SECURITY_REGISTRATION_LENGTH_PASSWORD_MORE_128_CHARACTERS);

        specification = given()
                .log().all(true)
                .port(port)
                .basePath("/api/v1/auth")
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON);

        Response response = given(specification)
                .body(body)
                .post("/register");

        assertRestApiBadRequestResponse(response, SECURITY_SCHEMA_FAILED);
    }

    @Test
    @DisplayName("Should registration new user Failed password without word")
    void shouldRegistrationNewUserFailedPasswordWithoutWord() {
        String body = getRequestBody(SECURITY_REGISTRATION_PASSWORD_WITHOUT_WORD);

        specification = given()
                .log().all(true)
                .port(port)
                .basePath("/api/v1/auth")
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON);

        Response response = given(specification)
                .body(body)
                .post("/register");

        assertRestApiBadRequestResponse(response, SECURITY_SCHEMA_FAILED);
    }

    @Test
    @Disabled("This is temporary to unblock SQ")
    @DisplayName("Should authenticate user")
    void shouldAuthenticateUser() {
        String body = getRequestBody(SECURITY_AUTHENTICATE);
        String bodyRegistration = getRequestBody(SECURITY_REGISTRATION_FOR_AUTH);

        specification = given()
                .log().all(true)
                .port(port)
                .basePath("/api/v1/auth")
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON);

        given(specification)
                .body(bodyRegistration)
                .post("/register");

        Response response = given(specification)
                .body(body)
                .post("/authenticate");

        assertRestApiOkResponse(response, SECURITY_SCHEMA);
    }

    @Test
    @DisplayName("Should authenticate user notfound")
    void shouldAuthenticateUserNotFound() {
        String body = getRequestBody(SECURITY_AUTHENTICATE_USER_NOT_FOUND);

        specification = given()
                .log().all(true)
                .port(port)
                .basePath("/api/v1/auth")
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON);

        Response response = given(specification)
                .body(body)
                .post("/authenticate");

        assertRestApiUnAuthorizedResponse(response, SECURITY_SCHEMA_FAILED);
    }

    @Test
    @DisplayName("Should authenticate incorrect password")
    void shouldAuthenticateIncorrectPassword() {
        String body = getRequestBody(SECURITY_AUTHENTICATE_INCORRECT_PASSWORD);

        specification = given()
                .log().all(true)
                .port(port)
                .basePath("/api/v1/auth")
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON);

        Response response = given(specification)
                .body(body)
                .post("/authenticate");

        assertRestApiUnAuthorizedResponse(response, SECURITY_SCHEMA_FAILED);
    }

}
