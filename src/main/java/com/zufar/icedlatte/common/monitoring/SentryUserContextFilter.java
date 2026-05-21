package com.zufar.icedlatte.common.monitoring;

import com.zufar.icedlatte.common.audit.Identifiable;
import io.sentry.Sentry;
import io.sentry.protocol.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Component
@Order(3)
@ConditionalOnProperty(name = "sentry.enabled", havingValue = "true")
public class SentryUserContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        try {
            var userId = currentUserId();
            if (userId != null) {
                User user = new User();
                user.setId(userId.toString());
                Sentry.setUser(user);
            }
        } catch (Exception e) {
            log.debug("sentry.user_context.failed: {}", e.getMessage());
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            Sentry.setUser(null);
        }
    }

    private UUID currentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Identifiable principal)) {
            return null;
        }
        return principal.getId();
    }
}
