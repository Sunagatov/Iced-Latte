package com.zufar.icedlatte.security.turnstile;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@DisplayName("TurnstileVerifier unit tests")
class TurnstileVerifierTest {

    @Nested
    @DisplayName("When disabled (blank secret key)")
    class Disabled {

        private final TurnstileVerifier verifier = new TurnstileVerifier("");

        @Test
        @DisplayName("should skip verification when token is null")
        void skipWhenTokenNull() {
            assertThatCode(() -> verifier.verify(null)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should skip verification when token is present")
        void skipWhenTokenPresent() {
            assertThatCode(() -> verifier.verify("some-token")).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("When enabled (secret key set)")
    class Enabled {

        private final TurnstileVerifier verifier = new TurnstileVerifier("test-secret");

        @Test
        @DisplayName("should throw when token is null")
        void throwWhenTokenNull() {
            assertThatThrownBy(() -> verifier.verify(null))
                    .isInstanceOf(TurnstileVerificationException.class)
                    .hasMessage("Turnstile verification required");
        }

        @Test
        @DisplayName("should throw when token is blank")
        void throwWhenTokenBlank() {
            assertThatThrownBy(() -> verifier.verify(""))
                    .isInstanceOf(TurnstileVerificationException.class)
                    .hasMessage("Turnstile verification required");
        }

        @Test
        @DisplayName("should throw when Cloudflare returns success=false")
        void throwWhenVerificationFails() {
            var restClient = buildMockedRestClient("{\"success\": false}");
            ReflectionTestUtils.setField(verifier, "restClient", restClient);

            assertThatThrownBy(() -> verifier.verify("invalid-token"))
                    .isInstanceOf(TurnstileVerificationException.class)
                    .hasMessage("Turnstile verification failed");
        }

        @Test
        @DisplayName("should pass when Cloudflare returns success=true")
        void passWhenVerificationSucceeds() {
            var restClient = buildMockedRestClient("{\"success\": true}");
            ReflectionTestUtils.setField(verifier, "restClient", restClient);

            assertThatCode(() -> verifier.verify("valid-token")).doesNotThrowAnyException();
        }

        private RestClient buildMockedRestClient(String responseBody) {
            var builder = RestClient.builder();
            var server = MockRestServiceServer.bindTo(builder).build();
            server.expect(requestTo("https://challenges.cloudflare.com/turnstile/v0/siteverify"))
                    .andExpect(method(HttpMethod.POST))
                    .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));
            return builder.build();
        }
    }
}
