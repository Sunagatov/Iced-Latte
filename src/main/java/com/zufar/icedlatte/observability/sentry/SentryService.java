package com.zufar.icedlatte.observability.sentry;

import io.sentry.Breadcrumb;
import io.sentry.Sentry;
import io.sentry.SentryLevel;
import io.sentry.protocol.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@ConditionalOnProperty(name = "sentry.enabled", havingValue = "true")
public class SentryService {

    public void setUser(String userId) {
        User user = new User();
        user.setId(userId);
        Sentry.setUser(user);
    }

    public void clearUser() {
        Sentry.setUser(null);
    }

    public void addBreadcrumb(String category, String message, Map<String, String> data) {
        Breadcrumb breadcrumb = new Breadcrumb();
        breadcrumb.setCategory(category);
        breadcrumb.setMessage(message);
        breadcrumb.setLevel(SentryLevel.INFO);
        if (data != null) {
            data.forEach(breadcrumb::setData);
        }
        Sentry.addBreadcrumb(breadcrumb);
    }

    public void captureException(Throwable throwable) {
        Sentry.captureException(throwable);
    }

    public void captureMessage(String message, SentryLevel level) {
        Sentry.captureMessage(message, level);
    }
}
