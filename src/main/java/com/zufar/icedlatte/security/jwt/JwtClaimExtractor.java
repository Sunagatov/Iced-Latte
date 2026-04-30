package com.zufar.icedlatte.security.jwt;

import com.zufar.icedlatte.security.exception.JwtTokenHasNoUserEmailException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Optional;
import java.util.UUID;

@Service
public class JwtClaimExtractor {

    private final JwtParser jwtParser;

    public JwtClaimExtractor(JwtSignKeyProvider jwtSignKeyProvider) {
        this.jwtParser = Jwts.parser()
                .verifyWith(jwtSignKeyProvider.get())
                .build();
    }

    public String extractEmail(final String jwtToken) {
        try {
            return Optional.ofNullable(extractAllClaims(jwtToken).getSubject())
                    .filter(StringUtils::hasText)
                    .orElseThrow(() -> new JwtTokenHasNoUserEmailException("Missing email in JWT token"));
        } catch (JwtTokenHasNoUserEmailException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new JwtTokenHasNoUserEmailException("Failed to extract email from JWT token", ex);
        }
    }

    public Optional<UUID> extractSessionId(final String jwtToken) {
        try {
            String sid = (String) extractAllClaims(jwtToken).get(JwtClaimNames.SESSION_ID);
            return StringUtils.hasText(sid) ? Optional.of(UUID.fromString(sid)) : Optional.empty();
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    private Claims extractAllClaims(final String jwtToken) {
        return jwtParser.parseSignedClaims(jwtToken).getPayload();
    }
}
