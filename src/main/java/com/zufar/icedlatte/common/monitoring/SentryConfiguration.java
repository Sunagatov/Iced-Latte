package com.zufar.icedlatte.common.monitoring;

import io.sentry.Breadcrumb;
import io.sentry.SentryEvent;
import io.sentry.SentryOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Configuration
@ConditionalOnProperty(name = "sentry.enabled", havingValue = "true")
public class SentryConfiguration {

    private static final Set<String> SENSITIVE_HEADER_NAMES =
            Set.of(HttpHeaders.AUTHORIZATION.toLowerCase(Locale.ROOT), HttpHeaders.COOKIE.toLowerCase(Locale.ROOT));
    private static final Set<String> SENSITIVE_BREADCRUMB_KEYS =
            Set.of("email", "password", "phone");

    @Value("${spring.application.name}")
    private String applicationName;

    @Value("${spring.application.version:unknown}")
    private String applicationVersion;

    @Value("${sentry.trace-critical-path-prefixes:}")
    private String traceCriticalPathPrefixes;

    @Value("${sentry.trace-user-facing-path-prefixes:}")
    private String traceUserFacingPathPrefixes;

    @Bean
    public SentryOptions.BeforeSendCallback beforeSendCallback() {
        return (event, _) -> {
            // Only send server errors (5xx) to Sentry. 4xx are client errors — expected, not bugs.
            if (event.getLevel() != null
                    && event.getLevel() != io.sentry.SentryLevel.ERROR
                    && event.getLevel() != io.sentry.SentryLevel.FATAL) {
                return null;
            }
            sanitizePii(event);
            addCustomTags(event);
            return event;
        };
    }

    @Bean
    public SentryOptions.BeforeBreadcrumbCallback beforeBreadcrumbCallback() {
        return (breadcrumb, _) -> {
            sanitizeBreadcrumb(breadcrumb);
            return breadcrumb;
        };
    }

    @Bean
    public SentryOptions.TracesSamplerCallback tracesSamplerCallback() {
        return samplingContext -> {
            var transactionContext = samplingContext.getTransactionContext();
            var transactionName = transactionContext.getName();

            if (containsAnyConfiguredPrefix(transactionName, traceCriticalPathPrefixes)) {
                return 1.0;
            }

            if (containsAnyConfiguredPrefix(transactionName, traceUserFacingPathPrefixes)) {
                return 0.5;
            }

            // Sample 10% of everything else
            return 0.1;
        };
    }

    @Bean
    public SentryOptions.BeforeSendTransactionCallback beforeSendTransactionCallback() {
        return (transaction, _) -> {
            // Add custom tags to transactions
            transaction.setTag("application", applicationName);
            transaction.setTag("version", applicationVersion);

            // Filter out health check transactions
            if (transaction.getTransaction() != null
                    && transaction.getTransaction().contains("/actuator/health")) {
                return null; // Don't send health check transactions
            }

            return transaction;
        };
    }

    private void sanitizePii(SentryEvent event) {
        if (event.getRequest() != null) {
            var request = event.getRequest();
            if (request.getHeaders() != null) {
                request.getHeaders()
                        .keySet()
                        .removeIf(header -> SENSITIVE_HEADER_NAMES.contains(header.toLowerCase(Locale.ROOT)));
            }
        }
    }

    private void sanitizeBreadcrumb(Breadcrumb breadcrumb) {
        breadcrumbDataKeys(breadcrumb).stream()
                .filter(key -> SENSITIVE_BREADCRUMB_KEYS.contains(key.toLowerCase(Locale.ROOT)))
                .toList()
                .forEach(breadcrumb::removeData);
    }

    private void addCustomTags(SentryEvent event) {
        event.setTag("application", applicationName);
        event.setTag("version", applicationVersion);
    }

    private static boolean containsAnyConfiguredPrefix(String value, String rawPrefixes) {
        if (value == null) {
            return false;
        }
        return configuredPrefixes(rawPrefixes).stream().anyMatch(value::startsWith);
    }

    private static Set<String> configuredPrefixes(String rawPrefixes) {
        if (rawPrefixes == null || rawPrefixes.isBlank()) {
            return Set.of();
        }
        return Stream.of(rawPrefixes.split(","))
                .map(String::trim)
                .filter(prefix -> !prefix.isBlank())
                .collect(Collectors.toUnmodifiableSet());
    }

    private static Set<String> breadcrumbDataKeys(Breadcrumb breadcrumb) {
        try {
            Object data = Breadcrumb.class.getMethod("getData").invoke(breadcrumb);
            return data instanceof Map<?, ?> map
                    ? map.keySet().stream()
                            .filter(String.class::isInstance)
                            .map(String.class::cast)
                            .collect(Collectors.toUnmodifiableSet())
                    : Set.of();
        } catch (ReflectiveOperationException exception) {
            return Set.of();
        }
    }
}
