package com.zufar.icedlatte.security.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authorization.event.AuthorizationDeniedEvent;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Enhanced security event listener with monitoring and alerting capabilities.
 * Uses Java 21 features for improved performance and security monitoring.
 */
@Slf4j
@Component
public class SecurityEventListener {

    // Track failed authentication attempts per IP
    private final ConcurrentHashMap<String, FailedAttempts> failedAttempts = new ConcurrentHashMap<>();
    
    // Track successful authentications for monitoring
    private final AtomicInteger successfulAuthentications = new AtomicInteger(0);
    private final AtomicInteger failedAuthentications = new AtomicInteger(0);

    @EventListener
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        String username = event.getAuthentication().getName();
        var authorities = event.getAuthentication().getAuthorities();
        
        // Increment success counter
        int successCount = successfulAuthentications.incrementAndGet();
        
        // Log with enhanced information
        log.info("Authentication successful for user: {} with authorities: {} (Total successes: {})", 
                username, authorities, successCount);
        
        // Clear any failed attempts for this user
        clearFailedAttempts(username);
    }

    @EventListener
    public void onAuthenticationFailure(AbstractAuthenticationFailureEvent event) {
        String username = event.getAuthentication().getName();
        String reason = event.getException().getMessage();
        var exceptionType = event.getException().getClass().getSimpleName();
        
        // Increment failure counter
        int failureCount = failedAuthentications.incrementAndGet();
        
        // Track failed attempts
        trackFailedAttempt(username, exceptionType);
        
        // Log with enhanced information
        log.warn("Authentication failed for user: {} - Type: {} - Reason: {} (Total failures: {})", 
                username, exceptionType, reason, failureCount);
        
        // Check for potential brute force attacks
        checkForBruteForceAttack(username);
    }

    @EventListener
    public void onAuthorizationDenied(AuthorizationDeniedEvent<?> event) {
        try {
            var auth = event.getAuthentication().get();
            String username = auth.getName();
            log.warn("Authorization denied for user: {} - Resource: {} - Authorities: {}",
                    username,
                    event.getAuthorizationResult(),
                    auth.getAuthorities());
        } catch (Exception e) {
            log.warn("Authorization denied for anonymous user - Resource: {}",
                    event.getAuthorizationResult());
        }
    }
    
    private void trackFailedAttempt(String username, String exceptionType) {
        failedAttempts.compute(username, (key, existing) -> {
            if (existing == null) {
                return new FailedAttempts(1, Instant.now(), exceptionType);
            }
            return existing.increment();
        });
    }
    
    private void clearFailedAttempts(String username) {
        failedAttempts.remove(username);
    }
    
    private void checkForBruteForceAttack(String username) {
        FailedAttempts attempts = failedAttempts.get(username);
        if (attempts != null && attempts.count() >= 5) {
            log.error("Potential brute force attack detected for user: {} - {} failed attempts in {} seconds", 
                    username, attempts.count(), 
                    java.time.Duration.between(attempts.firstAttempt(), Instant.now()).toSeconds());
            
            // In a real application, you might want to:
            // 1. Lock the account temporarily
            // 2. Send an alert to administrators
            // 3. Block the IP address
            // 4. Log to a security monitoring system
        }
    }
    
    // Record for tracking failed attempts - Java 21 feature
    private record FailedAttempts(int count, Instant firstAttempt, String lastExceptionType) {
        public FailedAttempts increment() {
            return new FailedAttempts(count + 1, firstAttempt, lastExceptionType);
        }
    }
}