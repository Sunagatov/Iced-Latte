package com.zufar.icedlatte.security.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class ActuatorPrometheusScrapeTokenFilter extends OncePerRequestFilter {

    private static final String PROMETHEUS_PATH = "/actuator/prometheus";
    private static final String AUTHORIZATION_PREFIX = "Metrics ";
    private static final SimpleGrantedAuthority ADMIN_AUTHORITY = new SimpleGrantedAuthority("ROLE_ADMIN");

    private final String scrapeToken;

    public ActuatorPrometheusScrapeTokenFilter(@Value("${management.prometheus.scrape-token:}") String scrapeToken) {
        this.scrapeToken = scrapeToken;
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        return !PROMETHEUS_PATH.equals(requestPath(request)) || !StringUtils.hasText(scrapeToken);
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String authorization = request.getHeader("Authorization");
            if (isValidScrapeAuthorization(authorization)) {
                var authentication =
                        new UsernamePasswordAuthenticationToken("prometheus-scraper", null, List.of(ADMIN_AUTHORITY));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }

            filterChain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private boolean isValidScrapeAuthorization(String authorization) {
        if (!StringUtils.hasText(authorization) || !authorization.startsWith(AUTHORIZATION_PREFIX)) {
            return false;
        }
        String suppliedToken =
                authorization.substring(AUTHORIZATION_PREFIX.length()).trim();
        return constantTimeEquals(suppliedToken, scrapeToken);
    }

    private boolean constantTimeEquals(String suppliedToken, String expectedToken) {
        byte[] suppliedBytes = suppliedToken.getBytes(StandardCharsets.UTF_8);
        byte[] expectedBytes = expectedToken.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(suppliedBytes, expectedBytes);
    }

    private String requestPath(HttpServletRequest request) {
        if (StringUtils.hasText(request.getServletPath())) {
            return request.getServletPath();
        }
        String requestUri = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (StringUtils.hasText(contextPath) && requestUri.startsWith(contextPath)) {
            return requestUri.substring(contextPath.length());
        }
        return requestUri;
    }
}
