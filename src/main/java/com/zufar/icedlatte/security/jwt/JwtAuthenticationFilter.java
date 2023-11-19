package com.zufar.icedlatte.security.jwt;

import com.zufar.icedlatte.security.api.SecurityPrincipalProvider;
import com.zufar.icedlatte.security.exception.AbsentBearerHeaderException;
import com.zufar.icedlatte.security.exception.JwtTokenBlacklistedException;
import com.zufar.icedlatte.security.exception.JwtTokenHasNoUserEmailException;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.lang.NonNull;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.stream.Stream;


@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String REGISTRATION_URL = "/api/v1/auth/register";
    private static final String OPEN_API_URL = "api/docs/schema";
    private static final String SWAGGER_API_URL = "api/docs/swagger-ui";
    private static final String AUTHENTICATION_URL = "/api/v1/auth/authenticate";
    private static final String PRODUCTS_API_URL = "/api/v1/products/";
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
            handleException(httpResponse, "JWT Token is blacklisted", exception);
        } catch (AbsentBearerHeaderException exception) {
            handleException(httpResponse, "Bearer authentication header is absent", exception);
        } catch (ExpiredJwtException exception) {
            handleException(httpResponse, "Jwt token is expired", exception);
        } catch (JwtTokenHasNoUserEmailException exception) {
            handleException(httpResponse, "User email not found in jwtToken", exception);
        } catch (UsernameNotFoundException exception) {
            handleException(httpResponse, "User with the provided email does not exist", exception);
        } catch (Exception exception) {
            handleException(httpResponse, "Internal server error", exception);
        }
        finally {
            MDC.remove(MDC_USER_ID_KEY2VALUE);
        }
    }

    private void handleException(HttpServletResponse httpResponse, String errorMessage, Exception exception) throws IOException {
        log.error(errorMessage, exception);
        httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        httpResponse.getWriter().write("{ \"message\": \"" + errorMessage + "\" }");
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return Stream.of(REGISTRATION_URL, OPEN_API_URL, SWAGGER_API_URL, AUTHENTICATION_URL, PRODUCTS_API_URL)
                .anyMatch(urlPath -> request.getServletPath().contains(urlPath) || urlPath.contains(request.getServletPath()));
    }
}
