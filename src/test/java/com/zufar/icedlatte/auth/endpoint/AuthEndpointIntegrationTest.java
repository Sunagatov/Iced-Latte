package com.zufar.icedlatte.auth.endpoint;

import com.zufar.icedlatte.auth.api.OAuthLoginService;
import com.zufar.icedlatte.auth.api.OAuthProvider;
import com.zufar.icedlatte.auth.api.OAuthProviderClient;
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

import java.net.URI;
import java.util.Optional;

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
    private OAuthLoginService oAuthLoginService;

    @MockitoBean
    private OAuthProviderClient oAuthProviderClient;

    private RequestSpecification specification;

    @BeforeEach
    void setUp() {
        specification = given()
                .port(port)
                .basePath(BASE_PATH);
        when(oAuthLoginService.findClient(OAuthProvider.GOOGLE)).thenReturn(Optional.of(oAuthProviderClient));
        when(oAuthProviderClient.buildAuthorizationUri(any(String.class)))
                .thenAnswer(invocation -> googleAuthUri(invocation.getArgument(0)));
    }

    @Test
    @DisplayName("Should initiate Google auth and redirect back with token pair using Redis-backed state")
    void shouldInitiateGoogleAuthAndRedirectBackWithTokenPairUsingRedisBackedState() {
        String callbackBase = frontendUrl + "/auth/google/callback";

        when(oAuthLoginService.handle(eq(OAuthProvider.GOOGLE), eq("valid-code"), any(HttpServletRequest.class)))
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
    @DisplayName("Should support provider-neutral Google OAuth routes")
    void shouldSupportProviderNeutralGoogleOAuthRoutes() {
        String callbackBase = frontendUrl + "/auth/google/callback";

        when(oAuthLoginService.handle(eq(OAuthProvider.GOOGLE), eq("valid-code"), any(HttpServletRequest.class)))
                .thenReturn(tokenPair("jwt-token", "refresh-token"));

        Response initiateResponse = given(specification)
                .redirects().follow(false)
                .queryParam("redirectUrl", callbackBase)
                .get("/oauth/google");

        String state = extractState(initiateResponse);
        assertNotNull(state);

        given(specification)
                .redirects().follow(false)
                .queryParam("code", "valid-code")
                .queryParam("state", state)
                .get("/oauth/google/callback")
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
    void shouldRejectReusedOAuthStateAfterFirstSuccessfulCallback() {
        String callbackBase = frontendUrl + "/auth/google/callback";

        when(oAuthLoginService.handle(eq(OAuthProvider.GOOGLE), any(String.class), any(HttpServletRequest.class)))
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

        verify(oAuthLoginService, times(1))
                .handle(eq(OAuthProvider.GOOGLE), any(String.class), any(HttpServletRequest.class));
    }

    @Test
    @DisplayName("Should fallback to configured frontend URL when redirectUrl origin is not allowed")
    void shouldFallbackToConfiguredFrontendUrlWhenRedirectUrlOriginIsNotAllowed() {
        when(oAuthLoginService.handle(eq(OAuthProvider.GOOGLE), eq("safe-code"), any(HttpServletRequest.class)))
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
                        containsString(frontendUrl + "/auth/google/callback"),
                        containsString("#token=safe-jwt"),
                        containsString("refreshToken=safe-refresh"),
                        not(containsString("evil.example.com"))
                ));
    }

    @Test
    @DisplayName("Should fallback to configured callback path when redirectUrl uses an unexpected frontend path")
    void shouldFallbackToConfiguredCallbackPathWhenRedirectUrlUsesUnexpectedFrontendPath() {
        when(oAuthLoginService.handle(eq(OAuthProvider.GOOGLE), eq("safe-code"), any(HttpServletRequest.class)))
                .thenReturn(tokenPair("safe-jwt", "safe-refresh"));

        Response initiateResponse = given(specification)
                .redirects().follow(false)
                .queryParam("redirectUrl", frontendUrl + "/profile")
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
                        containsString(frontendUrl + "/auth/google/callback"),
                        containsString("#token=safe-jwt"),
                        not(containsString("/profile"))
                ));
    }

    @Test
    @DisplayName("Should redirect with missing_code error when callback code is absent")
    void shouldRedirectWithMissingCodeErrorWhenCallbackCodeIsAbsent() {
        given(specification)
                .redirects().follow(false)
                .queryParam("state", "any-state")
                .get("/google/callback")
                .then()
                .statusCode(HttpStatus.FOUND.value())
                .header("Location", equalTo(frontendUrl + "/signin?error=missing_code"));

        verify(oAuthLoginService, never()).handle(any(OAuthProvider.class), any(String.class), any(HttpServletRequest.class));
    }

    @Test
    @DisplayName("Should redirect with invalid_state when callback state is absent")
    void shouldRedirectWithInvalidStateWhenCallbackStateIsAbsent() {
        given(specification)
                .redirects().follow(false)
                .queryParam("code", "valid-code")
                .get("/google/callback")
                .then()
                .statusCode(HttpStatus.FOUND.value())
                .header("Location", equalTo(frontendUrl + "/signin?error=invalid_state"));

        verify(oAuthLoginService, never()).handle(any(OAuthProvider.class), any(String.class), any(HttpServletRequest.class));
    }

    @Test
    @DisplayName("Should redirect back to stored callback with auth_failed when handler throws")
    void shouldRedirectBackToStoredCallbackWhenHandlerThrows() {
        String callbackBase = frontendUrl + "/auth/google/callback?next=/checkout";

        when(oAuthLoginService.handle(eq(OAuthProvider.GOOGLE), eq("broken-code"), any(HttpServletRequest.class)))
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
                .header("Location", equalTo(frontendUrl + "/signin?error=auth_failed&next=/checkout"));
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

    private URI googleAuthUri(String state) {
        return UriComponentsBuilder.fromUriString("https://accounts.google.com/o/oauth2/v2/auth")
                .queryParam("state", state)
                .queryParam("client_id", "client-id")
                .queryParam("redirect_uri", "http://localhost:" + port + BASE_PATH + "/google/callback")
                .build()
                .toUri();
    }

    private UserAuthenticationResponse tokenPair(String token, String refreshToken) {
        UserAuthenticationResponse response = new UserAuthenticationResponse();
        response.setToken(token);
        response.setRefreshToken(refreshToken);
        return response;
    }
}
