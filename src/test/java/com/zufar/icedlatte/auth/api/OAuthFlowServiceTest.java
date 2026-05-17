package com.zufar.icedlatte.auth.api;

import com.zufar.icedlatte.openapi.dto.UserAuthenticationResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.URI;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OAuthFlowServiceTest {

    @Mock private OAuthLoginService oAuthLoginService;
    @Mock private OAuthStateStore oAuthStateStore;
    @Mock private OAuthProviderClient oAuthProviderClient;
    @Mock private HttpServletRequest request;

    private OAuthFlowService service;

    @BeforeEach
    void setUp() {
        service = new OAuthFlowService(oAuthLoginService, oAuthStateStore);
        ReflectionTestUtils.setField(service, "frontendUrl", "https://app.example.com");
    }

    @Test
    void initiateStoresAllowedCallbackAndReturnsProviderAuthorizationUri() {
        stubGoogleClient();
        when(oAuthProviderClient.buildAuthorizationUri(anyString()))
                .thenReturn(URI.create("https://accounts.google.com/o/oauth2/v2/auth"));

        Optional<URI> result = service.initiate(
                OAuthProvider.GOOGLE,
                "https://app.example.com:443/auth/google/callback?next=/checkout"
        );

        assertThat(result).contains(URI.create("https://accounts.google.com/o/oauth2/v2/auth"));
        ArgumentCaptor<String> callbackBase = ArgumentCaptor.forClass(String.class);
        verify(oAuthStateStore).store(eq(OAuthProvider.GOOGLE), anyString(), callbackBase.capture());
        assertThat(callbackBase.getValue())
                .isEqualTo("https://app.example.com:443/auth/google/callback?next=/checkout");
    }

    @Test
    void initiateFallsBackToDefaultCallbackWhenRedirectOriginIsNotAllowed() {
        stubGoogleClient();
        when(oAuthProviderClient.buildAuthorizationUri(anyString()))
                .thenReturn(URI.create("https://accounts.google.com/o/oauth2/v2/auth"));

        service.initiate(OAuthProvider.GOOGLE, "https://evil.example.com/auth/google/callback");

        ArgumentCaptor<String> callbackBase = ArgumentCaptor.forClass(String.class);
        verify(oAuthStateStore).store(eq(OAuthProvider.GOOGLE), anyString(), callbackBase.capture());
        assertThat(callbackBase.getValue()).isEqualTo("https://app.example.com/auth/google/callback");
    }

    @Test
    void initiateReturnsEmptyWhenProviderClientIsNotRegistered() {
        when(oAuthLoginService.findClient(OAuthProvider.GOOGLE)).thenReturn(Optional.empty());

        Optional<URI> result = service.initiate(OAuthProvider.GOOGLE, null);

        assertThat(result).isEmpty();
        verifyNoInteractions(oAuthStateStore, oAuthProviderClient);
    }

    @Test
    void completeCallbackReturnsCallbackWithFragmentTokens() {
        stubGoogleClient();
        when(oAuthStateStore.consume(OAuthProvider.GOOGLE, "state-token"))
                .thenReturn("https://app.example.com/auth/google/callback?next=/checkout");
        when(oAuthLoginService.handle(OAuthProvider.GOOGLE, "valid-code", request))
                .thenReturn(tokenPair());

        URI redirect = service.completeCallback(OAuthProvider.GOOGLE, "valid-code", "state-token", request);

        assertThat(redirect.toString())
                .isEqualTo("https://app.example.com/auth/google/callback?next=/checkout#token=jwt-token&refreshToken=refresh-token");
    }

    @Test
    void completeCallbackReturnsInvalidStateRedirectWhenStateWasNotStored() {
        stubGoogleClient();
        when(oAuthStateStore.consume(OAuthProvider.GOOGLE, "missing")).thenReturn(null);

        URI redirect = service.completeCallback(OAuthProvider.GOOGLE, "valid-code", "missing", request);

        assertThat(redirect.toString()).isEqualTo("https://app.example.com/signin?error=invalid_state");
    }

    @Test
    void completeCallbackReturnsAuthFailedRedirectAndPreservesNextWhenLoginFails() {
        stubGoogleClient();
        when(oAuthStateStore.consume(OAuthProvider.GOOGLE, "state-token"))
                .thenReturn("https://app.example.com/auth/google/callback?next=/checkout");
        when(oAuthLoginService.handle(OAuthProvider.GOOGLE, "broken-code", request))
                .thenThrow(new IllegalStateException("exchange failed"));

        URI redirect = service.completeCallback(OAuthProvider.GOOGLE, "broken-code", "state-token", request);

        assertThat(redirect.toString()).isEqualTo("https://app.example.com/signin?error=auth_failed&next=/checkout");
    }

    @Test
    void completeCallbackReturnsMissingCodeRedirectBeforeConsumingState() {
        stubGoogleClient();

        URI redirect = service.completeCallback(OAuthProvider.GOOGLE, " ", "state-token", request);

        assertThat(redirect.toString()).isEqualTo("https://app.example.com/signin?error=missing_code");
        verifyNoInteractions(oAuthStateStore);
    }

    private void stubGoogleClient() {
        when(oAuthLoginService.findClient(OAuthProvider.GOOGLE)).thenReturn(Optional.of(oAuthProviderClient));
    }

    private static UserAuthenticationResponse tokenPair() {
        UserAuthenticationResponse response = new UserAuthenticationResponse();
        response.setToken("jwt-token");
        response.setRefreshToken("refresh-token");
        return response;
    }
}
