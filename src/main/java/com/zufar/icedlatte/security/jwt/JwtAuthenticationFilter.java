package com.zufar.icedlatte.security.jwt;

import com.zufar.icedlatte.security.api.SecurityPrincipalProvider;
import com.zufar.icedlatte.security.configuration.SecurityConstants;
import com.zufar.icedlatte.security.exception.AbsentBearerHeaderException;
import com.zufar.icedlatte.security.exception.JwtTokenBlacklistedException;
import com.zufar.icedlatte.security.exception.JwtTokenHasNoUserEmailException;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpMethod;
import org.springframework.lang.NonNull;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.stream.Stream;


@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String MDC_USER_ID_KEY2VALUE = "user.id.key2value";

    private final JwtAuthenticationProvider jwtAuthenticationProvider;
    private final SecurityPrincipalProvider securityPrincipalProvider;

    @Override
    protected void doFilterInternal(@NonNull final HttpServletRequest httpRequest,
                                    @NonNull final HttpServletResponse httpResponse,
                                    @NonNull final FilterChain filterChain) throws IOException {
        try {
            if (shouldNotFilter(httpRequest)) {
                return;
            }
            var authenticationToken = jwtAuthenticationProvider.get(httpRequest);

            SecurityContextHolder
                    .getContext()
                    .setAuthentication(authenticationToken);

            UUID userId = securityPrincipalProvider.getUserId();
            MDC.put(MDC_USER_ID_KEY2VALUE, "userId:" + userId.toString());

            filterChain.doFilter(httpRequest, httpResponse);

        } catch (JwtTokenBlacklistedException exception) {
            handleException(httpResponse, "JWT Token is blacklisted", exception, HttpServletResponse.SC_BAD_REQUEST);
        } catch (AbsentBearerHeaderException exception) {
            handleException(httpResponse, "Bearer authentication header is absent", exception, HttpServletResponse.SC_BAD_REQUEST);
        } catch (ExpiredJwtException exception) {
            handleException(httpResponse, "Jwt token is expired", exception, HttpServletResponse.SC_UNAUTHORIZED);
        } catch (JwtTokenHasNoUserEmailException exception) {
            handleException(httpResponse, "User email not found in jwtToken", exception, HttpServletResponse.SC_BAD_REQUEST);
        } catch (UsernameNotFoundException exception) {
            handleException(httpResponse, "User with the provided email does not exist", exception, HttpServletResponse.SC_NOT_FOUND);
        } catch (Exception exception) {
            handleException(httpResponse, "Internal server error", exception, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } finally {
            MDC.remove(MDC_USER_ID_KEY2VALUE);
        }
    }

    private void handleException(HttpServletResponse httpResponse,
                                 String errorMessage,
                                 Exception exception,
                                 int statusCode) throws IOException {
        log.error(errorMessage, exception);
        httpResponse.setStatus(statusCode);
        httpResponse.getWriter().write("{ \"message\": \"" + errorMessage + "\" }");
    }

    @Override
    protected boolean shouldNotFilter(@NotNull HttpServletRequest request) {
        return !isSecuredUrl(request);
    }

    private boolean isSecuredUrl(HttpServletRequest request) {
        if (new AntPathRequestMatcher(SecurityConstants.REVIEWS_URL).matches(request)){
            return !HttpMethod.GET.name().equals(request.getMethod());
        }
        return Stream.of(SecurityConstants.SHOPPING_CART_URL, SecurityConstants.PAYMENT_URL,
                        SecurityConstants.USERS_URL, SecurityConstants.FAVOURITES_URL,
                        SecurityConstants.AUTH_URL, SecurityConstants.ORDERS_URL,
                        SecurityConstants.PRODUCT_URL)
                .anyMatch(securedUrl -> new AntPathRequestMatcher(securedUrl).matches(request));
    }
}
