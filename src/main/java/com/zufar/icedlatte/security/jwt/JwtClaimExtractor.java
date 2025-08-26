package com.zufar.icedlatte.security.jwt;

import com.zufar.icedlatte.security.exception.JwtTokenHasNoUserEmailException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.Optional;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class JwtClaimExtractor {
    private final JwtSignKeyProvider jwtSignKeyProvider;

    public String extractEmail(final String jwtToken) {
        try {
            return Optional.ofNullable(extractAllClaims(jwtToken).getSubject())
                    .filter(StringUtils::hasText)
                    .filter(this::isValidEmailFormat)
                    .orElseThrow(() -> new JwtTokenHasNoUserEmailException("Invalid or missing email in JWT token"));
        } catch (JwtTokenHasNoUserEmailException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new JwtTokenHasNoUserEmailException("Failed to extract email from JWT token");
        }
    }

    public LocalDateTime extractExpiration(final String jwtToken) {
        try {
            Date expiration = extractAllClaims(jwtToken).getExpiration();
            if (expiration == null) {
                throw new IllegalArgumentException("JWT token has no expiration date");
            }
            return expiration.toInstant()
                    .atOffset(ZoneOffset.UTC)
                    .toLocalDateTime();
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to extract expiration from JWT token", ex);
        }
    }

    public boolean isTokenExpired(final String jwtToken) {
        try {
            LocalDateTime expiration = extractExpiration(jwtToken);
            LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
            return expiration.isBefore(now);
        } catch (Exception ex) {
            // If we can't determine expiration, treat as expired for security
            return true;
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
        return getJwtParser()
                .parseSignedClaims(jwtToken)
                .getPayload();
    }

    private JwtParser getJwtParser() {
        return Jwts
                .parser()
                .verifyWith(jwtSignKeyProvider.get())
                .build();
    }
}
