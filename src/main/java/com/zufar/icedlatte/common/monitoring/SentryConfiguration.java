package com.zufar.icedlatte.common.monitoring;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;

import com.zufar.icedlatte.common.http.RequestPathUtils;

import io.sentry.Breadcrumb;
import io.sentry.SentryEvent;
import io.sentry.SentryOptions;
import io.sentry.protocol.Message;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@ConditionalOnProperty(name = "sentry.enabled", havingValue = "true")
public class SentryConfiguration {

    private static final Set<String> SENSITIVE_HEADER_NAMES =
            Set.of(HttpHeaders.AUTHORIZATION.toLowerCase(Locale.ROOT), HttpHeaders.COOKIE.toLowerCase(Locale.ROOT));
    private static final Set<String> SENSITIVE_BREADCRUMB_KEYS = Set.of("email", "password", "phone");
    private static final Pattern CLIENT_IP_FIELD_PATTERN = Pattern.compile("(client_ip=)([^,\\s]+)");
    private static final String HTTP_ACCESS_LOGGER = "http.access";
    private static final String DEFAULT_FINGERPRINT_MARKER = "{{ default }}";

    @Value("${spring.application.name}")
    private String applicationName;

    @Value("${spring.application.version:unknown}")
    private String applicationVersion;

    @Value("${sentry.trace-critical-path-prefixes:}")
    private String traceCriticalPathPrefixes;

    @Value("${sentry.trace-user-facing-path-prefixes:}")
    private String traceUserFacingPathPrefixes;

    @Value("${sentry.trace-critical-sample-rate:1.0}")
    private double traceCriticalSampleRate;

    @Value("${sentry.trace-user-facing-sample-rate:0.5}")
    private double traceUserFacingSampleRate;

    @Value("${sentry.trace-default-sample-rate:0.1}")
    private double traceDefaultSampleRate;

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
            normalizeFingerprint(event);
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
                return traceCriticalSampleRate;
            }

            if (containsAnyConfiguredPrefix(transactionName, traceUserFacingPathPrefixes)) {
                return traceUserFacingSampleRate;
            }

            return traceDefaultSampleRate;
        };
    }

    @Bean
    public SentryOptions.BeforeSendTransactionCallback beforeSendTransactionCallback() {
        return (transaction, _) -> {
            // Add custom tags to transactions
            transaction.setTag("application", applicationName);
            transaction.setTag("version", applicationVersion);

            // Filter out health check transactions
            if (isHealthCheckTransaction(transaction.getTransaction())) {
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
        sanitizeMessage(event.getMessage());
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

    private static void sanitizeMessage(Message message) {
        if (message == null) {
            return;
        }
        if (message.getMessage() != null) {
            message.setMessage(redactClientIp(message.getMessage()));
        }
        if (message.getFormatted() != null) {
            message.setFormatted(redactClientIp(message.getFormatted()));
        }
        if (message.getParams() != null && !message.getParams().isEmpty()) {
            message.setParams(message.getParams().stream()
                    .map(SentryConfiguration::redactClientIp)
                    .toList());
        }
    }

    private static String redactClientIp(String value) {
        return value == null ? null : CLIENT_IP_FIELD_PATTERN.matcher(value).replaceAll("$1[redacted]");
    }

    private static void normalizeFingerprint(SentryEvent event) {
        if (!HTTP_ACCESS_LOGGER.equals(event.getLogger())) {
            return;
        }
        String formattedMessage =
                event.getMessage() != null ? event.getMessage().getFormatted() : null;
        String method = defaultIfBlank(extractStructuredLogValue(formattedMessage, "method"), "unknown");
        String path = defaultIfBlank(extractStructuredLogValue(formattedMessage, "path"), extractRequestPath(event));
        String statusFamily =
                toStatusFamily(defaultIfBlank(extractStructuredLogValue(formattedMessage, "status"), "unknown"));
        event.setFingerprints(List.of(HTTP_ACCESS_LOGGER, method, path, statusFamily, DEFAULT_FINGERPRINT_MARKER));
    }

    private static String extractStructuredLogValue(String formattedMessage, String key) {
        if (formattedMessage == null || formattedMessage.isBlank()) {
            return null;
        }
        String keyPrefix = key + "=";
        int start = formattedMessage.indexOf(keyPrefix);
        if (start < 0) {
            return null;
        }
        int valueStart = start + keyPrefix.length();
        int valueEnd = formattedMessage.indexOf(',', valueStart);
        String rawValue = valueEnd >= 0
                ? formattedMessage.substring(valueStart, valueEnd)
                : formattedMessage.substring(valueStart);
        String normalized = rawValue.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private static String extractRequestPath(SentryEvent event) {
        if (event.getRequest() == null || event.getRequest().getUrl() == null) {
            return "unknown";
        }
        try {
            URI requestUri = new URI(event.getRequest().getUrl());
            String path = requestUri.getPath();
            return path == null || path.isBlank() ? "unknown" : path;
        } catch (URISyntaxException _) {
            return event.getRequest().getUrl();
        }
    }

    private static String toStatusFamily(String rawStatus) {
        try {
            int status = Integer.parseInt(rawStatus);
            return (status / 100) + "xx";
        } catch (NumberFormatException _) {
            return rawStatus;
        }
    }

    private static String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static boolean containsAnyConfiguredPrefix(String value, String rawPrefixes) {
        if (value == null) {
            return false;
        }
        return configuredPrefixes(rawPrefixes).stream()
                .anyMatch(prefix -> RequestPathUtils.matchesRootOrNested(value, prefix));
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

    private static boolean isHealthCheckTransaction(String transactionName) {
        if (transactionName == null) {
            return false;
        }
        int pathStart = transactionName.indexOf('/');
        String path = pathStart >= 0 ? transactionName.substring(pathStart) : transactionName;
        return RequestPathUtils.matchesRootOrNested(path, "/actuator/health");
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
