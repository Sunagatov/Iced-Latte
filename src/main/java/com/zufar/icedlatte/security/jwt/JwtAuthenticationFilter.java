package com.zufar.icedlatte.security.jwt;

import com.zufar.icedlatte.security.api.SecurityPrincipalProvider;
import com.zufar.icedlatte.security.configuration.SecurityConstants;
import com.zufar.icedlatte.security.exception.AbsentBearerHeaderException;
import com.zufar.icedlatte.security.exception.JwtTokenBlacklistedException;
import com.zufar.icedlatte.security.exception.JwtTokenHasNoUserEmailException;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringEscapeUtils;
import org.slf4j.MDC;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;


@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String MDC_USER_ID_KEY = "userId";
    private static final String MDC_REQUEST_ID_KEY = "requestId";
    
    private static final Set<String> SECURED_URLS = Set.of(
            SecurityConstants.SHOPPING_CART_URL,
            SecurityConstants.PAYMENT_URL,
            SecurityConstants.USERS_URL,
            SecurityConstants.FAVOURITES_URL,
            SecurityConstants.AUTH_URL,
            SecurityConstants.ORDERS_URL,
            SecurityConstants.SHIPPING_URL,
            SecurityConstants.REVIEWS_URL,
            SecurityConstants.REVIEW_URL
    );

    private final JwtAuthenticationProvider jwtAuthenticationProvider;
    private final SecurityPrincipalProvider securityPrincipalProvider;

    @Override
    protected void doFilterInternal(@NonNull final HttpServletRequest httpRequest,
                                    @NonNull final HttpServletResponse httpResponse,
                                    @NonNull final FilterChain filterChain) throws IOException {
        
        String requestId = UUID.randomUUID().toString();
        MDC.put(MDC_REQUEST_ID_KEY, requestId);
        
        try {
            if (shouldNotFilter(httpRequest)) {
                filterChain.doFilter(httpRequest, httpResponse);
                return;
            }
            
            var authenticationToken = jwtAuthenticationProvider.get(httpRequest);
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);

            UUID userId = securityPrincipalProvider.getUserId();
            MDC.put(MDC_USER_ID_KEY, userId.toString());

            filterChain.doFilter(httpRequest, httpResponse);

        } catch (Exception ex) {
            handleAuthenticationException(httpResponse, ex);
        } finally {
            MDC.remove(MDC_USER_ID_KEY);
            MDC.remove(MDC_REQUEST_ID_KEY);
        }
    }

    private void handleAuthenticationException(HttpServletResponse httpResponse, Exception exception) throws IOException {
        String requestId = MDC.get(MDC_REQUEST_ID_KEY);
        
        // Using Java 21 pattern matching for switch expressions
        var errorInfo = switch (exception) {
            case JwtTokenBlacklistedException ignored -> new ErrorInfo("Authentication failed: token revoked", HttpServletResponse.SC_UNAUTHORIZED);
            case AbsentBearerHeaderException ignored -> new ErrorInfo("Authentication failed: invalid authorization header", HttpServletResponse.SC_UNAUTHORIZED);
            case ExpiredJwtException ignored -> new ErrorInfo("Authentication failed: token expired", HttpServletResponse.SC_UNAUTHORIZED);
            case JwtTokenHasNoUserEmailException ignored -> new ErrorInfo("Authentication failed: invalid token format", HttpServletResponse.SC_UNAUTHORIZED);
            case UsernameNotFoundException ignored -> new ErrorInfo("Authentication failed: user not found", HttpServletResponse.SC_UNAUTHORIZED);
            case ServletException ignored -> new ErrorInfo("Authentication failed: internal error", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            default -> new ErrorInfo("Authentication failed: internal error", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        };
        
        if (errorInfo.statusCode() >= 500) {
            log.error("Authentication error: {} - Request ID: {}", 
                    StringEscapeUtils.escapeJava(errorInfo.message()), requestId, exception);
        } else {
            log.warn("Authentication failed: {} - Request ID: {}", 
                    StringEscapeUtils.escapeJava(errorInfo.message()), requestId);
            log.debug("Authentication failure details", exception);
        }
        
        httpResponse.setStatus(errorInfo.statusCode());
        httpResponse.setContentType(MediaType.APPLICATION_JSON_VALUE);
        httpResponse.setCharacterEncoding("UTF-8");
        httpResponse.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        httpResponse.setHeader("Pragma", "no-cache");
        httpResponse.setHeader("Expires", "0");
        httpResponse.setHeader("X-Request-ID", requestId);
        
        String jsonResponse = String.format("""
            {
                "error": "%s",
                "message": "%s",
                "timestamp": "%s",
                "status": %d,
                "requestId": "%s"
            }
            """, 
            StringEscapeUtils.escapeJson("Unauthorized"), 
            StringEscapeUtils.escapeJson(errorInfo.message()),
            java.time.Instant.now(), 
            errorInfo.statusCode(),
            StringEscapeUtils.escapeJson(requestId));
        
        httpResponse.getWriter().write(jsonResponse);
    }
    
    // Record for error information - Java 21 feature
    private record ErrorInfo(String message, int statusCode) {}

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        return !isSecuredUrl(request);
    }

    private boolean isSecuredUrl(HttpServletRequest request) {
        if (isUnauthorizedGetReviewsUrl(request) || isUnauthorizedPostStripeWebhookUrl(request)) {
            return false;
        }
        
        return SECURED_URLS.stream()
                .anyMatch(securedUrl -> PathPatternRequestMatcher.withDefaults().matcher(securedUrl).matches(request));
    }

    private boolean isUnauthorizedGetReviewsUrl(HttpServletRequest request) {
        boolean isReviewsUrl = SecurityConstants.ALLOWED_PRODUCT_REVIEWS_URLS.stream()
                .anyMatch(securedUrl -> PathPatternRequestMatcher.withDefaults().matcher(securedUrl).matches(request));

        return isReviewsUrl && HttpMethod.GET.name().equals(request.getMethod());
    }

    private boolean isUnauthorizedPostStripeWebhookUrl(HttpServletRequest request) {
        boolean isStripeWebhookUrl = PathPatternRequestMatcher.withDefaults().matcher(SecurityConstants.STRIPE_WEBHOOK_URL)
                .matches(request);

        return isStripeWebhookUrl && HttpMethod.POST.name().equals(request.getMethod());
    }
}
