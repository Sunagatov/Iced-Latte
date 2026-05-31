package com.zufar.icedlatte.security.oauth.flow;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.zufar.icedlatte.security.oauth.config.OAuthProvider;

class OAuthStateCookieServiceTest {

    private final OAuthStateCookieService service = new OAuthStateCookieService();

    @Test
    void bindSetsHttpOnlySameSiteLaxCookieOnOAuthPath() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        service.bind(request, response, OAuthProvider.GOOGLE, "state-token", Duration.ofMinutes(10));

        assertThat(response.getHeader(HttpHeaders.SET_COOKIE))
                .contains("iced_latte_oauth_state_google=state-token")
                .contains("Path=/api/v1/auth/oauth")
                .contains("Max-Age=600")
                .contains("HttpOnly")
                .contains("SameSite=Lax")
                .doesNotContain("Secure");
    }

    @Test
    void bindMarksCookieSecureBehindForwardedHttpsProxy() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-Proto", "https");
        MockHttpServletResponse response = new MockHttpServletResponse();

        service.bind(request, response, OAuthProvider.GOOGLE, "state-token", Duration.ofMinutes(10));

        assertThat(response.getHeader(HttpHeaders.SET_COOKIE)).contains("Secure");
    }

    @Test
    void matchesRequiresCookieValueToEqualCallbackState() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new jakarta.servlet.http.Cookie("iced_latte_oauth_state_google", "state-token"));

        assertThat(service.matches(request, OAuthProvider.GOOGLE, "state-token"))
                .isTrue();
        assertThat(service.matches(request, OAuthProvider.GOOGLE, "other-state"))
                .isFalse();
    }

    @Test
    void clearExpiresProviderCookieOnOAuthPath() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        service.clear(request, response, OAuthProvider.GOOGLE);

        assertThat(response.getHeader(HttpHeaders.SET_COOKIE))
                .contains("iced_latte_oauth_state_google=")
                .contains("Path=/api/v1/auth/oauth")
                .contains("Max-Age=0")
                .contains("HttpOnly")
                .contains("SameSite=Lax");
    }
}
