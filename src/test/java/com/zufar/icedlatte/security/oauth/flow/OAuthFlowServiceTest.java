package com.zufar.icedlatte.security.oauth.flow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.net.URI;
import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.zufar.icedlatte.common.exception.UnauthorizedException;
import com.zufar.icedlatte.security.oauth.config.OAuthProvider;
import com.zufar.icedlatte.security.oauth.login.OAuthLoginService;
import com.zufar.icedlatte.security.oauth.login.OAuthProviderClient;
import com.zufar.icedlatte.security.session.token.AuthenticationTokens;

@ExtendWith(MockitoExtension.class)
class OAuthFlowServiceTest {

    @Mock
    private OAuthLoginService oAuthLoginService;

    @Mock
    private OAuthStateStore oAuthStateStore;

    @Mock
    private OAuthTokenHandoffStore oAuthTokenHandoffStore;

    @Mock
    private OAuthProviderClient oAuthProviderClient;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private OAuthStateCookieService oAuthStateCookieService;

    private OAuthFlowService service;

    @BeforeEach
    void setUp() {
        service = new OAuthFlowService(
                oAuthLoginService,
                oAuthStateStore,
                oAuthTokenHandoffStore,
                new OAuthRedirectService("https://app.example.com"),
                oAuthStateCookieService);
    }

    @Test
    void initiateStoresAllowedCallbackAndReturnsProviderAuthorizationUri() {
        stubGoogleClient();
        when(oAuthProviderClient.buildAuthorizationUri(anyString()))
                .thenReturn(URI.create("https://accounts.google.com/o/oauth2/v2/auth"));

        Optional<URI> result = service.initiate(
                OAuthProvider.GOOGLE,
                "https://app.example.com:443/auth/google/callback?next=/checkout",
                request,
                response);

        assertThat(result).contains(URI.create("https://accounts.google.com/o/oauth2/v2/auth"));
        ArgumentCaptor<String> callbackBase = ArgumentCaptor.forClass(String.class);
        verify(oAuthStateStore).store(eq(OAuthProvider.GOOGLE), anyString(), callbackBase.capture());
        verify(oAuthStateCookieService).bind(eq(request), eq(response), eq(OAuthProvider.GOOGLE), anyString(), any());
        assertThat(callbackBase.getValue())
                .isEqualTo("https://app.example.com:443/auth/google/callback?next=/checkout");
    }

    @Test
    void initiateFallsBackToDefaultCallbackWhenRedirectOriginIsNotAllowed() {
        stubGoogleClient();
        when(oAuthProviderClient.buildAuthorizationUri(anyString()))
                .thenReturn(URI.create("https://accounts.google.com/o/oauth2/v2/auth"));

        service.initiate(OAuthProvider.GOOGLE, "https://evil.example.com/auth/google/callback", request, response);

        ArgumentCaptor<String> callbackBase = ArgumentCaptor.forClass(String.class);
        verify(oAuthStateStore).store(eq(OAuthProvider.GOOGLE), anyString(), callbackBase.capture());
        assertThat(callbackBase.getValue()).isEqualTo("https://app.example.com/auth/google/callback");
    }

    @Test
    void initiateDropsUnsafeNextFromAllowedCallback() {
        stubGoogleClient();
        when(oAuthProviderClient.buildAuthorizationUri(anyString()))
                .thenReturn(URI.create("https://accounts.google.com/o/oauth2/v2/auth"));

        service.initiate(
                OAuthProvider.GOOGLE,
                "https://app.example.com/auth/google/callback?next=https://evil.example.com",
                request,
                response);

        ArgumentCaptor<String> callbackBase = ArgumentCaptor.forClass(String.class);
        verify(oAuthStateStore).store(eq(OAuthProvider.GOOGLE), anyString(), callbackBase.capture());
        assertThat(callbackBase.getValue()).isEqualTo("https://app.example.com/auth/google/callback");
    }

    @Test
    void initiateDropsEncodedBackslashNextFromAllowedCallback() {
        stubGoogleClient();
        when(oAuthProviderClient.buildAuthorizationUri(anyString()))
                .thenReturn(URI.create("https://accounts.google.com/o/oauth2/v2/auth"));

        service.initiate(
                OAuthProvider.GOOGLE,
                "https://app.example.com/auth/google/callback?next=/%5Cevil.example.com",
                request,
                response);

        ArgumentCaptor<String> callbackBase = ArgumentCaptor.forClass(String.class);
        verify(oAuthStateStore).store(eq(OAuthProvider.GOOGLE), anyString(), callbackBase.capture());
        assertThat(callbackBase.getValue()).isEqualTo("https://app.example.com/auth/google/callback");
    }

