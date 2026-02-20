package com.zufar.icedlatte.security.endpoint;

import com.zufar.icedlatte.email.api.token.TokenManager;
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

import static com.zufar.icedlatte.test.config.RestAssertion.*;
import static com.zufar.icedlatte.test.config.RestUtils.getRequestBody;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@DisplayName("SecurityEndpoint Tests")
class SecurityEndpointTest extends IntegrationTestBase {

    @LocalServerPort
    protected Integer port;

    @Autowired
    private TokenManager tokenManager;

    private static final String SECURITY_SCHEMA = "security/model/schema/security-schema.json";
    private static final String SECURITY_SCHEMA_FAILED = "security/model/schema/security-schema-failed.json";
    private static final String SECURITY_REGISTRATION = "/security/model/security-registration.json";
    private static final String SECURITY_AUTHENTICATE = "/security/model/security-authenticate.json";
    private static final String SECURITY_AUTHENTICATE_USER_NOT_FOUND = "/security/model/security-authenticate-not-found-user.json";
    private static final String SECURITY_REGISTRATION_WITHOUT_NAME = "/security/model/security-registration-without-name.json";
    private static final String SECURITY_REGISTRATION_LENGTH_NAME_LESS_TWO_WORD = "/security/model/security-registration-length-name-less-two-word.json";
    private static final String SECURITY_REGISTRATION_LENGTH_NAME_MORE_128_WORD = "/security/model/security-registration-length-name-more-128-word.json";
    private static final String SECURITY_REGISTRATION_WITHOUT_LAST_NAME = "/security/model/security-registration-without-last-name.json";
    private static final String SECURITY_REGISTRATION_LENGTH_LAST_NAME_LESS_TWO_WORD = "/security/model/security-registration-length-last-name-less-two-word.json";
    private static final String SECURITY_REGISTRATION_LENGTH_LAST_NAME_MORE_128_WORD = "/security/model/security-registration-length-last-name-more-128-word.json";
    private static final String SECURITY_REGISTRATION_EMPTY_EMAIL = "/security/model/security-registration-empty-email.json";
    private static final String SECURITY_REGISTRATION_LENGTH_EMAIL_LESS_EIGHT_WORD = "/security/model/security-registration-length-email-less-eight-word.json";
    private static final String SECURITY_REGISTRATION_LENGTH_EMAIL_MORE_128_WORD = "/security/model/security-registration-length-email-more-128-word.json";
    private static final String SECURITY_REGISTRATION_EMPTY_PASSWORD = "/security/model/security-registration-empty-password.json";
    private static final String SECURITY_REGISTRATION_LENGTH_PASSWORD_LESS_EIGHT_CHARACTERS = "/security/model/security-registration-length-password-less-eight-characters.json";
    private static final String SECURITY_REGISTRATION_LENGTH_PASSWORD_MORE_128_CHARACTERS = "/security/model/security-registration-length-password-more-128-characters.json";
    private static final String SECURITY_REGISTRATION_PASSWORD_WITHOUT_WORD = "/security/model/security-registration-password-without-word.json";
    private static final String AUTH_BASE_PATH = "/api/v1/auth";

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
    @DisplayName("Should registration new user")
    void shouldRegistrationNewUser() {
        given(specification).body(getRequestBody(SECURITY_REGISTRATION)).post("/register")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body(containsString("Email verification token sent"));
    }

    @Test
    @DisplayName("Should fail registration without name")
    void shouldFailRegistrationWithoutName() {
        given(specification).body(getRequestBody(SECURITY_REGISTRATION_WITHOUT_NAME)).post("/register")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("message", notNullValue())
                .body("httpStatusCode", equalTo(400));
    }

    @Test
    @DisplayName("Should fail registration with short name")
    void shouldFailRegistrationWithShortName() {
        given(specification).body(getRequestBody(SECURITY_REGISTRATION_LENGTH_NAME_LESS_TWO_WORD)).post("/register")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("message", notNullValue());
    }

    @Test
    @DisplayName("Should registration new user Failed length name more one hundred twenty-eight word")
    void shouldRegistrationNewUserFailedLengthNameMoreOneHundredTwentyEightWord() {
        assertRestApiBadRequestResponse(
                given(specification).body(getRequestBody(SECURITY_REGISTRATION_LENGTH_NAME_MORE_128_WORD)).post("/register"),
                SECURITY_SCHEMA_FAILED);
    }

    @Test
    @DisplayName("Should registration new user Failed length last name less two word")
    void shouldRegistrationNewUserFailedLengthLastNameLessTwoWord() {
        assertRestApiBadRequestResponse(
                given(specification).body(getRequestBody(SECURITY_REGISTRATION_LENGTH_LAST_NAME_LESS_TWO_WORD)).post("/register"),
                SECURITY_SCHEMA_FAILED);
    }

    @Test
    @DisplayName("Should registration new user Failed length last name more one hundred twenty-eight word")
    void shouldRegistrationNewUserFailedLengthLastNameMoreOneHundredTwentyEightWord() {
        assertRestApiBadRequestResponse(
                given(specification).body(getRequestBody(SECURITY_REGISTRATION_LENGTH_LAST_NAME_MORE_128_WORD)).post("/register"),
                SECURITY_SCHEMA_FAILED);
    }

