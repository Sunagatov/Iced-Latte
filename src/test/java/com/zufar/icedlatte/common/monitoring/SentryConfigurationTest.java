package com.zufar.icedlatte.common.monitoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import io.sentry.*;
import io.sentry.protocol.Message;
import io.sentry.protocol.Request;
import io.sentry.protocol.SentryTransaction;

@DisplayName("SentryConfiguration unit tests")
@SuppressWarnings("StaticImportCanBeUsed")
class SentryConfigurationTest {

    @Test
    @DisplayName("before-send callback drops non-error events and sanitizes 5xx events")
    void beforeSendCallbackFiltersAndSanitizes() {
        SentryConfiguration configuration = configuration();
        SentryOptions.BeforeSendCallback callback = configuration.beforeSendCallback();
        Hint hint = new Hint();

        SentryEvent infoEvent = new SentryEvent();
        infoEvent.setLevel(SentryLevel.INFO);

        assertThat(callback.execute(infoEvent, hint)).isNull();

        SentryEvent errorEvent = new SentryEvent();
        errorEvent.setLevel(SentryLevel.ERROR);
        Request request = new Request();
        request.setHeaders(new HashMap<>(Map.of(
                "authorization", "Bearer token",
                "COOKIE", "sid=1",
                "X-Trace-ID", "trace")));
        errorEvent.setRequest(request);

        SentryEvent result = Objects.requireNonNull(callback.execute(errorEvent, hint));
        Request sanitizedRequest = Objects.requireNonNull(result.getRequest());

        assertThat(result).isSameAs(errorEvent);
        assertThat(sanitizedRequest.getHeaders()).isNotNull();
        assertThat(sanitizedRequest.getHeaders()).doesNotContainKeys("authorization", "COOKIE");
        assertThat(sanitizedRequest.getHeaders()).containsEntry("X-Trace-ID", "trace");
        assertThat(result.getTag("application")).isEqualTo("iced-latte");
        assertThat(result.getTag("version")).isEqualTo("2026.04");
    }

    @Test
    @DisplayName("before-send callback redacts client IPs from captured log messages")
    void beforeSendCallbackRedactsClientIpFromCapturedLogMessages() {
        SentryConfiguration configuration = configuration();
        SentryOptions.BeforeSendCallback callback = configuration.beforeSendCallback();

        SentryEvent errorEvent = new SentryEvent();
        errorEvent.setLevel(SentryLevel.ERROR);
        Message message = new Message();
        message.setMessage(
                "http.request.completed: method=GET, path=/api/v1/orders/{orderId}, status=500, duration_ms=42,"
                        + " client_ip=203.0.113.10, authenticated=true, outcome=SERVER_ERROR");
        message.setFormatted(
                "http.request.completed: method=GET, path=/api/v1/orders/{orderId}, status=500, duration_ms=42,"
                        + " client_ip=203.0.113.10, authenticated=true, outcome=SERVER_ERROR");
        message.setParams(List.of("203.0.113.10"));
        errorEvent.setMessage(message);

        SentryEvent result = Objects.requireNonNull(callback.execute(errorEvent, new Hint()));
        Message sanitizedMessage = Objects.requireNonNull(result.getMessage());

        assertThat(sanitizedMessage.getMessage()).contains("client_ip=[redacted]");
        assertThat(sanitizedMessage.getMessage()).doesNotContain("203.0.113.10");
        assertThat(sanitizedMessage.getFormatted()).contains("client_ip=[redacted]");
        assertThat(sanitizedMessage.getFormatted()).doesNotContain("203.0.113.10");
    }

    @Test
    @DisplayName("before-send callback fingerprints http access errors by route and status family")
    void beforeSendCallbackFingerprintsHttpAccessErrorsByRouteAndStatusFamily() {
        SentryConfiguration configuration = configuration();
        SentryOptions.BeforeSendCallback callback = configuration.beforeSendCallback();

        SentryEvent errorEvent = new SentryEvent();
        errorEvent.setLevel(SentryLevel.ERROR);
        errorEvent.setLogger("http.access");
        Message message = new Message();
        message.setFormatted(
                "http.request.completed: method=GET, path=/api/v1/orders/{orderId}, status=503, duration_ms=42,"
                        + " authenticated=true, outcome=SERVER_ERROR");
        errorEvent.setMessage(message);

        SentryEvent result = Objects.requireNonNull(callback.execute(errorEvent, new Hint()));

        assertThat(result.getFingerprints())
                .containsExactly("http.access", "GET", "/api/v1/orders/{orderId}", "5xx", "{{ default }}");
    }

