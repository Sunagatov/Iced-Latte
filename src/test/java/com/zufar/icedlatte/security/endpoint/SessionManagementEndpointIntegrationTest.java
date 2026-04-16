package com.zufar.icedlatte.security.endpoint;

import com.zufar.icedlatte.test.config.AuthenticatedUserIntegrationSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Session management integration tests")
class SessionManagementEndpointIntegrationTest extends AuthenticatedUserIntegrationSupport {

    private static final String AUTH_BASE_PATH = "/api/v1/auth";
    private static final String SECOND_SESSION_USER_AGENT = "integration-test-second-session";

    @Test
    @DisplayName("Should list active sessions and revoke a specific session by id")
    void shouldListActiveSessionsAndRevokeSpecificSessionById() {
        AuthenticatedUser user = registerAndAuthenticateUser();
        authenticateWithUserAgent(user.email(), user.password(), SECOND_SESSION_USER_AGENT);

        List<?> sessions = given(authenticatedJsonSpec(AUTH_BASE_PATH, user.accessToken()))
                .get("/sessions")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("", hasSize(2))
                .extract()
                .jsonPath()
                .getList("", Map.class);

        String secondSessionId = sessions.stream()
                .map(Map.class::cast)
                .filter(session -> SECOND_SESSION_USER_AGENT.equals(String.valueOf(session.get("userAgent"))))
                .map(session -> String.valueOf(session.get("sessionId")))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Second session was not found in /sessions response"));

        given(authenticatedJsonSpec(AUTH_BASE_PATH, user.accessToken()))
                .delete("/sessions/{sessionId}", secondSessionId)
                .then()
                .statusCode(HttpStatus.NO_CONTENT.value());

        List<?> remainingSessions = given(authenticatedJsonSpec(AUTH_BASE_PATH, user.accessToken()))
                .get("/sessions")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("", hasSize(1))
                .extract()
                .jsonPath()
                .getList("", Map.class);

        assertEquals(1, remainingSessions.size());
        assertTrue(remainingSessions.stream()
                .map(Map.class::cast)
                .noneMatch(session -> SECOND_SESSION_USER_AGENT.equals(String.valueOf(session.get("userAgent")))));
    }

    @Test
    @DisplayName("Should return forbidden when revoking another user's session")
    void shouldReturnForbiddenWhenRevokingAnotherUsersSession() {
        AuthenticatedUser firstUser = registerAndAuthenticateUser();
        AuthenticatedUser secondUser = registerAndAuthenticateUser();

        List<?> firstUserSessions = given(authenticatedJsonSpec(AUTH_BASE_PATH, firstUser.accessToken()))
                .get("/sessions")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("", hasSize(1))
                .extract()
                .jsonPath()
                .getList("", Map.class);

        String firstUserSessionId = firstUserSessions.stream()
                .map(Map.class::cast)
                .map(session -> String.valueOf(session.get("sessionId")))
                .findFirst()
                .orElseThrow(() -> new AssertionError("First user's session id was not found"));

        given(authenticatedJsonSpec(AUTH_BASE_PATH, secondUser.accessToken()))
                .delete("/sessions/{sessionId}", firstUserSessionId)
                .then()
                .statusCode(HttpStatus.FORBIDDEN.value());
    }

    @Test
    @DisplayName("Should return not found when revoking unknown session")
    void shouldReturnNotFoundWhenRevokingUnknownSession() {
        AuthenticatedUser user = registerAndAuthenticateUser();

        given(authenticatedJsonSpec(AUTH_BASE_PATH, user.accessToken()))
                .delete("/sessions/{sessionId}", UUID.randomUUID())
                .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
    }

    private AuthenticatedUser authenticateWithUserAgent(String email, String password, String userAgent) {
        var response = given(jsonSpec(AUTH_BASE_PATH)
                .header("User-Agent", userAgent))
                .body("""
                        {
                          "email": "%s",
                          "password": "%s"
                        }
                        """.formatted(email, password))
                .post("/authenticate")
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .jsonPath();

        return new AuthenticatedUser(
                response.getString("token"),
                response.getString("refreshToken"),
                email,
                password
        );
    }
}