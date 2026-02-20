package com.zufar.icedlatte.security.jwt;

import com.zufar.icedlatte.security.exception.JwtTokenHasNoUserEmailException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Service
public class JwtClaimExtractor {

    private final JwtParser jwtParser;

    public JwtClaimExtractor(JwtSignKeyProvider jwtSignKeyProvider) {
        this.jwtParser = Jwts.parser().verifyWith(jwtSignKeyProvider.get()).build();
    }

    public String extractEmail(final String jwtToken) {
        try {
            return Optional.ofNullable(extractAllClaims(jwtToken).getSubject())
                    .filter(StringUtils::hasText)
                    .filter(this::isValidEmailFormat)
                    .orElseThrow(() -> new JwtTokenHasNoUserEmailException("Invalid or missing email in JWT token"));
        } catch (JwtTokenHasNoUserEmailException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new JwtTokenHasNoUserEmailException("Failed to extract email from JWT token", ex);
        }
    }

    private boolean isValidEmailFormat(String email) {
        return email != null &&
               email.contains("@") &&
               email.length() >= 5 &&
               email.length() <= 254 &&
               !email.startsWith("@") &&
               !email.endsWith("@");
    }

    private Claims extractAllClaims(final String jwtToken) {
        return jwtParser.parseSignedClaims(jwtToken).getPayload();
    }
}
