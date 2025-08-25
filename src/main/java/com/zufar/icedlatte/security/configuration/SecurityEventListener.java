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
        log.info("Authentication successful for user: {}", username);
    }

    @EventListener
    public void onAuthenticationFailure(AbstractAuthenticationFailureEvent event) {
        String username = event.getAuthentication().getName();
        String reason = event.getException().getMessage();
        log.warn("Authentication failed for user: {} - Reason: {}", username, reason);
    }

    @EventListener
    public void onAuthorizationDenied(AuthorizationDeniedEvent event) {
        String username = event.getAuthentication().get().getName();
        log.warn("Authorization denied for user: {} - Resource: {}", 
                username, 
                event.getAuthorizationDecision());
    }
}