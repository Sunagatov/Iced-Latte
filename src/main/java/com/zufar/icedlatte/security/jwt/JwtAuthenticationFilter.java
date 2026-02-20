package com.zufar.icedlatte.security.jwt;

import com.zufar.icedlatte.security.api.SecurityPrincipalProvider;
import com.zufar.icedlatte.security.exception.AbsentBearerHeaderException;
import com.zufar.icedlatte.security.exception.InvalidCredentialsException;
import com.zufar.icedlatte.security.exception.JwtTokenBlacklistedException;
import com.zufar.icedlatte.security.exception.JwtTokenHasNoUserEmailException;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;


@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String MDC_USER_ID_KEY = "userId";
    private static final String MDC_REQUEST_ID_KEY = "requestId";
    
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final JwtAuthenticationProvider jwtAuthenticationProvider;
    private final SecurityPrincipalProvider securityPrincipalProvider;

    @Override
    protected void doFilterInternal(@NonNull final HttpServletRequest httpRequest,
                                    @NonNull final HttpServletResponse httpResponse,
                                    @NonNull final FilterChain filterChain) throws IOException, ServletException {

        String requestId = UUID.randomUUID().toString();
        MDC.put(MDC_REQUEST_ID_KEY, requestId);

        try {
            var authenticationToken = jwtAuthenticationProvider.get(httpRequest);
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);

            UUID userId = securityPrincipalProvider.getUserId();
            MDC.put(MDC_USER_ID_KEY, userId.toString());

        } catch (AbsentBearerHeaderException ex) {
            // No token present — continue as anonymous, let Spring Security authorization decide
        } catch (Exception ex) {
            handleAuthenticationException(httpResponse, ex);
            return;
        } finally {
            MDC.remove(MDC_USER_ID_KEY);
            MDC.remove(MDC_REQUEST_ID_KEY);
        }

        filterChain.doFilter(httpRequest, httpResponse);
    }

    private void handleAuthenticationException(HttpServletResponse httpResponse, Exception exception) throws IOException {
        String requestId = MDC.get(MDC_REQUEST_ID_KEY);
        
        // Using Java 21 pattern matching for switch expressions
        var errorInfo = switch (exception) {
            case InvalidCredentialsException ignored -> new ErrorInfo("Authentication failed: invalid credentials", HttpServletResponse.SC_UNAUTHORIZED);
            case JwtTokenBlacklistedException ignored -> new ErrorInfo("Authentication failed: token revoked", HttpServletResponse.SC_UNAUTHORIZED);
            case ExpiredJwtException ignored -> new ErrorInfo("Authentication failed: token expired", HttpServletResponse.SC_UNAUTHORIZED);
            case JwtTokenHasNoUserEmailException ignored -> new ErrorInfo("Authentication failed: invalid token format", HttpServletResponse.SC_UNAUTHORIZED);
            case UsernameNotFoundException ignored -> new ErrorInfo("Authentication failed: user not found", HttpServletResponse.SC_UNAUTHORIZED);
            default -> new ErrorInfo("Authentication failed: internal error", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        };
        
        if (errorInfo.statusCode() >= 500) {
            log.error("Authentication error: {} - Request ID: {}", errorInfo.message(), requestId, exception);
        } else {
            log.warn("Authentication failed: {} - Request ID: {}", errorInfo.message(), requestId);
            log.debug("Authentication failure details", exception);
        }
        
        httpResponse.setStatus(errorInfo.statusCode());
        httpResponse.setContentType(MediaType.APPLICATION_JSON_VALUE);
        httpResponse.setCharacterEncoding("UTF-8");
        httpResponse.setHeader("X-Content-Type-Options", "nosniff");
        httpResponse.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        httpResponse.setHeader("Pragma", "no-cache");
        httpResponse.setHeader("Expires", "0");
        httpResponse.setHeader("X-Request-ID", requestId);
        
        ObjectNode json = OBJECT_MAPPER.createObjectNode()
                .put("error", "Unauthorized")
                .put("message", errorInfo.message())
                .put("timestamp", java.time.Instant.now().toString())
                .put("status", errorInfo.statusCode())
                .put("requestId", requestId);
        byte[] responseBytes = OBJECT_MAPPER.writeValueAsBytes(json);
        httpResponse.setContentLength(responseBytes.length);
        httpResponse.getOutputStream().write(responseBytes);
    }
    
    // Record for error information - Java 21 feature
    private record ErrorInfo(String message, int statusCode) {}

}
