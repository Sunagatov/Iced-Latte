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
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringEscapeUtils;
import org.slf4j.MDC;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
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

        } catch (JwtTokenBlacklistedException ex) {
            handleAuthenticationException(httpResponse, "JWT token is blacklisted", ex, HttpServletResponse.SC_UNAUTHORIZED);
        } catch (AbsentBearerHeaderException ex) {
            handleAuthenticationException(httpResponse, "Bearer authentication header is absent", ex, HttpServletResponse.SC_UNAUTHORIZED);
        } catch (ExpiredJwtException ex) {
            handleAuthenticationException(httpResponse, "JWT token has expired", ex, HttpServletResponse.SC_UNAUTHORIZED);
        } catch (JwtTokenHasNoUserEmailException ex) {
            handleAuthenticationException(httpResponse, "User email not found in JWT token", ex, HttpServletResponse.SC_BAD_REQUEST);
        } catch (UsernameNotFoundException ex) {
            handleAuthenticationException(httpResponse, "User with the provided email does not exist", ex, HttpServletResponse.SC_UNAUTHORIZED);
        } catch (ServletException | RuntimeException ex) {
            handleAuthenticationException(httpResponse, "Internal server error", ex, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } finally {
            MDC.remove(MDC_USER_ID_KEY);
            MDC.remove(MDC_REQUEST_ID_KEY);
        }
    }

    private void handleAuthenticationException(HttpServletResponse httpResponse,
                                             String errorMessage,
                                             Exception exception,
                                             int statusCode) throws IOException {
        log.warn("Authentication failed: {} - Request ID: {}", 
                StringEscapeUtils.escapeJava(errorMessage), 
                MDC.get(MDC_REQUEST_ID_KEY), 
                exception);
        
        httpResponse.setStatus(statusCode);
        httpResponse.setContentType(MediaType.APPLICATION_JSON_VALUE);
        httpResponse.setCharacterEncoding("UTF-8");
        
        String jsonResponse = STR."""
            {
                "error": "\{StringEscapeUtils.escapeJson(errorMessage)}",
                "timestamp": "\{java.time.Instant.now()}",
                "status": \{statusCode}
            }
            """;
        
        httpResponse.getWriter().write(jsonResponse);
    }

    @Override
    protected boolean shouldNotFilter(@NotNull HttpServletRequest request) {
        return !isSecuredUrl(request);
    }

    private boolean isSecuredUrl(HttpServletRequest request) {
        if (isUnauthorizedGetReviewsUrl(request) || isUnauthorizedPostStripeWebhookUrl(request)) {
            return false;
        }
        
        return SECURED_URLS.stream()
                .anyMatch(securedUrl -> new AntPathRequestMatcher(securedUrl).matches(request));
    }

    private boolean isUnauthorizedGetReviewsUrl(HttpServletRequest request) {
        boolean isReviewsUrl = SecurityConstants.ALLOWED_PRODUCT_REVIEWS_URLS.stream()
                .anyMatch(securedUrl -> new AntPathRequestMatcher(securedUrl).matches(request));

        return isReviewsUrl && HttpMethod.GET.name().equals(request.getMethod());
    }

    private boolean isUnauthorizedPostStripeWebhookUrl(HttpServletRequest request) {
        boolean isStripeWebhookUrl = new AntPathRequestMatcher(SecurityConstants.STRIPE_WEBHOOK_URL)
                .matches(request);

        return isStripeWebhookUrl && HttpMethod.POST.name().equals(request.getMethod());
    }
}
