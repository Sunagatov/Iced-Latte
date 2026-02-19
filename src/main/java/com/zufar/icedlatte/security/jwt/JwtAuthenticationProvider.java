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

    private final JwtTokenFromAuthHeaderExtractor jwtTokenFromAuthHeaderExtractor;
    private final JwtClaimExtractor jwtClaimExtractor;
    private final UserDetailsService userDetailsService;
    private final JwtBlacklistValidator jwtBlacklistValidator;

    public Authentication get(final HttpServletRequest httpRequest) {
        String jwtToken = jwtTokenFromAuthHeaderExtractor.extract(httpRequest);
        
        try {
            // Validate token is not blacklisted first (fastest check)
            jwtBlacklistValidator.validate(jwtToken);
            
            // Validate token expiration
            if (jwtClaimExtractor.isTokenExpired(jwtToken)) {
                log.debug("Authentication failed: JWT token has expired");
                throw new io.jsonwebtoken.ExpiredJwtException(null, null, "JWT token has expired");
            }
            
            // Extract user email from token
            String userEmail = jwtClaimExtractor.extractEmail(jwtToken);
            
            // Load user details
            UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);
            
            // Create authentication token with enhanced security context
            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                    userDetails, 
                    null, 
                    userDetails.getAuthorities()
            );
            
            // Set additional security details
            authenticationToken.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(httpRequest)
            );
            
            log.debug("Authentication successful");
            return authenticationToken;
            
        } catch (Exception ex) {
            log.debug("Authentication failed", ex);
            throw ex;
        }
    }
}

