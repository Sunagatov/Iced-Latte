package com.zufar.icedlatte.common.monitoring;

import com.zufar.icedlatte.security.api.SecurityPrincipalProvider;
import io.sentry.Sentry;
import io.sentry.protocol.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@Order(3)
@RequiredArgsConstructor
@ConditionalOnProperty(name = "sentry.enabled", havingValue = "true")
public class SentryUserContextFilter extends OncePerRequestFilter {

    private final SecurityPrincipalProvider securityPrincipalProvider;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        try {
            var userId = securityPrincipalProvider.getUserId();
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
}
