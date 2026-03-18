package com.zufar.icedlatte.observability.sentry;

import io.sentry.Breadcrumb;
import io.sentry.SentryEvent;
import io.sentry.SentryOptions;
import io.sentry.protocol.TransactionNameSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@ConditionalOnProperty(name = "sentry.enabled", havingValue = "true")
public class SentryConfiguration {

    @Value("${spring.application.name}")
    private String applicationName;

    @Value("${spring.application.version:unknown}")
    private String applicationVersion;

    @Bean
    public SentryOptions.BeforeSendCallback beforeSendCallback() {
        return (event, hint) -> {
            sanitizePii(event);
            addCustomTags(event);
            return event;
        };
    }

    @Bean
    public SentryOptions.BeforeBreadcrumbCallback beforeBreadcrumbCallback() {
        return (breadcrumb, hint) -> {
            sanitizeBreadcrumb(breadcrumb);
            return breadcrumb;
        };
    }

    @Bean
    public SentryOptions.TracesSamplerCallback tracesSamplerCallback() {
        return samplingContext -> {
            var transactionContext = samplingContext.getTransactionContext();
            var transactionName = transactionContext.getName();
            
            // Sample 100% of critical endpoints
            if (transactionName.contains("/api/v1/auth/") || 
                transactionName.contains("/api/v1/payment/") ||
                transactionName.contains("/api/v1/orders/")) {
                return 1.0;
            }
            
            // Sample 50% of user-facing endpoints
            if (transactionName.contains("/api/v1/products/") ||
                transactionName.contains("/api/v1/cart/")) {
                return 0.5;
            }
            
            // Sample 10% of everything else
            return 0.1;
        };
    }

    @Bean
    public SentryOptions.BeforeSendTransactionCallback beforeSendTransactionCallback() {
        return (transaction, hint) -> {
            // Add custom tags to transactions
            transaction.setTag("application", applicationName);
            transaction.setTag("version", applicationVersion);
            
            // Filter out health check transactions
            if (transaction.getTransaction() != null && 
                transaction.getTransaction().contains("/actuator/health")) {
                return null; // Don't send health check transactions
            }
            
            return transaction;
        };
    }

    private void sanitizePii(SentryEvent event) {
        if (event.getRequest() != null) {
            var request = event.getRequest();
            if (request.getHeaders() != null) {
                request.getHeaders().remove("Authorization");
                request.getHeaders().remove("Cookie");
            }
            if (request.getData() != null) {
                var data = request.getData();
                if (data instanceof String dataStr) {
                    dataStr = dataStr.replaceAll("\"email\"\\s*:\\s*\"[^\"]+\"", "\"email\":\"<redacted>\"");
                    dataStr = dataStr.replaceAll("\"password\"\\s*:\\s*\"[^\"]+\"", "\"password\":\"<redacted>\"");
                    dataStr = dataStr.replaceAll("\"phone\"\\s*:\\s*\"[^\"]+\"", "\"phone\":\"<redacted>\"");
                }
            }
        }
    }

    private void sanitizeBreadcrumb(Breadcrumb breadcrumb) {
        if (breadcrumb.getData() != null) {
            breadcrumb.getData().remove("email");
            breadcrumb.getData().remove("password");
            breadcrumb.getData().remove("phone");
        }
    }

    private void addCustomTags(SentryEvent event) {
        event.setTag("application", applicationName);
        event.setTag("version", applicationVersion);
    }
}
