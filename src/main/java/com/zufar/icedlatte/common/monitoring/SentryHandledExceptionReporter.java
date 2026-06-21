package com.zufar.icedlatte.common.monitoring;

import java.util.function.Consumer;

import org.springframework.stereotype.Component;

import io.sentry.IScope;
import io.sentry.Sentry;
import io.sentry.SentryLevel;

@Component
public class SentryHandledExceptionReporter {

    public void capture(Throwable throwable, Consumer<IScope> scopeCustomizer) {
        Sentry.withScope(scope -> {
            scope.setLevel(SentryLevel.ERROR);
            scope.setTag("handled", "true");
            if (scopeCustomizer != null) {
                scopeCustomizer.accept(scope);
            }
            Sentry.captureException(throwable);
        });
    }
}
