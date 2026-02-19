package com.zufar.icedlatte.security.endpoint;

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
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static com.zufar.icedlatte.test.config.RestAssertion.*;
import static com.zufar.icedlatte.test.config.RestUtils.getRequestBody;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.Matchers.*;

import org.springframework.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;

@Testcontainers
@DisplayName("SecurityEndpoint Tests")
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SecurityEndpointTest {
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:13.11-bullseye");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void dataSourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379).toString());
    }

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
    private static final String AUTH_BASE_PATH = "/api/v1/auth";
    private static final String TOKEN_FIELD = "token";
    private static final String TOKEN_NULL_MESSAGE = "Token should not be null";

    protected static RequestSpecification specification;

    @BeforeEach
    void setupSpecification() {
        specification = given()
                .log().all(true)
                .port(port)
                .basePath(AUTH_BASE_PATH)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON);
    }

    @Test
    @Disabled("Registration flow changed to email-verification-first: /register now sends a verification email instead of returning a token directly. Test needs rewriting to mock mail sender or use the 2-step flow.")
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

        assertNotNull(response.getBody().path(TOKEN_FIELD), TOKEN_NULL_MESSAGE);
    }

    @Test
    @DisplayName("Should fail registration without name")
    void shouldFailRegistrationWithoutName() {
        String body = getRequestBody(SECURITY_REGISTRATION_WITHOUT_NAME);

        Response response = given(specification)
                .body(body)
                .post("/register");

        response.then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("message", notNullValue())
                .body("httpStatusCode", equalTo(400));
    }

    @Test
    @DisplayName("Should fail registration with short name")
    void shouldFailRegistrationWithShortName() {
        String body = getRequestBody(SECURITY_REGISTRATION_LENGTH_NAME_LESS_TWO_WORD);

        Response response = given(specification)
                .body(body)
                .post("/register");

        response.then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("message", notNullValue());
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
    @Disabled("Registration flow changed to email-verification-first: /register now sends a verification email instead of returning a token directly. Test needs rewriting to mock mail sender or use the 2-step flow.")
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
    @Disabled("Registration flow changed to email-verification-first: /register now sends a verification email instead of returning a token directly. Test needs rewriting to mock mail sender or use the 2-step flow.")
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
    @DisplayName("Should fail authentication for non-existent user")
    void shouldFailAuthenticationForNonExistentUser() {
        String body = getRequestBody(SECURITY_AUTHENTICATE_USER_NOT_FOUND);

        Response response = given(specification)
                .body(body)
                .post("/authenticate");

        response.then()
                .statusCode(HttpStatus.UNAUTHORIZED.value())
                .body("message", notNullValue());
    }

    @Test
    @DisplayName("Should fail authentication with incorrect password")
    void shouldFailAuthenticationWithIncorrectPassword() {
        String body = getRequestBody(SECURITY_AUTHENTICATE_INCORRECT_PASSWORD);

        Response response = given(specification)
                .body(body)
                .post("/authenticate");

        response.then()
                .statusCode(HttpStatus.UNAUTHORIZED.value())
                .body("message", notNullValue());
    }

}
