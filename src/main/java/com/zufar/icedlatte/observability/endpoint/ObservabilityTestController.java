package com.zufar.icedlatte.observability.endpoint;

import com.zufar.icedlatte.observability.sentry.SentryService;
import io.sentry.SentryLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/observability/test")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "sentry.enabled", havingValue = "true")
public class ObservabilityTestController {

    private final SentryService sentryService;

    @GetMapping("/sentry/error")
    public void testSentryError() {
        log.info("observability.test.sentry_error: triggering test exception");
        sentryService.addBreadcrumb("test", "User triggered test error", Map.of("endpoint", "/sentry/error"));
        throw new RuntimeException("Test error for Sentry — this is intentional");
    }

    @GetMapping("/sentry/message")
    public ResponseEntity<String> testSentryMessage(@RequestParam(defaultValue = "INFO") String level) {
        log.info("observability.test.sentry_message: level={}", level);
        
        SentryLevel sentryLevel = switch (level.toUpperCase()) {
            case "DEBUG" -> SentryLevel.DEBUG;
            case "INFO" -> SentryLevel.INFO;
            case "WARNING" -> SentryLevel.WARNING;
            case "ERROR" -> SentryLevel.ERROR;
            case "FATAL" -> SentryLevel.FATAL;
            default -> SentryLevel.INFO;
        };
        
        sentryService.captureMessage("Test message from Iced Latte", sentryLevel);
        return ResponseEntity.ok("Message sent to Sentry with level: " + level);
    }

    @GetMapping("/sentry/breadcrumbs")
    public ResponseEntity<String> testSentryBreadcrumbs() {
        log.info("observability.test.sentry_breadcrumbs: adding breadcrumbs");
        
        sentryService.addBreadcrumb("cart", "User viewed cart", Map.of("itemCount", "3"));
        sentryService.addBreadcrumb("product", "User viewed product", Map.of("productId", "123"));
        sentryService.addBreadcrumb("order", "User started checkout", Map.of("cartTotal", "99.99"));
        
        throw new RuntimeException("Test error with breadcrumbs trail");
    }

    @GetMapping("/sentry/nested-error")
    public void testSentryNestedError() {
        log.info("observability.test.sentry_nested_error: triggering nested exception");
        try {
            causeNullPointerException();
        } catch (NullPointerException e) {
            throw new IllegalStateException("Wrapped exception for Sentry", e);
        }
    }

    private void causeNullPointerException() {
        String nullString = null;
        nullString.length();
    }
}
