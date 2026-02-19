package com.zufar.icedlatte.common.correlation;

import lombok.extern.slf4j.Slf4j;

/**
 * Utility class for correlation ID operations.
 * Provides convenient methods for logging with correlation context.
 */
@Slf4j
public final class CorrelationUtils {
    
    private CorrelationUtils() {}
    
    /**
     * Log info message with correlation ID prefix
     */
    public static void logInfo(String message, Object... args) {
        String correlationId = CorrelationContext.get();
        if (correlationId != null) {
            log.info("[{}] " + message, prependCorrelationId(correlationId, args));
        } else {
            log.info(message, args);
        }
    }
    
    /**
     * Log error message with correlation ID prefix
     */
    public static void logError(String message, Object... args) {
        String correlationId = CorrelationContext.get();
        if (correlationId != null) {
            log.error("[{}] " + message, prependCorrelationId(correlationId, args));
        } else {
            log.error(message, args);
        }
    }
    
    /**
     * Log warn message with correlation ID prefix
     */
    public static void logWarn(String message, Object... args) {
        String correlationId = CorrelationContext.get();
        if (correlationId != null) {
            log.warn("[{}] " + message, prependCorrelationId(correlationId, args));
        } else {
            log.warn(message, args);
        }
    }
    
    private static Object[] prependCorrelationId(String correlationId, Object[] args) {
        Object[] newArgs = new Object[args.length + 1];
        newArgs[0] = correlationId;
        System.arraycopy(args, 0, newArgs, 1, args.length);
        return newArgs;
    }
}