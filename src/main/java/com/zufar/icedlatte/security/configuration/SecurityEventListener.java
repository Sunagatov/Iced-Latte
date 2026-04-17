package com.zufar.icedlatte.security.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authorization.event.AuthorizationDeniedEvent;
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
    @SuppressWarnings("unused")
    public void onAuthorizationDenied(AuthorizationDeniedEvent<?> event) {
        var attrs = RequestContextHolder.getRequestAttributes();
        String method = "-";
        String path = "-";
        if (attrs instanceof ServletRequestAttributes sra) {
            method = sra.getRequest().getMethod();
            path = sanitize(normalizePath(sra.getRequest().getRequestURI()));
        }

        String principal = event.getAuthentication().get().getName();

        if (isExpectedAnonymousDeny(principal, path)) {
            log.debug("auth.denied: method={}, path={}, principal={}", method, path, principal);
        } else {
            log.warn("auth.denied: method={}, path={}, principal={}", method, path, principal);
        }
    }

    private static boolean isExpectedAnonymousDeny(String principal, String path) {
        return "anonymousUser".equals(principal)
                && ("/api/v1/users".equals(path)
                || "/api/v1/cart".equals(path)
                || "/api/v1/favorites".equals(path));
    }

    private static String normalizePath(String value) {
        if (value == null || value.isBlank()) {
            return "/";
        }
        return value.startsWith("/") ? value : "/" + value;
    }

    private static String sanitize(String value) {
        return value == null ? "" : value.replaceAll("[\\r\\n]", "_");
    }
}