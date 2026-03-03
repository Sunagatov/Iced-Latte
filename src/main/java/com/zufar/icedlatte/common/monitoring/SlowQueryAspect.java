package com.zufar.icedlatte.common.monitoring;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class SlowQueryAspect {

    @Value("${monitoring.slow-query-threshold-ms:200}")
    private long thresholdMs;

    @Around("within(@org.springframework.stereotype.Repository *)")
    @SuppressWarnings("unused") // invoked by AOP proxy at runtime
    public Object track(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.currentTimeMillis();
        try {
            return pjp.proceed();
        } finally {
            long elapsed = System.currentTimeMillis() - start;
            if (elapsed >= thresholdMs) {
                log.warn("db.slow_query: method={}, durationMs={}",
                        pjp.getSignature().toShortString(), elapsed);
            }
        }
    }
}