    @Test
    @DisplayName("Should registration new user Failed without last name")
    void shouldRegistrationNewUserFailedWithoutLastName() {
        assertRestApiBadRequestResponse(
                given(specification).body(getRequestBody(SECURITY_REGISTRATION_WITHOUT_LAST_NAME)).post("/register"),
                SECURITY_SCHEMA_FAILED);
    }

    @Test
    @DisplayName("Should registration new user Failed Email empty")
    void shouldRegistrationNewUserFailedEmailEmpty() {
        assertRestApiBadRequestResponse(
                given(specification).body(getRequestBody(SECURITY_REGISTRATION_EMPTY_EMAIL)).post("/register"),
                SECURITY_SCHEMA_FAILED);
    }

    @Test
    @DisplayName("Should registration new user Failed email not unique")
    void shouldRegistrationNewUserFailedEmailNotUnique() {
        String uniqueEmail = "duplicate." + System.currentTimeMillis() + "@gmail.com";
        String body = "{\"firstName\":\"Jon\",\"lastName\":\"Smith\",\"email\":\"" + uniqueEmail + "\",\"password\":\"!h2h3kKl\"}";

        given(specification).body(body).post("/register").then().statusCode(HttpStatus.OK.value());
        given(specification).body(body).post("/register").then().statusCode(425).body("message", notNullValue());
    }

    @Test
    @DisplayName("Should registration new user Failed empty password")
    void shouldRegistrationNewUserFailedPasswordEmpty() {
        assertRestApiBadRequestResponse(
                given(specification).body(getRequestBody(SECURITY_REGISTRATION_EMPTY_PASSWORD)).post("/register"),
                SECURITY_SCHEMA_FAILED);
    }

    @Test
    @DisplayName("Should registration new user Failed length email less eight word")
    void shouldRegistrationNewUserFailedLengthEmailLessEightWord() {
        assertRestApiBadRequestResponse(
                given(specification).body(getRequestBody(SECURITY_REGISTRATION_LENGTH_EMAIL_LESS_EIGHT_WORD)).post("/register"),
                SECURITY_SCHEMA_FAILED);
    }

    @Test
    @DisplayName("Should registration new user Failed length email more one hundred-eight word")
    void shouldRegistrationNewUserFailedLengthEmailMoreOneHundredEightWord() {
        assertRestApiBadRequestResponse(
                given(specification).body(getRequestBody(SECURITY_REGISTRATION_LENGTH_EMAIL_MORE_128_WORD)).post("/register"),
                SECURITY_SCHEMA_FAILED);
    }

    @Test
    @DisplayName("Should registration new user Failed length password less eight characters")
    void shouldRegistrationNewUserFailedLengthPasswordLessEightCharacters() {
        assertRestApiBadRequestResponse(
                given(specification).body(getRequestBody(SECURITY_REGISTRATION_LENGTH_PASSWORD_LESS_EIGHT_CHARACTERS)).post("/register"),
                SECURITY_SCHEMA_FAILED);
    }

    @Test
    @DisplayName("Should registration new user Failed length password more one hundred characters")
    void shouldRegistrationNewUserFailedLengthPasswordMoreOneHundredEightCharacters() {
        assertRestApiBadRequestResponse(
                given(specification).body(getRequestBody(SECURITY_REGISTRATION_LENGTH_PASSWORD_MORE_128_CHARACTERS)).post("/register"),
                SECURITY_SCHEMA_FAILED);
    }

    @Test
    @DisplayName("Should registration new user Failed password without word")
    void shouldRegistrationNewUserFailedPasswordWithoutWord() {
        assertRestApiBadRequestResponse(
                given(specification).body(getRequestBody(SECURITY_REGISTRATION_PASSWORD_WITHOUT_WORD)).post("/register"),
                SECURITY_SCHEMA_FAILED);
    }

    @Test
    @DisplayName("Should authenticate user")
    void shouldAuthenticateUser() {
        UserRegistrationRequest pending = new UserRegistrationRequest("Auth", "Registr", "AuthReg@gmail.com", "!h2h3kKl22");
        String token = tokenManager.generateToken(pending);

        given(specification).body("{\"token\":\"" + token + "\"}").post("/confirm")
                .then().statusCode(HttpStatus.CREATED.value());

        assertRestApiOkResponse(given(specification).body(getRequestBody(SECURITY_AUTHENTICATE)).post("/authenticate"), SECURITY_SCHEMA);
    }

    @Test
    @DisplayName("Should fail authentication for non-existent user")
    void shouldFailAuthenticationForNonExistentUser() {
        given(specification).body(getRequestBody(SECURITY_AUTHENTICATE_USER_NOT_FOUND)).post("/authenticate")
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value())
                .body("message", notNullValue());
    }

    @Test
    @DisplayName("Should fail authentication with incorrect password")
    void shouldFailAuthenticationWithIncorrectPassword() {
        given(specification).body("{\"email\":\"olivia@example.com\",\"password\":\"wrongpassword1\"}").post("/authenticate")
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value())
                .body("message", notNullValue());
    }
}
