package com.zufar.icedlatte.security.jwt;

import com.zufar.icedlatte.security.exception.JwtTokenBlacklistedException;
import com.zufar.icedlatte.security.exception.JwtTokenHasNoUserEmailException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Optional;
import java.util.UUID;

@Service
public class JwtRefreshTokenValidator {

    private final JwtParser refreshParser;
    private final JwtTokenFromAuthHeaderExtractor tokenExtractor;
    private final JwtBlacklistService blacklistService;

    public JwtRefreshTokenValidator(JwtSignKeyProvider keyProvider,
                                    JwtTokenFromAuthHeaderExtractor tokenExtractor,
                                    JwtBlacklistService blacklistService) {
        this.refreshParser = Jwts.parser().verifyWith(keyProvider.getRefresh()).build();
        this.tokenExtractor = tokenExtractor;
        this.blacklistService = blacklistService;
    }

    /**
     * Validates the refresh token from the request and returns the user email.
     * Also validates against the server-side session store (reuse detection included).
     */
    public String extractEmail(HttpServletRequest request) {
        String token = tokenExtractor.extract(request);

        // Legacy blacklist check (bridge for tokens issued before session model)
        if (blacklistService.isBlacklisted(token)) {
            throw new JwtTokenBlacklistedException("Refresh token has been revoked");
        }

        try {
            String subject = refreshParser.parseSignedClaims(token).getPayload().getSubject();
            if (!StringUtils.hasText(subject)) {
                throw new JwtTokenHasNoUserEmailException("Refresh token has no subject");
            }
            return subject;
        } catch (JwtTokenHasNoUserEmailException | JwtTokenBlacklistedException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new JwtTokenHasNoUserEmailException("Invalid refresh token", ex);
        }
    }

    public String extractRawToken(HttpServletRequest request) {
        return tokenExtractor.extract(request);
    }

    /**
     * Returns true if the token carries ver=2, meaning it was issued after session management
     * was introduced. Legacy tokens (no ver claim) may be migrated once; modern tokens must
     * have a matching active session or be rejected.
     */
    public boolean isSessionManaged(String rawToken) {
        try {
            Object ver = refreshParser.parseSignedClaims(rawToken).getPayload().get("ver");
            return ver != null;
        } catch (Exception ex) {
            return false;
        }
    }

    /**
     * Extracts the sid (session ID) claim from a refresh token.
     * Returns empty if the token is invalid or has no sid.
     */
    public Optional<UUID> extractSessionId(String rawToken) {
        try {
            String sid = (String) refreshParser.parseSignedClaims(rawToken)
                    .getPayload()
                    .get("sid");
            return StringUtils.hasText(sid)
                    ? Optional.of(UUID.fromString(sid))
                    : Optional.empty();
        } catch (Exception ex) {
            return Optional.empty();
        }
    }
}