    @Test
    @DisplayName("before-breadcrumb callback strips sensitive breadcrumb data")
    void beforeBreadcrumbCallbackSanitizesSensitiveData() {
        SentryConfiguration configuration = configuration();
        SentryOptions.BeforeBreadcrumbCallback callback = configuration.beforeBreadcrumbCallback();
        Hint hint = new Hint();
        Breadcrumb breadcrumb = new Breadcrumb();
        breadcrumb.setData("Email", "user@example.com");
        breadcrumb.setData("PASSWORD", "secret");
        breadcrumb.setData("phone", "123");
        breadcrumb.setData("safe", "ok");

        Breadcrumb result = Objects.requireNonNull(callback.execute(breadcrumb, hint));
        Object email = result.getData("Email");
        Object password = result.getData("PASSWORD");
        Object phone = result.getData("phone");
        Object safe = result.getData("safe");

        assertThat(result).isSameAs(breadcrumb);
        assertThat(email).isNull();
        assertThat(password).isNull();
        assertThat(phone).isNull();
        assertThat(safe).isEqualTo("ok");
    }

    @Test
    @DisplayName("before-breadcrumb callback strips sensitive breadcrumb data regardless of key casing")
    void beforeBreadcrumbCallbackSanitizesSensitiveDataRegardlessOfKeyCasing() {
        SentryConfiguration configuration = configuration();
        SentryOptions.BeforeBreadcrumbCallback callback = configuration.beforeBreadcrumbCallback();
        Breadcrumb breadcrumb = new Breadcrumb();
        breadcrumb.setData("eMaIl", "user@example.com");
        breadcrumb.setData("PaSsWoRd", "secret");

        Breadcrumb result = Objects.requireNonNull(callback.execute(breadcrumb, new Hint()));

        assertThat(result.getData("eMaIl")).isNull();
        assertThat(result.getData("PaSsWoRd")).isNull();
    }

    @Test
    @DisplayName("before-breadcrumb callback tolerates breadcrumbs without data")
    void beforeBreadcrumbCallbackToleratesBreadcrumbsWithoutData() {
        SentryConfiguration configuration = configuration();
        SentryOptions.BeforeBreadcrumbCallback callback = configuration.beforeBreadcrumbCallback();
        Breadcrumb breadcrumb = new Breadcrumb();

        Breadcrumb result = callback.execute(breadcrumb, new Hint());

        assertThat(result).isSameAs(breadcrumb);
    }

    @Test
    @DisplayName("trace sampler uses endpoint-specific sampling rates")
    @SuppressWarnings("deprecation")
    void tracesSamplerUsesEndpointCategories() {
        SentryConfiguration configuration = configuration();
        SentryOptions.TracesSamplerCallback callback = configuration.tracesSamplerCallback();
        CustomSamplingContext customSamplingContext = new CustomSamplingContext();

        assertThat(callback.sample(new SamplingContext(
                        new TransactionContext("/api/v1/auth/authenticate", "http.server"), customSamplingContext)))
                .isEqualTo(1.0);
        assertThat(callback.sample(new SamplingContext(
                        new TransactionContext("/api/v1/products/42", "http.server"), customSamplingContext)))
                .isEqualTo(0.5);
        assertThat(callback.sample(new SamplingContext(
                        new TransactionContext("/actuator/info", "http.server"), customSamplingContext)))
                .isEqualTo(0.1);
    }

    @Test
    @DisplayName("trace sampler matches only configured path prefixes")
    @SuppressWarnings("deprecation")
    void tracesSamplerMatchesOnlyConfiguredPathPrefixes() {
        SentryConfiguration configuration = configuration();
        SentryOptions.TracesSamplerCallback callback = configuration.tracesSamplerCallback();
        CustomSamplingContext customSamplingContext = new CustomSamplingContext();

        assertThat(callback.sample(new SamplingContext(
                        new TransactionContext("/internal/proxy/api/v1/products/42", "http.server"),
                        customSamplingContext)))
                .isEqualTo(0.1);
        assertThat(callback.sample(new SamplingContext(
                        new TransactionContext("/debug/api/v1/orders/123", "http.server"), customSamplingContext)))
                .isEqualTo(0.1);
    }

