package com.zufar.icedlatte.common.audit;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Example service showing how to use audit capabilities
 */
@Service
@RequiredArgsConstructor
public class AuditExample {

    private final AuditService auditService;

    // Example 1: Using @Auditable annotation
    @Auditable(entityType = "Product", operation = AuditOperation.CREATE)
    public Object createProduct(Object product) {
        // Business logic here
        return product;
    }

    // Example 2: Manual audit logging
    public void updateProductPrice(String productId, Object oldProduct, Object newProduct) {
        // Business logic here
        
        // Manual audit logging
        auditService.logOperation("Product", productId, AuditOperation.UPDATE, oldProduct, newProduct);
    }

    // Example 3: Security event logging
    public void logSecurityEvent(String userId, AuditOperation operation) {
        auditService.logOperation("Security", userId, operation, null, null);
    }
}