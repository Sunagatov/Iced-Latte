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
        log.debug("auth.success: authorities={}",
                event.getAuthentication().getAuthorities());
    }

    @EventListener
    public void onAuthenticationFailure(AbstractAuthenticationFailureEvent event) {
        log.warn("auth.failed: reason={}",
                event.getException().getClass().getSimpleName());
    }

    @EventListener
    public void onAuthorizationDenied(AuthorizationDeniedEvent<?> event) {
        var authSupplier = event.getAuthentication();
        if (authSupplier != null && authSupplier.get() != null) {
            log.warn("auth.denied: authorities={}",
                    authSupplier.get().getAuthorities());
        } else {
            log.warn("auth.denied: reason=anonymous");
        }
    }
}