package com.zufar.icedlatte.common.retry;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import lombok.experimental.UtilityClass;

@UtilityClass
public class RetryAttemptSupport {

    private static final int MAX_ERROR_LENGTH = 1000;
    private static final int MAX_BACKOFF_EXPONENT = 8;
    private static final long MAX_BACKOFF_SECONDS = 300L;

    public static Instant nextAttemptAt(int attemptCount) {
        return Instant.now().plus(backoffSeconds(attemptCount), ChronoUnit.SECONDS);
    }

    public static long backoffSeconds(int attemptCount) {
        if (attemptCount <= 0) {
            return 1L;
        }
        long backoff = attemptCount > MAX_BACKOFF_EXPONENT ? MAX_BACKOFF_SECONDS : 1L << attemptCount;
        return Math.min(backoff, MAX_BACKOFF_SECONDS);
    }

    public static String sanitizedError(Throwable failure) {
        String failureMessage = failure.getMessage();
        String message = failure.getClass().getSimpleName() + (failureMessage == null ? "" : ": " + failureMessage);
        message = message.replaceAll("[\\r\\n]", "_");
        return message.length() <= MAX_ERROR_LENGTH ? message : message.substring(0, MAX_ERROR_LENGTH);
    }
}
