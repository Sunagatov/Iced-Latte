package com.zufar.icedlatte.common.monitoring;

import io.sentry.Breadcrumb;
import io.sentry.CustomSamplingContext;
import io.sentry.Hint;
import io.sentry.SamplingContext;
import io.sentry.SentryEvent;
import io.sentry.SentryLevel;
import io.sentry.SentryOptions;
import io.sentry.TransactionContext;
import io.sentry.protocol.Request;
import io.sentry.protocol.SentryTransaction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("SentryConfiguration unit tests")
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
                "Authorization", "Bearer token",
                "Cookie", "sid=1",
                "X-Trace-ID", "trace"
        )));
        errorEvent.setRequest(request);

        SentryEvent result = Objects.requireNonNull(callback.execute(errorEvent, hint));
        Request sanitizedRequest = Objects.requireNonNull(result.getRequest());

        assertThat(result).isSameAs(errorEvent);
        assertThat(sanitizedRequest.getHeaders()).isNotNull();
        assertThat(sanitizedRequest.getHeaders()).doesNotContainKeys("Authorization", "Cookie");
        assertThat(sanitizedRequest.getHeaders()).containsEntry("X-Trace-ID", "trace");
        assertThat(result.getTag("application")).isEqualTo("iced-latte");
        assertThat(result.getTag("version")).isEqualTo("2026.04");
    }

    @Test
    @DisplayName("before-breadcrumb callback strips sensitive breadcrumb data")
    void beforeBreadcrumbCallbackSanitizesSensitiveData() {
        SentryConfiguration configuration = configuration();
        SentryOptions.BeforeBreadcrumbCallback callback = configuration.beforeBreadcrumbCallback();
        Hint hint = new Hint();
        Breadcrumb breadcrumb = new Breadcrumb();
        breadcrumb.setData("email", "user@example.com");
        breadcrumb.setData("password", "secret");
        breadcrumb.setData("phone", "123");
        breadcrumb.setData("safe", "ok");

        Breadcrumb result = Objects.requireNonNull(callback.execute(breadcrumb, hint));
        Object email = result.getData("email");
        Object password = result.getData("password");
        Object phone = result.getData("phone");
        Object safe = result.getData("safe");

        assertThat(result).isSameAs(breadcrumb);
        assertThat(email).isNull();
        assertThat(password).isNull();
        assertThat(phone).isNull();
        assertThat(safe).isEqualTo("ok");
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
    }

    private SentryConfiguration configuration() {
        SentryConfiguration configuration = new SentryConfiguration();
        ReflectionTestUtils.setField(configuration, "applicationName", "iced-latte");
        ReflectionTestUtils.setField(configuration, "applicationVersion", "2026.04");
        return configuration;
    }
}
