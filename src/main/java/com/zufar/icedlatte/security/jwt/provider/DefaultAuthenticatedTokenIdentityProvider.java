package com.zufar.icedlatte.security.jwt.provider;

import com.zufar.icedlatte.ratelimit.api.AuthenticatedRequestIdentityProvider;
import com.zufar.icedlatte.security.jwt.blacklist.JwtTokenBlacklist;
import com.zufar.icedlatte.security.jwt.resolver.JwtBearerTokenResolver;
import com.zufar.icedlatte.security.jwt.resolver.JwtTokenClaims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DefaultAuthenticatedTokenIdentityProvider implements AuthenticatedRequestIdentityProvider {

    private final JwtBearerTokenResolver jwtBearerTokenResolver;
    private final JwtTokenClaims jwtTokenClaims;
    private final JwtTokenBlacklist jwtTokenBlacklist;

    @Override
    public Optional<String> findIdentity(HttpServletRequest request) {
        try {
            String token = jwtBearerTokenResolver.extract(request);
            jwtTokenBlacklist.validateNotBlacklisted(token);
            return Optional.of(jwtTokenClaims.extractAccessTokenEmail(token));
        } catch (Exception _) {
            return Optional.empty();
        }
    }
}
