package com.zufar.icedlatte.common.turnstile;

import java.time.Duration;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.web.client.RestClient;

@DisplayName("TurnstileVerifier unit tests")
class TurnstileVerifierTest {

    @Nested
    @DisplayName("When disabled")
    class Disabled {

        private final TurnstileVerifier verifier = new TurnstileVerifier(TurnstileProperties.disabled());

        @Test
        @DisplayName("should skip verification when token is null")
        void skipWhenTokenNull() {
            Assertions.assertThatCode(() -> verifier.verify(null)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should skip verification when token is present")
        void skipWhenTokenPresent() {
            Assertions.assertThatCode(() -> verifier.verify("some-token")).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("When enabled (secret key set)")
    class Enabled {

        private final TurnstileVerifier verifier = new TurnstileVerifier(TurnstileProperties.enabledForTests());

        @Test
        @DisplayName("should throw when token is null")
        void throwWhenTokenNull() {
            Assertions.assertThatThrownBy(() -> verifier.verify(null))
                    .isInstanceOf(TurnstileVerificationException.class)
                    .hasMessage("Turnstile verification required");
        }

        @Test
        @DisplayName("should throw when token is blank")
        void throwWhenTokenBlank() {
            Assertions.assertThatThrownBy(() -> verifier.verify(""))
                    .isInstanceOf(TurnstileVerificationException.class)
                    .hasMessage("Turnstile verification required");
        }

        @Test
        @DisplayName("should throw when Cloudflare returns success=false")
        void throwWhenVerificationFails() {
            var restClient = buildMockedRestClient("{\"success\": false}");
            var verifier = new TurnstileVerifier(TurnstileProperties.enabledForTests(), restClient);

            Assertions.assertThatThrownBy(() -> verifier.verify("invalid-token"))
                    .isInstanceOf(TurnstileVerificationException.class)
                    .hasMessage("Turnstile verification failed");
        }

        @Test
        @DisplayName("should pass when Cloudflare returns success=true")
        void passWhenVerificationSucceeds() {
            var restClient = buildMockedRestClient("{\"success\": true}");
            var verifier = new TurnstileVerifier(TurnstileProperties.enabledForTests(), restClient);

            Assertions.assertThatCode(() -> verifier.verify("valid-token")).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should construct RestClient with explicit timeout settings")
        void constructsRestClientWithExplicitTimeoutSettings() {
            Assertions.assertThatCode(() -> new TurnstileVerifier(new TurnstileProperties(
                            true, false, false, false, "test-secret", Duration.ofMillis(500), Duration.ofSeconds(1))))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should trim secret key")
        void trimsSecretKey() {
            TurnstileProperties properties = new TurnstileProperties(
                    true, false, false, false, " test-secret ", Duration.ofMillis(500), Duration.ofSeconds(1));

            Assertions.assertThat(properties.secretKey()).isEqualTo("test-secret");
        }

        @Test
        @DisplayName("should fail fast when enabled without secret key")
        void failFastWhenEnabledWithoutSecretKey() {
            Assertions.assertThatThrownBy(() -> bindTurnstileProperties("turnstile.enabled", "true"))
                    .isInstanceOf(org.springframework.boot.context.properties.bind.BindException.class)
                    .hasRootCauseInstanceOf(IllegalStateException.class)
                    .hasRootCauseMessage("turnstile.secret-key must be configured when turnstile.enabled=true");
        }

        @Test
        @DisplayName("should fail fast when a feature flag is enabled while Turnstile is disabled")
        void failFastWhenFeatureProtectionEnabledWithoutGlobalTurnstile() {
            Assertions.assertThatThrownBy(() ->
                            bindTurnstileProperties("turnstile.enabled", "false", "turnstile.checkout-enabled", "true"))
                    .isInstanceOf(org.springframework.boot.context.properties.bind.BindException.class)
                    .hasRootCauseInstanceOf(IllegalStateException.class)
                    .hasRootCauseMessage(
                            "turnstile.enabled must be true when feature-specific Turnstile protection is enabled");
        }

        @Test
        @DisplayName("should fail fast when timeout settings are not positive")
        void failFastWhenTimeoutSettingsAreNotPositive() {
            Assertions.assertThatThrownBy(() -> new TurnstileProperties(
                            false, false, false, false, "", Duration.ZERO, Duration.ofSeconds(1)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("turnstile.connect-timeout must be positive");
            Assertions.assertThatThrownBy(() -> new TurnstileProperties(
                            false, false, false, false, "", Duration.ofSeconds(1), Duration.ZERO))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("turnstile.read-timeout must be positive");
        }

        private void bindTurnstileProperties(String... entries) {
            MapConfigurationPropertySource source = new MapConfigurationPropertySource();
            for (int i = 0; i < entries.length; i += 2) {
                source.put(entries[i], entries[i + 1]);
            }
            new Binder(source).bind("turnstile", TurnstileProperties.class).get();
        }

        private RestClient buildMockedRestClient(String responseBody) {
            var builder = RestClient.builder();
            var server = MockRestServiceServer.bindTo(builder).build();
            server.expect(MockRestRequestMatchers.requestTo(
                            "https://challenges.cloudflare.com/turnstile/v0/siteverify"))
                    .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                    .andRespond(MockRestResponseCreators.withSuccess(responseBody, MediaType.APPLICATION_JSON));
            return builder.build();
        }
    }
}
