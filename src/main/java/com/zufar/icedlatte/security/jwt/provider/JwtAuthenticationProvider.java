package com.zufar.icedlatte.security.jwt.provider;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Service;

import com.zufar.icedlatte.common.audit.Identifiable;
import com.zufar.icedlatte.security.jwt.blacklist.JwtTokenBlacklist;
import com.zufar.icedlatte.security.jwt.exception.JwtTokenException;
import com.zufar.icedlatte.security.jwt.resolver.JwtBearerTokenResolver;
import com.zufar.icedlatte.security.jwt.resolver.JwtTokenClaims;
import com.zufar.icedlatte.security.session.management.AuthSessionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtAuthenticationProvider {

    private final JwtBearerTokenResolver jwtBearerTokenResolver;
    private final JwtTokenClaims jwtTokenClaims;
    private final UserDetailsService userDetailsService;
    private final JwtTokenBlacklist jwtTokenBlacklist;
    private final AuthSessionService authSessionService;

    public Authentication get(final HttpServletRequest httpRequest) {
        String jwtToken = jwtBearerTokenResolver.extract(httpRequest);
        UsernamePasswordAuthenticationToken authenticationToken = authenticate(jwtToken);
        authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(httpRequest));
        return authenticationToken;
    }

    public Authentication get(final String authorizationHeader) {
        String jwtToken = jwtBearerTokenResolver.extract(authorizationHeader);
        return authenticate(jwtToken);
    }

    private UsernamePasswordAuthenticationToken authenticate(String jwtToken) {
        jwtTokenBlacklist.validateNotBlacklisted(jwtToken);

        String userEmail = jwtTokenClaims.extractAccessTokenEmail(jwtToken);
        UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);
        JwtAccountStatusValidator.requireActive(userDetails);
        validateActiveSession(jwtToken, userDetails);

        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        log.debug("auth.success");
        return authenticationToken;
    }

    private void validateActiveSession(String jwtToken, UserDetails userDetails) {
        if (!(userDetails instanceof Identifiable user)) {
            throw new JwtTokenException("Authenticated principal does not expose a session owner id");
        }

        var sessionId = jwtTokenClaims
                .extractAccessTokenSessionId(jwtToken)
                .orElseThrow(() -> new JwtTokenException("Access token session is missing"));
        authSessionService.validateActiveSession(sessionId, user.getId());
    }
}
