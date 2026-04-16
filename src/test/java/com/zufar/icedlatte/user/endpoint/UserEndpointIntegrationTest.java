package com.zufar.icedlatte.user.endpoint;

import com.zufar.icedlatte.test.config.AuthenticatedUserIntegrationSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@DisplayName("UserEndpoint integration tests")
class UserEndpointIntegrationTest extends AuthenticatedUserIntegrationSupport {

    private static final String BASE_PATH = "/api/v1/users";
    private static final String AUTH_BASE_PATH = "/api/v1/auth";

    @Test
    @DisplayName("Should return authenticated user profile")
    void shouldReturnAuthenticatedUserProfile() {
        AuthenticatedUser user = registerAndAuthenticateUser();

        given(authenticatedJsonSpec(BASE_PATH, user.accessToken()))
                .get()
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("id", notNullValue())
                .body("email", equalTo(user.email()))
                .body("firstName", equalTo("Integration"))
                .body("lastName", equalTo("User"));
    }

    @Test
    @DisplayName("Should update user profile and persist updated fields")
    void shouldUpdateUserProfileAndPersistUpdatedFields() {
        AuthenticatedUser user = registerAndAuthenticateUser();

        String updateBody = """
                {
                  "firstName": "Updated",
                  "lastName": "Customer",
                  "birthDate": "1994-04-16",
                  "phoneNumber": "+447400000001",
                  "address": {
                    "country": "United Kingdom",
                    "city": "London",
                    "line": "221B Baker Street",
                    "postcode": "NW1 6XE"
                  }
                }
                """;

        given(authenticatedJsonSpec(BASE_PATH, user.accessToken()))
                .body(updateBody)
                .put()
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("email", equalTo(user.email()))
                .body("firstName", equalTo("Updated"))
                .body("lastName", equalTo("Customer"))
                .body("birthDate", equalTo("1994-04-16"))
                .body("phoneNumber", equalTo("+447400000001"))
                .body("address.country", equalTo("United Kingdom"))
                .body("address.city", equalTo("London"))
                .body("address.line", equalTo("221B Baker Street"))
                .body("address.postcode", equalTo("NW1 6XE"));

        given(authenticatedJsonSpec(BASE_PATH, user.accessToken()))
                .get()
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("firstName", equalTo("Updated"))
                .body("lastName", equalTo("Customer"))
                .body("birthDate", equalTo("1994-04-16"))
                .body("phoneNumber", equalTo("+447400000001"))
                .body("address.country", equalTo("United Kingdom"))
                .body("address.city", equalTo("London"))
                .body("address.line", equalTo("221B Baker Street"))
                .body("address.postcode", equalTo("NW1 6XE"));
    }

    @Test
    @DisplayName("Should change password and authenticate only with the new password")
    void shouldChangePasswordAndAuthenticateOnlyWithTheNewPassword() {
        AuthenticatedUser user = registerAndAuthenticateUser();
        String newPassword = "NewPass123!";

        String changePasswordBody = """
                {
                  "oldPassword": "%s",
                  "newPassword": "%s"
                }
                """.formatted(user.password(), newPassword);

        given(authenticatedJsonSpec(BASE_PATH, user.accessToken()))
                .body(changePasswordBody)
                .patch()
                .then()
                .statusCode(HttpStatus.OK.value());

        given(jsonSpec(AUTH_BASE_PATH))
                .body("""
                        {
                          "email": "%s",
                          "password": "%s"
                        }
                        """.formatted(user.email(), user.password()))
                .post("/authenticate")
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value());

        given(jsonSpec(AUTH_BASE_PATH))
                .body("""
                        {
                          "email": "%s",
                          "password": "%s"
                        }
                        """.formatted(user.email(), newPassword))
                .post("/authenticate")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("token", notNullValue())
                .body("refreshToken", notNullValue());
    }
}