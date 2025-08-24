package com.zufar.icedlatte.common.audit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    public void auditMethod(JoinPoint joinPoint, Auditable auditable, Object result) {
        try {
            String entityType = auditable.entityType();
            String entityId = extractEntityId(result, auditable);
            
            auditService.logOperation(entityType, entityId, auditable.operation(), null, result);
        } catch (Exception e) {
            log.error("Failed to audit method: {}", joinPoint.getSignature().getName(), e);
        }
    }

    private String extractEntityId(Object result, Auditable auditable) {
        if (result == null) {
            return "unknown";
        }
        
        try {
            var field = result.getClass().getDeclaredField("id");
            field.setAccessible(true);
            Object id = field.get(result);
            return id != null ? id.toString() : "unknown";
        } catch (Exception e) {
            return "unknown";
        }
    }
}