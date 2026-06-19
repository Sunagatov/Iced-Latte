package com.zufar.icedlatte.common.turnstile;

import java.time.Duration;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.hamcrest.Matchers;
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

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

@DisplayName("TurnstileVerifier unit tests")
class TurnstileVerifierTest {

    @Nested
    @DisplayName("When disabled")
    class Disabled {

        private final TurnstileVerifier verifier = new TurnstileVerifier(TurnstileProperties.disabled());

        @Test
        @DisplayName("should skip verification when token is null")
        void skipWhenTokenNull() {
            Assertions.assertThatCode(() -> verifier.verify((String) null)).doesNotThrowAnyException();
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
            Assertions.assertThatThrownBy(() -> verifier.verify((String) null))
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
            var restClient = buildMockedRestClient("{\"success\": true, \"hostname\": \"app.iced-latte.uk\"}");
            var verifier = new TurnstileVerifier(TurnstileProperties.enabledForTests(), restClient);

            Assertions.assertThatCode(() -> verifier.verify("valid-token")).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should include remote IP when provided")
        void includesRemoteIpWhenProvided() {
            var builder = RestClient.builder();
            var server = MockRestServiceServer.bindTo(builder).build();
            server.expect(MockRestRequestMatchers.requestTo(
                            "https://challenges.cloudflare.com/turnstile/v0/siteverify"))
                    .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                    .andExpect(MockRestRequestMatchers.content().string(Matchers.containsString("idempotency_key=")))
                    .andExpect(
                            MockRestRequestMatchers.content().string(Matchers.containsString("remoteip=203.0.113.10")))
                    .andRespond(MockRestResponseCreators.withSuccess(
                            "{\"success\": true, \"hostname\": \"app.iced-latte.uk\"}", MediaType.APPLICATION_JSON));
            var verifier = new TurnstileVerifier(TurnstileProperties.enabledForTests(), builder.build());

            Assertions.assertThatCode(() -> verifier.verify("valid-token", "203.0.113.10"))
                    .doesNotThrowAnyException();
            server.verify();
        }

        @Test
        @DisplayName("should reject tokens longer than Cloudflare allows")
        void rejectWhenTokenTooLong() {
            Assertions.assertThatThrownBy(() -> verifier.verify("a".repeat(2049)))
                    .isInstanceOf(TurnstileVerificationException.class)
                    .hasMessage("Turnstile verification failed");
        }

        @Test
        @DisplayName("should reject success response with unexpected hostname")
        void rejectWhenHostnameDoesNotMatchExpectedHostnames() {
            var restClient = buildMockedRestClient("{\"success\": true, \"hostname\": \"attacker.example\"}");
            var properties = new TurnstileProperties(
                    true,
                    false,
                    false,
                    false,
                    "test-secret",
                    Duration.ofSeconds(2),
                    Duration.ofSeconds(3),
                    List.of("app.iced-latte.uk", "localhost"));
            var verifier = new TurnstileVerifier(properties, restClient);

            Assertions.assertThatThrownBy(() -> verifier.verify("valid-token"))
                    .isInstanceOf(TurnstileVerificationException.class)
                    .hasMessage("Turnstile verification failed");
        }

        @Test
        @DisplayName("should record outcome metrics for rejected responses")
        void recordsMetricsForRejectedResponses() {
            var meterRegistry = new SimpleMeterRegistry();
            var restClient = buildMockedRestClient("{\"success\": false, \"error-codes\": [\"timeout-or-duplicate\"]}");
            var verifier = new TurnstileVerifier(TurnstileProperties.enabledForTests(), restClient, meterRegistry);

            Assertions.assertThatThrownBy(() -> verifier.verify(
                            new TurnstileVerificationRequest("invalid-token", "203.0.113.10", "login", null)))
                    .isInstanceOf(TurnstileVerificationException.class)
                    .hasMessage("Turnstile verification failed");

            Assertions.assertThat(meterRegistry
                            .get("security.turnstile.verifications")
                            .tag("source", "login")
                            .tag("outcome", "rejected")
                            .counter()
                            .count())
                    .isEqualTo(1.0);
        }

        @Test
        @DisplayName("should construct RestClient with explicit timeout settings")
        void constructsRestClientWithExplicitTimeoutSettings() {
            Assertions.assertThatCode(() -> new TurnstileVerifier(new TurnstileProperties(
                            true,
                            false,
                            false,
                            false,
                            "test-secret",
                            Duration.ofMillis(500),
                            Duration.ofSeconds(1),
                            List.of())))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should trim secret key")
        void trimsSecretKey() {
            TurnstileProperties properties = new TurnstileProperties(
                    true,
                    false,
                    false,
                    false,
                    " test-secret ",
                    Duration.ofMillis(500),
                    Duration.ofSeconds(1),
                    List.of(" app.iced-latte.uk ", "localhost"));

            Assertions.assertThat(properties.secretKey()).isEqualTo("test-secret");
            Assertions.assertThat(properties.expectedHostnames()).containsExactly("app.iced-latte.uk", "localhost");
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
                            false, false, false, false, "", Duration.ZERO, Duration.ofSeconds(1), List.of()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("turnstile.connect-timeout must be positive");
            Assertions.assertThatThrownBy(() -> new TurnstileProperties(
                            false, false, false, false, "", Duration.ofSeconds(1), Duration.ZERO, List.of()))
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
