package com.zufar.icedlatte.security.jwt;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtAuthenticationProvider {

    private final JwtBearerTokenResolver jwtBearerTokenResolver;
    private final JwtTokenClaims jwtTokenClaims;
    private final UserDetailsService userDetailsService;
    private final JwtTokenBlacklist jwtTokenBlacklist;

    public Authentication get(final HttpServletRequest httpRequest) {
        String jwtToken = jwtBearerTokenResolver.extract(httpRequest);

        jwtTokenBlacklist.validateNotBlacklisted(jwtToken);

        String userEmail = jwtTokenClaims.extractAccessTokenEmail(jwtToken);
        UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);

        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );
        authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(httpRequest));

        log.debug("auth.success");
        return authenticationToken;
    }
}
