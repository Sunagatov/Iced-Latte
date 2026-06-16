package com.zufar.icedlatte.security.jwt.filter;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.jspecify.annotations.NonNull;
import org.slf4j.MDC;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.zufar.icedlatte.common.correlation.RequestContextConstants;
import com.zufar.icedlatte.common.http.ApiPaths;
import com.zufar.icedlatte.common.util.ClientIpExtractor;
import com.zufar.icedlatte.security.api.CurrentUserProvider;
import com.zufar.icedlatte.security.config.SecurityProblemResponseWriter;
import com.zufar.icedlatte.security.jwt.exception.JwtTokenException;
import com.zufar.icedlatte.security.jwt.provider.JwtAuthenticationProvider;
import com.zufar.icedlatte.security.jwt.resolver.JwtBearerTokenResolver;
import com.zufar.icedlatte.security.jwt.resolver.JwtTokenClaims;
import com.zufar.icedlatte.security.signin.exception.AbsentBearerHeaderException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtAuthenticationProvider jwtAuthenticationProvider;
    private final CurrentUserProvider currentUserProvider;
    private final JwtTokenClaims jwtTokenClaims;
    private final JwtBearerTokenResolver jwtBearerTokenResolver;
    private final ClientIpExtractor clientIpExtractor;
    private final SecurityProblemResponseWriter problemResponseWriter;
    private final JwtAuthenticationFailureMapper failureMapper;

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String uri = request.getRequestURI();
        return ApiPaths.AUTH_REFRESH.equals(uri) || uri.startsWith(ApiPaths.AUTH_OAUTH + "/");
    }

    @Override
    protected void doFilterInternal(
            @NonNull final HttpServletRequest httpRequest,
            @NonNull final HttpServletResponse httpResponse,
            @NonNull final FilterChain filterChain)
            throws IOException, ServletException {
        try {
            var authenticationToken = jwtAuthenticationProvider.get(httpRequest);
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            MDC.put(
                    RequestContextConstants.USER_ID_MDC_KEY,
                    currentUserProvider.getUserId().toString());
            try {
                String rawToken = jwtBearerTokenResolver.extract(httpRequest);
                jwtTokenClaims
                        .extractAccessTokenSessionId(rawToken)
                        .ifPresent(sid -> MDC.put(RequestContextConstants.SESSION_ID_MDC_KEY, sid.toString()));
            } catch (AbsentBearerHeaderException | JwtTokenException _) {
                // sid is best-effort for expected token parsing failures.
            }
        } catch (AbsentBearerHeaderException _) {
            // No token present — continue as anonymous, let Spring Security authorization decide
        } catch (Exception ex) {
            clearAuthState();
            handleAuthenticationException(httpRequest, httpResponse, ex);
            return;
        }

        try {
            filterChain.doFilter(httpRequest, httpResponse);
        } finally {
            clearAuthState();
        }
    }

    private void handleAuthenticationException(
            HttpServletRequest httpRequest, HttpServletResponse httpResponse, Exception exception) throws IOException {
        String requestId = MDC.get(RequestContextConstants.REQUEST_ID_MDC_KEY);
        String method = httpRequest.getMethod();
        String path = httpRequest.getRequestURI();
        String clientIp = clientIpExtractor.extract(httpRequest);

        JwtAuthenticationFailure failure = failureMapper.map(exception);

        if (failure.statusCode() >= 500) {
            String logMessage =
                    "auth.error: reason_code={}, method={}, path={}, client_ip={}, status={}, request_id={}";
            log.error(
                    logMessage,
                    failure.reasonCode(),
                    method,
                    path,
                    clientIp,
                    failure.statusCode(),
                    requestId,
                    exception);
        } else {
            String logMessage =
                    "auth.failed: reason_code={}, method={}, path={}, client_ip={}, status={}, request_id={}";
            log.warn(logMessage, failure.reasonCode(), method, path, clientIp, failure.statusCode(), requestId);
            log.debug("auth.failed.details", exception);
        }

        problemResponseWriter.write(
                httpResponse,
                failure.statusCode(),
                failure.typeSlug(),
                failure.title(),
                failure.detail(),
                path,
                requestId);
    }

    private void clearAuthState() {
        SecurityContextHolder.clearContext();
        MDC.remove(RequestContextConstants.USER_ID_MDC_KEY);
        MDC.remove(RequestContextConstants.SESSION_ID_MDC_KEY);
    }
}
