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
        log.info("Authentication successful for user: {} with authorities: {}",
                event.getAuthentication().getName(),
                event.getAuthentication().getAuthorities());
    }

    @EventListener
    public void onAuthenticationFailure(AbstractAuthenticationFailureEvent event) {
        log.warn("Authentication failed for user: {} - {}: {}",
                event.getAuthentication().getName(),
                event.getException().getClass().getSimpleName(),
                event.getException().getMessage());
    }

    @EventListener
    public void onAuthorizationDenied(AuthorizationDeniedEvent<?> event) {
        var authSupplier = event.getAuthentication();
        if (authSupplier != null && authSupplier.get() != null) {
            var auth = authSupplier.get();
            log.warn("Authorization denied for user: {} - authorities: {}",
                    auth.getName(), auth.getAuthorities());
        } else {
            log.warn("Authorization denied for anonymous user");
        }
    }
}