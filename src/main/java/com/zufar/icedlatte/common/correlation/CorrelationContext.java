package com.zufar.icedlatte.common.correlation;

import java.util.UUID;
import java.util.concurrent.Callable;

/**
 * Correlation context using ThreadLocal for thread-safe correlation ID management.
 */
public final class CorrelationContext {
    
    private static final ThreadLocal<String> CORRELATION_ID = new ThreadLocal<>();
    
    private CorrelationContext() {}
    
    public static String get() {
        return CORRELATION_ID.get();
    }
    
    public static String getOrGenerate() {
        String correlationId = CORRELATION_ID.get();
        return correlationId != null ? correlationId : generateId();
    }
    
    public static <T> T runWithCorrelationId(String correlationId, Callable<T> task) throws Exception {
        String previousId = CORRELATION_ID.get();
        try {
            CORRELATION_ID.set(correlationId);
            return task.call();
        } finally {
            if (previousId != null) {
                CORRELATION_ID.set(previousId);
            } else {
                CORRELATION_ID.remove();
            }
        }
    }
    
    public static void runWithCorrelationId(String correlationId, Runnable task) {
        String previousId = CORRELATION_ID.get();
        try {
            CORRELATION_ID.set(correlationId);
            task.run();
        } finally {
            if (previousId != null) {
                CORRELATION_ID.set(previousId);
            } else {
                CORRELATION_ID.remove();
            }
        }
    }
    
    private static String generateId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}