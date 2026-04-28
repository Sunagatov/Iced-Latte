package com.zufar.icedlatte.auth.endpoint;

import com.zufar.icedlatte.auth.api.GoogleAuthCallbackHandler;
import com.zufar.icedlatte.openapi.dto.UserAuthenticationResponse;
import com.zufar.icedlatte.test.config.IntegrationTestBase;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.util.UriComponentsBuilder;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("AuthEndpoint integration tests")
class AuthEndpointIntegrationTest extends IntegrationTestBase {

    private static final String BASE_PATH = "/api/v1/auth";

    @LocalServerPort
    private Integer port;

    @Value("${frontend.url}")
    private String frontendUrl;

    @MockitoBean
    private GoogleAuthCallbackHandler googleAuthCallbackHandler;

    private RequestSpecification specification;

    @BeforeEach
    void setUp() {
        specification = given()
                .port(port)
                .basePath(BASE_PATH);
    }

    @Test
    @DisplayName("Should initiate Google auth and redirect back with token pair using Redis-backed state")
    void shouldInitiateGoogleAuthAndRedirectBackWithTokenPairUsingRedisBackedState() throws Exception {
        String callbackBase = frontendUrl + "/oauth/callback";

        when(googleAuthCallbackHandler.handle(eq("valid-code"), any(HttpServletRequest.class)))
                .thenReturn(tokenPair("jwt-token", "refresh-token"));

        Response initiateResponse = given(specification)
                .redirects().follow(false)
                .queryParam("redirectUrl", callbackBase)
                .get("/google");

        initiateResponse.then()
                .statusCode(HttpStatus.FOUND.value())
                .header("Location", allOf(
                        containsString("state="),
                        containsString("client_id="),
                        containsString("redirect_uri=")
                ));

        String state = extractState(initiateResponse);
        assertNotNull(state);

        given(specification)
                .redirects().follow(false)
                .queryParam("code", "valid-code")
                .queryParam("state", state)
                .get("/google/callback")
                .then()
                .statusCode(HttpStatus.FOUND.value())
                .header("Location", allOf(
                        containsString(callbackBase),
                        containsString("#token=jwt-token"),
                        containsString("refreshToken=refresh-token")
                ));
    }

    @Test
    @DisplayName("Should reject reused OAuth state after first successful callback")
    void shouldRejectReusedOAuthStateAfterFirstSuccessfulCallback() throws Exception {
        String callbackBase = frontendUrl + "/oauth/callback";

        when(googleAuthCallbackHandler.handle(any(String.class), any(HttpServletRequest.class)))
                .thenReturn(tokenPair("jwt-once", "refresh-once"));

        Response initiateResponse = given(specification)
                .redirects().follow(false)
                .queryParam("redirectUrl", callbackBase)
                .get("/google");

        String state = extractState(initiateResponse);
        assertNotNull(state);

        given(specification)
                .redirects().follow(false)
                .queryParam("code", "first-code")
                .queryParam("state", state)
                .get("/google/callback")
                .then()
                .statusCode(HttpStatus.FOUND.value());

        given(specification)
                .redirects().follow(false)
                .queryParam("code", "second-code")
                .queryParam("state", state)
                .get("/google/callback")
                .then()
                .statusCode(HttpStatus.FOUND.value())
                .header("Location", equalTo(frontendUrl + "/signin?error=invalid_state"));

        verify(googleAuthCallbackHandler, times(1))
                .handle(any(String.class), any(HttpServletRequest.class));
    }

    @Test
    @DisplayName("Should fallback to configured frontend URL when redirectUrl origin is not allowed")
    void shouldFallbackToConfiguredFrontendUrlWhenRedirectUrlOriginIsNotAllowed() throws Exception {
        when(googleAuthCallbackHandler.handle(eq("safe-code"), any(HttpServletRequest.class)))
                .thenReturn(tokenPair("safe-jwt", "safe-refresh"));

        Response initiateResponse = given(specification)
                .redirects().follow(false)
                .queryParam("redirectUrl", "https://evil.example.com/callback")
                .get("/google");

        String state = extractState(initiateResponse);
        assertNotNull(state);

        given(specification)
                .redirects().follow(false)
                .queryParam("code", "safe-code")
                .queryParam("state", state)
                .get("/google/callback")
                .then()
                .statusCode(HttpStatus.FOUND.value())
                .header("Location", allOf(
                        containsString(frontendUrl),
                        containsString("#token=safe-jwt"),
                        containsString("refreshToken=safe-refresh"),
                        not(containsString("evil.example.com"))
                ));
    }

    @Test
    @DisplayName("Should redirect with missing_code error when callback code is absent")
    void shouldRedirectWithMissingCodeErrorWhenCallbackCodeIsAbsent() throws Exception {
        given(specification)
                .redirects().follow(false)
                .queryParam("state", "any-state")
                .get("/google/callback")
                .then()
                .statusCode(HttpStatus.FOUND.value())
                .header("Location", equalTo(frontendUrl + "/signin?error=missing_code"));

        verify(googleAuthCallbackHandler, never()).handle(any(String.class), any(HttpServletRequest.class));
    }

    @Test
    @DisplayName("Should redirect with invalid_state when callback state is absent")
    void shouldRedirectWithInvalidStateWhenCallbackStateIsAbsent() throws Exception {
        given(specification)
                .redirects().follow(false)
                .queryParam("code", "valid-code")
                .get("/google/callback")
                .then()
                .statusCode(HttpStatus.FOUND.value())
                .header("Location", equalTo(frontendUrl + "/signin?error=invalid_state"));

        verify(googleAuthCallbackHandler, never()).handle(any(String.class), any(HttpServletRequest.class));
    }

    @Test
    @DisplayName("Should redirect back to stored callback with auth_failed when handler throws")
    void shouldRedirectBackToStoredCallbackWhenHandlerThrows() throws Exception {
        String callbackBase = frontendUrl + "/oauth/callback";

        when(googleAuthCallbackHandler.handle(eq("broken-code"), any(HttpServletRequest.class)))
                .thenThrow(new IllegalStateException("exchange failed"));

        Response initiateResponse = given(specification)
                .redirects().follow(false)
                .queryParam("redirectUrl", callbackBase)
                .get("/google");

        String state = extractState(initiateResponse);
        assertNotNull(state);

        given(specification)
                .redirects().follow(false)
                .queryParam("code", "broken-code")
                .queryParam("state", state)
                .get("/google/callback")
                .then()
                .statusCode(HttpStatus.FOUND.value())
                .header("Location", equalTo(callbackBase + "/signin?error=auth_failed"));
    }

    @Test
    @DisplayName("Should return 405 for GET refresh requests")
    void shouldReturn405ForGetRefreshRequests() {
        given(specification)
                .get("/refresh")
                .then()
                .statusCode(HttpStatus.METHOD_NOT_ALLOWED.value());
    }

    private String extractState(Response response) {
        String location = response.getHeader("Location");
        return UriComponentsBuilder.fromUriString(location)
                .build()
                .getQueryParams()
                .getFirst("state");
    }

    private UserAuthenticationResponse tokenPair(String token, String refreshToken) {
        UserAuthenticationResponse response = new UserAuthenticationResponse();
        response.setToken(token);
        response.setRefreshToken(refreshToken);
        return response;
    }
}
