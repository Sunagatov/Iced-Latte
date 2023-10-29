package com.zufar.onlinestore.security.jwt;

import com.zufar.onlinestore.security.exception.AbsentBearerHeaderException;
import com.zufar.onlinestore.security.exception.JwtTokenBlacklistedException;
import com.zufar.onlinestore.security.exception.JwtTokenHasNoUserEmailException;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.stream.Stream;


@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String REGISTRATION_URL = "/api/v1/auth/register";
    private static final String AUTHENTICATION_URL = "/api/v1/auth/authenticate";

    private final JwtTokenFromAuthHeaderExtractor jwtTokenFromAuthHeaderExtractor;
    private final JwtClaimExtractor jwtClaimExtractor;
    private final UserDetailsService userDetailsService;
    private final JwtBlacklistValidator jwtBlacklistValidator;

    @Override
    protected void doFilterInternal(@NonNull final HttpServletRequest httpRequest,
                                    @NonNull final HttpServletResponse httpResponse,
                                    @NonNull final FilterChain filterChain) throws ServletException, IOException {
        try {
            String jwtToken = jwtTokenFromAuthHeaderExtractor.extract(httpRequest);

            jwtBlacklistValidator.validate(jwtToken);

            jwtClaimExtractor.extractExpiration(jwtToken);

            final String userEmail = jwtClaimExtractor.extractEmail(jwtToken);

            UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

            var authenticationToken = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            authenticationToken
                    .setDetails(new WebAuthenticationDetailsSource().buildDetails(httpRequest));

            SecurityContextHolder
                    .getContext()
                    .setAuthentication(authenticationToken);

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
    }

    private void handleException(HttpServletResponse httpResponse, String errorMessage, Exception exception) throws IOException {
        log.error(errorMessage, exception);
        httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        httpResponse.getWriter().write("{ \"message\": \"" + errorMessage + "\" }");
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return Stream.of(REGISTRATION_URL, AUTHENTICATION_URL)
                .anyMatch(urlPath -> request.getServletPath().contains(urlPath));
    }
}
