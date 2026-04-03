package com.zufar.icedlatte.security.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
@Component
public class SecurityEventListener {

    @EventListener
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        log.debug("auth.success: authType={}",
                event.getAuthentication().getClass().getSimpleName());
    }

    @EventListener
    public void onAuthenticationFailure(AbstractAuthenticationFailureEvent event) {
        log.warn("auth.failed: reason={}",
                event.getException().getClass().getSimpleName());
    }

    @EventListener
    public void onAuthorizationDenied() {
        var attrs = RequestContextHolder.getRequestAttributes();
        String method = "-";
        String path = "-";
        if (attrs instanceof ServletRequestAttributes sra) {
            method = sra.getRequest().getMethod();
            path = sra.getRequest().getRequestURI();
        }
        log.warn("auth.denied: method={}, path={}", method, path);
    }
}