    @Test
    @DisplayName("trace sampler does not treat lookalike prefixes as configured paths")
    @SuppressWarnings("deprecation")
    void tracesSamplerDoesNotTreatLookalikePrefixesAsConfiguredPaths() {
        SentryConfiguration configuration = configurationWithoutTrailingSlashes();
        SentryOptions.TracesSamplerCallback callback = configuration.tracesSamplerCallback();
        CustomSamplingContext customSamplingContext = new CustomSamplingContext();

        assertThat(callback.sample(new SamplingContext(
                        new TransactionContext("/api/v1/cartography/42", "http.server"), customSamplingContext)))
                .isEqualTo(0.1);
        assertThat(callback.sample(new SamplingContext(
                        new TransactionContext("/api/v1/authenticate-extra", "http.server"), customSamplingContext)))
                .isEqualTo(0.1);
    }

    @Test
    @DisplayName("before-send-transaction adds tags and skips health checks")
    void beforeSendTransactionAddsTagsAndSkipsHealthChecks() {
        SentryConfiguration configuration = configuration();
        SentryOptions.BeforeSendTransactionCallback callback = configuration.beforeSendTransactionCallback();
        Hint hint = new Hint();

        SentryTransaction transaction = Mockito.mock(SentryTransaction.class);
        when(transaction.getTransaction()).thenReturn("/api/v1/products/42");

        SentryTransaction result = callback.execute(transaction, hint);

        assertThat(result).isSameAs(transaction);
        verify(transaction).setTag("application", "iced-latte");
        verify(transaction).setTag("version", "2026.04");

        SentryTransaction health = Mockito.mock(SentryTransaction.class);
        when(health.getTransaction()).thenReturn("GET /actuator/health");

        assertThat(callback.execute(health, hint)).isNull();

        SentryTransaction nestedHealth = Mockito.mock(SentryTransaction.class);
        when(nestedHealth.getTransaction()).thenReturn("/actuator/health/readiness");

        assertThat(callback.execute(nestedHealth, hint)).isNull();

        SentryTransaction lookalike = Mockito.mock(SentryTransaction.class);
        when(lookalike.getTransaction()).thenReturn("GET /actuator/healthcheck");

        assertThat(callback.execute(lookalike, hint)).isSameAs(lookalike);
    }

    private SentryConfiguration configuration() {
        SentryConfiguration configuration = new SentryConfiguration();
        ReflectionTestUtils.setField(configuration, "applicationName", "iced-latte");
        ReflectionTestUtils.setField(configuration, "applicationVersion", "2026.04");
        ReflectionTestUtils.setField(
                configuration, "traceCriticalPathPrefixes", "/api/v1/auth/,/api/v1/payment/,/api/v1/orders/");
        ReflectionTestUtils.setField(configuration, "traceUserFacingPathPrefixes", "/api/v1/products/,/api/v1/cart/");
        ReflectionTestUtils.setField(configuration, "traceCriticalSampleRate", 1.0);
        ReflectionTestUtils.setField(configuration, "traceUserFacingSampleRate", 0.5);
        ReflectionTestUtils.setField(configuration, "traceDefaultSampleRate", 0.1);
        return configuration;
    }

    private SentryConfiguration configurationWithoutTrailingSlashes() {
        SentryConfiguration configuration = new SentryConfiguration();
        ReflectionTestUtils.setField(configuration, "applicationName", "iced-latte");
        ReflectionTestUtils.setField(configuration, "applicationVersion", "2026.04");
        ReflectionTestUtils.setField(configuration, "traceCriticalPathPrefixes", "/api/v1/auth,/api/v1/payment");
        ReflectionTestUtils.setField(configuration, "traceUserFacingPathPrefixes", "/api/v1/products,/api/v1/cart");
        ReflectionTestUtils.setField(configuration, "traceCriticalSampleRate", 1.0);
        ReflectionTestUtils.setField(configuration, "traceUserFacingSampleRate", 0.5);
        ReflectionTestUtils.setField(configuration, "traceDefaultSampleRate", 0.1);
        return configuration;
    }
}
