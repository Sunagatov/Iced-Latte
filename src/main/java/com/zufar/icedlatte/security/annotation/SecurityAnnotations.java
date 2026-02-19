package com.zufar.icedlatte.security.annotation;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom security annotations for method-level security.
 * Demonstrates the use of Spring Security 6.x method-level security features.
 */
public class SecurityAnnotations {

    /**
     * Annotation for admin-only operations.
     */
    @Target({ElementType.METHOD, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @PreAuthorize("hasRole('ADMIN')")
    public @interface AdminOnly {
    }

    /**
     * Annotation for user operations that require authentication.
     */
    @Target({ElementType.METHOD, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @PreAuthorize("isAuthenticated()")
    public @interface AuthenticatedOnly {
    }

    /**
     * Annotation for operations that can be performed by the user themselves or admins.
     */
    @Target({ElementType.METHOD, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @PreAuthorize("hasRole('ADMIN') or authentication.name == #userId")
    public @interface UserOrAdmin {
    }

    /**
     * Annotation for operations that require specific permissions.
     */
    @Target({ElementType.METHOD, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @PreAuthorize("hasAuthority('READ_PRODUCTS')")
    public @interface CanReadProducts {
    }

    /**
     * Annotation for operations that require write permissions.
     */
    @Target({ElementType.METHOD, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @PreAuthorize("hasAuthority('WRITE_PRODUCTS')")
    public @interface CanWriteProducts {
    }

    /**
     * Annotation for operations that require payment permissions.
     */
    @Target({ElementType.METHOD, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @PreAuthorize("hasAuthority('PROCESS_PAYMENTS')")
    public @interface CanProcessPayments {
    }
}
