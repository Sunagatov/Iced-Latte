package com.zufar.icedlatte.security.jwt;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtAuthenticationProvider {

    private final JwtTokenFromAuthHeaderExtractor jwtTokenFromAuthHeaderExtractor;
    private final JwtClaimExtractor jwtClaimExtractor;
    private final UserDetailsService userDetailsService;
    private final JwtBlacklistValidator jwtBlacklistValidator;

    public UsernamePasswordAuthenticationToken get(final HttpServletRequest httpRequest) {
        String jwtToken = jwtTokenFromAuthHeaderExtractor.extract(httpRequest);

        jwtBlacklistValidator.validate(jwtToken);

        jwtClaimExtractor.extractExpiration(jwtToken);

        final String userEmail = jwtClaimExtractor.extractEmail(jwtToken);

        UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

        var authenticationToken = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        authenticationToken
                .setDetails(new WebAuthenticationDetailsSource().buildDetails(httpRequest));

        return authenticationToken;
    }
}

