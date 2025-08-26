package com.zufar.icedlatte.security.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authorization.event.AuthorizationDeniedEvent;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SecurityEventListener {

    @EventListener
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        String username = event.getAuthentication().getName();
        var authorities = event.getAuthentication().getAuthorities();
        log.info("Authentication successful for user: {} with authorities: {}", username, authorities);
    }

    @EventListener
    public void onAuthenticationFailure(AbstractAuthenticationFailureEvent event) {
        String username = event.getAuthentication().getName();
        String reason = event.getException().getMessage();
        var exceptionType = event.getException().getClass().getSimpleName();
        log.warn("Authentication failed for user: {} - Type: {} - Reason: {}", username, exceptionType, reason);
    }

    @EventListener
    public void onAuthorizationDenied(AuthorizationDeniedEvent<?> event) {
        if (event.getAuthentication().get() != null) {
            String username = event.getAuthentication().get().getName();
            log.warn("Authorization denied for user: {} - Resource: {}",
                    username,
                    event.getAuthorizationResult());
        } else {
            log.warn("Authorization denied for anonymous user - Resource: {}",
                    event.getAuthorizationResult());
        }
    }
}