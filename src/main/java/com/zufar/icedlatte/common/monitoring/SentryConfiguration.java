package com.zufar.icedlatte.common.monitoring;

import com.zufar.icedlatte.common.http.ApiPaths;
import com.zufar.icedlatte.security.configuration.AuthPaths;
import io.sentry.Breadcrumb;
import io.sentry.SentryEvent;
import io.sentry.SentryOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;

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
        return (event, _) -> {
            // Only send server errors (5xx) to Sentry. 4xx are client errors — expected, not bugs.
            if (event.getLevel() != null && event.getLevel() != io.sentry.SentryLevel.ERROR
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
            
            // Sample 100% of critical endpoints
            if (transactionName.contains(AuthPaths.ROOT_PREFIX) ||
                transactionName.contains(ApiPaths.PAYMENT + "/") ||
                transactionName.contains(ApiPaths.ORDERS + "/")) {
                return 1.0;
            }
            
            // Sample 50% of user-facing endpoints
            if (transactionName.contains(ApiPaths.PRODUCTS + "/") ||
                transactionName.contains(ApiPaths.CART + "/")) {
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
                request.getHeaders().remove(HttpHeaders.AUTHORIZATION);
                request.getHeaders().remove(HttpHeaders.COOKIE);
            }
        }
    }

    private void sanitizeBreadcrumb(Breadcrumb breadcrumb) {
        breadcrumb.removeData("email");
        breadcrumb.removeData("password");
        breadcrumb.removeData("phone");
    }

    private void addCustomTags(SentryEvent event) {
        event.setTag("application", applicationName);
        event.setTag("version", applicationVersion);
    }
}
