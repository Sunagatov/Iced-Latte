package com.zufar.icedlatte.common.correlation;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * Aspect to automatically log correlation ID for service and repository methods.
 * Uses Java 21 pattern matching for cleaner code.
 */
@Slf4j
@Aspect
@Component
public class CorrelationAspect {
    
//    @Around("execution(* com.zufar.icedlatte..api..*(..))")
//    public Object logServiceMethods(ProceedingJoinPoint joinPoint) throws Throwable {
//        return logWithCorrelation(joinPoint, "SERVICE");
//    }
//
//    @Around("execution(* com.zufar.icedlatte..repository..*(..))")
//    public Object logRepositoryMethods(ProceedingJoinPoint joinPoint) throws Throwable {
//        return logWithCorrelation(joinPoint, "REPOSITORY");
//    }
//
//    @Around("execution(* com.zufar.icedlatte..endpoint..*(..))")
//    public Object logControllerMethods(ProceedingJoinPoint joinPoint) throws Throwable {
//        return logWithCorrelation(joinPoint, "CONTROLLER");
//    }
//
//    private Object logWithCorrelation(ProceedingJoinPoint joinPoint, String layer) throws Throwable {
//        String correlationId = CorrelationContext.getOrGenerate();
//        String methodName = joinPoint.getSignature().toShortString();
//
//        log.info("[{}] [{}] Starting: {}", correlationId, layer, methodName);
//
//        try {
//            Object result = joinPoint.proceed();
//            log.info("[{}] [{}] Completed: {}", correlationId, layer, methodName);
//            return result;
//        } catch (Exception e) {
//            log.error("[{}] [{}] Failed: {} - {}", correlationId, layer, methodName, e.getMessage());
//            throw e;
//        }
//    }
}