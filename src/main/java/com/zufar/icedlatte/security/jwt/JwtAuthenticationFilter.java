package com.zufar.icedlatte.security.jwt;

import com.zufar.icedlatte.common.correlation.RequestContextConstants;
import com.zufar.icedlatte.common.util.ClientIpExtractor;
import com.zufar.icedlatte.security.configuration.AuthPaths;
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
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final JwtAuthenticationProvider jwtAuthenticationProvider;
    private final SecurityPrincipalProvider securityPrincipalProvider;
    private final JwtClaimExtractor jwtClaimExtractor;
    private final JwtTokenFromAuthHeaderExtractor jwtTokenFromAuthHeaderExtractor;
    private final ClientIpExtractor clientIpExtractor;

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String uri = request.getRequestURI();
        return AuthPaths.REFRESH.equals(uri)
                || AuthPaths.GOOGLE.equals(uri)
                || AuthPaths.GOOGLE_CALLBACK.equals(uri);
    }

    @Override
    protected void doFilterInternal(@NonNull final HttpServletRequest httpRequest,
                                    @NonNull final HttpServletResponse httpResponse,
                                    @NonNull final FilterChain filterChain) throws IOException, ServletException {

        try {
            var authenticationToken = jwtAuthenticationProvider.get(httpRequest);
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            MDC.put(RequestContextConstants.USER_ID_MDC_KEY, securityPrincipalProvider.getUserId().toString());
            try {
                String rawToken = jwtTokenFromAuthHeaderExtractor.extract(httpRequest);
                jwtClaimExtractor.extractSessionId(rawToken)
                        .ifPresent(sid -> MDC.put(RequestContextConstants.SESSION_ID_MDC_KEY, sid.toString()));
            } catch (Exception ignored) {
                // sid is best-effort; never block the request
            }
        } catch (AbsentBearerHeaderException ex) {
            // No token present — continue as anonymous, let Spring Security authorization decide
        } catch (Exception ex) {
            handleAuthenticationException(httpRequest, httpResponse, ex);
            return;
        }

        try {
            filterChain.doFilter(httpRequest, httpResponse);
        } finally {
            MDC.remove(RequestContextConstants.USER_ID_MDC_KEY);
            MDC.remove(RequestContextConstants.SESSION_ID_MDC_KEY);
        }
    }
// amazonq-ignore-next-line

    private void handleAuthenticationException(HttpServletRequest httpRequest,
                                               HttpServletResponse httpResponse,
                                               Exception exception) throws IOException {
        String requestId = MDC.get(RequestContextConstants.REQUEST_ID_MDC_KEY);
        String method = httpRequest.getMethod();
        String path = httpRequest.getRequestURI();
        String clientIp = clientIpExtractor.extract(httpRequest);

        // amazonq-ignore-next-line
        var errorInfo = switch (exception) {
            case InvalidCredentialsException _ -> new ErrorInfo("invalid-credentials", "Authentication failed", "Authentication failed.", HttpServletResponse.SC_UNAUTHORIZED, "INVALID_CREDENTIALS");
            case JwtTokenBlacklistedException _ -> new ErrorInfo("session-expired", "Session expired", "Session expired. Please sign in again.", HttpServletResponse.SC_UNAUTHORIZED, "TOKEN_REVOKED");
            case ExpiredJwtException _ -> new ErrorInfo("session-expired", "Session expired", "Authentication token has expired.", HttpServletResponse.SC_UNAUTHORIZED, "TOKEN_EXPIRED");
            case JwtTokenHasNoUserEmailException _ -> new ErrorInfo("auth-failed", "Authentication failed", "Authentication failed.", HttpServletResponse.SC_UNAUTHORIZED, "TOKEN_INVALID_FORMAT");
            case UsernameNotFoundException _ -> new ErrorInfo("auth-failed", "Authentication failed", "Authentication failed.", HttpServletResponse.SC_UNAUTHORIZED, "USER_NOT_FOUND");
            default -> new ErrorInfo("internal-error", "Authentication error", "An internal server error occurred.", HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "AUTH_INTERNAL_ERROR");
        };

        if (errorInfo.statusCode() >= 500) {
            log.error("auth.error: reason_code={}, method={}, path={}, client_ip={}, status={}, request_id={}",
                    errorInfo.reasonCode(), method, path, clientIp, errorInfo.statusCode(), requestId, exception);
        } else {
            log.warn("auth.failed: reason_code={}, method={}, path={}, client_ip={}, status={}, request_id={}",
                    errorInfo.reasonCode(), method, path, clientIp, errorInfo.statusCode(), requestId);
            log.debug("auth.failed.details", exception);
        }
        
        httpResponse.setStatus(errorInfo.statusCode());
        httpResponse.setContentType(MediaType.APPLICATION_JSON_VALUE);
        httpResponse.setCharacterEncoding("UTF-8");
        httpResponse.setHeader("X-Content-Type-Options", "nosniff");
        httpResponse.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        httpResponse.setHeader("Pragma", "no-cache");
        httpResponse.setHeader("Expires", "0");
        ObjectNode json = OBJECT_MAPPER.createObjectNode()
                .put("type", "https://iced-latte.uk/errors/" + errorInfo.typeSlug())
                .put("title", errorInfo.title())
                .put("status", errorInfo.statusCode())
                .put("detail", errorInfo.detail())
                .put("instance", path)
                .put("timestamp", java.time.Instant.now().toString())
                .put("requestId", requestId);
        byte[] responseBytes = OBJECT_MAPPER.writeValueAsBytes(json);
        httpResponse.setContentLength(responseBytes.length);
        httpResponse.getOutputStream().write(responseBytes);
    }
    
    // Record for error information - Java 21 feature
    private record ErrorInfo(String typeSlug, String title, String detail, int statusCode, String reasonCode) {}

}