    @Test
    void initiateReturnsEmptyWhenProviderClientIsNotRegistered() {
        when(oAuthLoginService.findClient(OAuthProvider.GOOGLE)).thenReturn(Optional.empty());

        Optional<URI> result = service.initiate(OAuthProvider.GOOGLE, null, request, response);

        assertThat(result).isEmpty();
        verifyNoInteractions(oAuthStateStore, oAuthProviderClient);
    }

    @Test
    void redirectServiceRejectsInvalidFrontendUrlConfiguration() {
        assertThatThrownBy(() -> new OAuthRedirectService("app.example.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("frontend.url");
    }

    @Test
    void redirectServiceRejectsNonHttpFrontendUrlConfiguration() {
        assertThatThrownBy(() -> new OAuthRedirectService("ftp://app.example.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("http or https");
    }

    @Test
    void completeCallbackReturnsCallbackWithOneTimeHandoffCode() {
        stubGoogleClient();
        when(oAuthStateCookieService.matches(request, OAuthProvider.GOOGLE, "state-token"))
                .thenReturn(true);
        when(oAuthStateStore.consume(OAuthProvider.GOOGLE, "state-token"))
                .thenReturn(Optional.of("https://app.example.com/auth/google/callback?next=/checkout"));
        when(oAuthLoginService.handle(OAuthProvider.GOOGLE, "valid-code", request))
                .thenReturn(tokenPair());
        when(oAuthTokenHandoffStore.store(any(AuthenticationTokens.class))).thenReturn("handoff-code");

        URI redirect = service.completeCallback(OAuthProvider.GOOGLE, "valid-code", "state-token", request, response);

        assertThat(redirect.toString())
                .isEqualTo("https://app.example.com/auth/google/callback?next=/checkout#oauthCode=handoff-code");
        assertThat(redirect.toString()).doesNotContain("jwt-token", "refresh-token");
        verify(oAuthTokenHandoffStore).store(any(AuthenticationTokens.class));
        verify(oAuthStateCookieService).clear(request, response, OAuthProvider.GOOGLE);
    }

    @Test
    void completeCallbackReplacesExistingFragmentWithOneTimeHandoffCode() {
        stubGoogleClient();
        when(oAuthStateCookieService.matches(request, OAuthProvider.GOOGLE, "state-token"))
                .thenReturn(true);
        when(oAuthStateStore.consume(OAuthProvider.GOOGLE, "state-token"))
                .thenReturn(Optional.of("https://app.example.com/auth/google/callback#old"));
        when(oAuthLoginService.handle(OAuthProvider.GOOGLE, "valid-code", request))
                .thenReturn(tokenPair());
        when(oAuthTokenHandoffStore.store(any(AuthenticationTokens.class))).thenReturn("handoff-code");

        URI redirect = service.completeCallback(OAuthProvider.GOOGLE, "valid-code", "state-token", request, response);

        assertThat(redirect.toString())
                .isEqualTo("https://app.example.com/auth/google/callback#oauthCode=handoff-code");
    }

    @Test
    void completeTokenHandoffConsumesStoredTokenPair() {
        AuthenticationTokens tokenPair = tokenPair();
        when(oAuthTokenHandoffStore.consume("handoff-code")).thenReturn(Optional.of(tokenPair));

        AuthenticationTokens result = service.completeTokenHandoff("handoff-code");

        assertThat(result).isSameAs(tokenPair);
    }

    @Test
    void completeCallbackReturnsInvalidStateRedirectWhenStateWasNotStored() {
        stubGoogleClient();
        when(oAuthStateCookieService.matches(request, OAuthProvider.GOOGLE, "missing"))
                .thenReturn(true);
        when(oAuthStateStore.consume(OAuthProvider.GOOGLE, "missing")).thenReturn(Optional.empty());

        URI redirect = service.completeCallback(OAuthProvider.GOOGLE, "valid-code", "missing", request, response);

        assertThat(redirect.toString()).isEqualTo("https://app.example.com/signin?error=invalid_state");
        verify(oAuthStateCookieService).clear(request, response, OAuthProvider.GOOGLE);
    }

    @Test
    void completeCallbackRejectsStateWhenStateCookieDoesNotMatch() {
        stubGoogleClient();
        when(oAuthStateCookieService.matches(request, OAuthProvider.GOOGLE, "state-token"))
                .thenReturn(false);

        URI redirect = service.completeCallback(OAuthProvider.GOOGLE, "valid-code", "state-token", request, response);

        assertThat(redirect.toString()).isEqualTo("https://app.example.com/signin?error=invalid_state");
        verifyNoInteractions(oAuthStateStore, oAuthTokenHandoffStore);
        verify(oAuthStateCookieService).clear(request, response, OAuthProvider.GOOGLE);
        verify(oAuthLoginService, never()).handle(any(), anyString(), any());
    }

    @Test
    void completeCallbackReturnsAuthFailedRedirectAndPreservesNextWhenLoginFails() {
        stubGoogleClient();
        when(oAuthStateCookieService.matches(request, OAuthProvider.GOOGLE, "state-token"))
                .thenReturn(true);
        when(oAuthStateStore.consume(OAuthProvider.GOOGLE, "state-token"))
                .thenReturn(Optional.of("https://app.example.com/auth/google/callback?next=/checkout"));
        when(oAuthLoginService.handle(OAuthProvider.GOOGLE, "broken-code", request))
                .thenThrow(new UnauthorizedException("exchange failed"));

        URI redirect = service.completeCallback(OAuthProvider.GOOGLE, "broken-code", "state-token", request, response);

        assertThat(redirect.toString()).isEqualTo("https://app.example.com/signin?error=auth_failed&next=/checkout");
    }

    @Test
    void completeCallbackDropsExternalNextWhenLoginFails() {
        stubGoogleClient();
        when(oAuthStateCookieService.matches(request, OAuthProvider.GOOGLE, "state-token"))
                .thenReturn(true);
        when(oAuthStateStore.consume(OAuthProvider.GOOGLE, "state-token"))
                .thenReturn(Optional.of("https://app.example.com/auth/google/callback?next=https://evil.example.com"));
        when(oAuthLoginService.handle(OAuthProvider.GOOGLE, "broken-code", request))
                .thenThrow(new UnauthorizedException("exchange failed"));

        URI redirect = service.completeCallback(OAuthProvider.GOOGLE, "broken-code", "state-token", request, response);

        assertThat(redirect.toString()).isEqualTo("https://app.example.com/signin?error=auth_failed");
    }

    @Test
    void completeCallbackDropsProtocolRelativeNextWhenLoginFails() {
        stubGoogleClient();
        when(oAuthStateCookieService.matches(request, OAuthProvider.GOOGLE, "state-token"))
                .thenReturn(true);
        when(oAuthStateStore.consume(OAuthProvider.GOOGLE, "state-token"))
                .thenReturn(Optional.of("https://app.example.com/auth/google/callback?next=//evil.example.com"));
        when(oAuthLoginService.handle(OAuthProvider.GOOGLE, "broken-code", request))
                .thenThrow(new UnauthorizedException("exchange failed"));

        URI redirect = service.completeCallback(OAuthProvider.GOOGLE, "broken-code", "state-token", request, response);

        assertThat(redirect.toString()).isEqualTo("https://app.example.com/signin?error=auth_failed");
    }

    @Test
    void completeCallbackReturnsMissingCodeRedirectBeforeConsumingState() {
        stubGoogleClient();

        URI redirect = service.completeCallback(OAuthProvider.GOOGLE, " ", "state-token", request, response);

        assertThat(redirect.toString()).isEqualTo("https://app.example.com/signin?error=missing_code");
        verifyNoInteractions(oAuthStateStore);
        verify(oAuthStateCookieService).clear(request, response, OAuthProvider.GOOGLE);
    }

    @Test
    void completeCallbackClearsStateCookieWhenStateIsMissing() {
        stubGoogleClient();

        URI redirect = service.completeCallback(OAuthProvider.GOOGLE, "valid-code", " ", request, response);

        assertThat(redirect.toString()).isEqualTo("https://app.example.com/signin?error=invalid_state");
        verifyNoInteractions(oAuthStateStore);
        verify(oAuthStateCookieService).clear(request, response, OAuthProvider.GOOGLE);
    }

    private void stubGoogleClient() {
        when(oAuthLoginService.findClient(OAuthProvider.GOOGLE)).thenReturn(Optional.of(oAuthProviderClient));
    }

    private static AuthenticationTokens tokenPair() {
        return new AuthenticationTokens("jwt-token", "refresh-token");
    }
}
