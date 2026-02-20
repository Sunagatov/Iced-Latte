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
        log.debug("Authentication successful, authorities: {}",
                event.getAuthentication().getAuthorities());
    }

    @EventListener
    public void onAuthenticationFailure(AbstractAuthenticationFailureEvent event) {
        log.warn("Authentication failed - {}",
                event.getException().getClass().getSimpleName());
    }

    @EventListener
    public void onAuthorizationDenied(AuthorizationDeniedEvent<?> event) {
        var authSupplier = event.getAuthentication();
        if (authSupplier != null && authSupplier.get() != null) {
            log.warn("Authorization denied, authorities: {}",
                    authSupplier.get().getAuthorities());
        } else {
            log.warn("Authorization denied for anonymous user");
        }
    }
}