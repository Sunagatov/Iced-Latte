package com.zufar.onlinestore.security.configuration;

import com.zufar.onlinestore.security.api.SecurityPrincipalProvider;
import com.zufar.onlinestore.security.jwt.filter.JwtAuthenticationProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Enriches MDC with user.userId.
 */
@Order
@Component
@AllArgsConstructor
public class UserIdMdcFilter extends OncePerRequestFilter {

    private final SecurityPrincipalProvider securityPrincipalProvider;
    private final JwtAuthenticationProvider jwtAuthenticationProvider;

    private static final String MDC_USER_ID_KEY2VALUE = "user.id.key2value";

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        var isUserPresent = jwtAuthenticationProvider.get(request).isPresent();
        if (isUserPresent) {
            var userId = securityPrincipalProvider.getUserId();
            MDC.put(MDC_USER_ID_KEY2VALUE, "USERID:" + userId.toString());
        } else {
            MDC.remove(MDC_USER_ID_KEY2VALUE);
        }
        filterChain.doFilter(request, response);
    }
}
