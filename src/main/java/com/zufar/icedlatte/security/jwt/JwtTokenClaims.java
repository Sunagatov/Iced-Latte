package com.zufar.icedlatte.security.jwt;

import com.zufar.icedlatte.security.exception.JwtTokenHasNoUserEmailException;
import com.zufar.icedlatte.security.jwt.support.JwtClaimNames;
import com.zufar.icedlatte.security.jwt.support.JwtSigningKeys;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Optional;
import java.util.UUID;

@Service
public class JwtTokenClaims {

    private final JwtParser accessTokenParser;
    private final JwtParser refreshTokenParser;

    public JwtTokenClaims(JwtSigningKeys jwtSigningKeys) {
        this.accessTokenParser = Jwts.parser()
                .verifyWith(jwtSigningKeys.get())
                .build();
        this.refreshTokenParser = Jwts.parser()
                .verifyWith(jwtSigningKeys.getRefresh())
                .build();
    }

    public String extractAccessTokenEmail(final String token) {
        try {
            return extractEmail(accessTokenClaims(token), "Missing email in JWT token");
        } catch (JwtTokenHasNoUserEmailException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new JwtTokenHasNoUserEmailException("Failed to extract email from JWT token", ex);
        }
    }

    public Optional<UUID> extractAccessTokenSessionId(final String token) {
        try {
            return extractSessionId(accessTokenClaims(token));
        } catch (Exception _) {
            return Optional.empty();
        }
    }

    public String extractRefreshTokenEmail(final String token) {
        try {
            return extractEmail(refreshTokenClaims(token), "Refresh token has no subject");
        } catch (JwtTokenHasNoUserEmailException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new JwtTokenHasNoUserEmailException("Invalid refresh token", ex);
        }
    }

    public boolean isSessionManagedRefreshToken(final String token) {
        try {
            return refreshTokenClaims(token).get(JwtClaimNames.VERSION) != null;
        } catch (Exception _) {
            return false;
        }
    }

    public Optional<UUID> extractRefreshTokenSessionId(final String token) {
        try {
            return extractSessionId(refreshTokenClaims(token));
        } catch (Exception _) {
            return Optional.empty();
        }
    }

    private String extractEmail(Claims claims, String missingEmailMessage) {
        return Optional.ofNullable(claims.getSubject())
                .filter(StringUtils::hasText)
                .orElseThrow(() -> new JwtTokenHasNoUserEmailException(missingEmailMessage));
    }

    private Optional<UUID> extractSessionId(Claims claims) {
        String sessionId = (String) claims.get(JwtClaimNames.SESSION_ID);
        return StringUtils.hasText(sessionId)
                ? Optional.of(UUID.fromString(sessionId))
                : Optional.empty();
    }

    private Claims accessTokenClaims(final String token) {
        return accessTokenParser.parseSignedClaims(token).getPayload();
    }

    private Claims refreshTokenClaims(final String token) {
        return refreshTokenParser.parseSignedClaims(token).getPayload();
    }
}
