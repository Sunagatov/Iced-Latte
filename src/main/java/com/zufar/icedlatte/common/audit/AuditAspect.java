package com.zufar.icedlatte.common.audit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.lang.reflect.Field;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditService auditService;

    @AfterReturning(pointcut = "@annotation(auditable)", returning = "result")
    @SuppressWarnings("unused") // invoked by AOP proxy at runtime
    void auditMethod(JoinPoint joinPoint, Auditable auditable, Object result) {
        try {
            String entityType = auditable.entityType();
            String entityId = extractEntityId(result);
            auditService.logOperation(entityType, entityId, auditable.operation(), null, result);
        } catch (org.springframework.dao.DataAccessException e) {
            log.error("audit.error.db: method={}, message={}", joinPoint.getSignature().getName(), e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            log.error("audit.error.invalid_arg: method={}, message={}", joinPoint.getSignature().getName(), e.getMessage(), e);
        }
    }

    private String extractEntityId(Object result) {
        if (result == null) {
            return "unknown";
        }
        try {
            Field field = result.getClass().getDeclaredField("id");
            field.setAccessible(true);
            Object id = field.get(result);
            return id != null ? id.toString() : "unknown";
        } catch (NoSuchFieldException | IllegalAccessException e) {
            return result.toString();
        }
    }
}