package com.zufar.icedlatte.security.jwt;

import com.zufar.icedlatte.security.api.AuthSessionService;
import com.zufar.icedlatte.security.entity.AuthSessionEntity;
import com.zufar.icedlatte.security.exception.JwtTokenBlacklistedException;
import com.zufar.icedlatte.security.exception.JwtTokenHasNoUserEmailException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class JwtRefreshTokenValidator {

    private final JwtParser refreshParser;
    private final JwtTokenFromAuthHeaderExtractor tokenExtractor;
    private final JwtBlacklistService blacklistService;
    private final AuthSessionService authSessionService;

    public JwtRefreshTokenValidator(JwtSignKeyProvider keyProvider,
                                    JwtTokenFromAuthHeaderExtractor tokenExtractor,
                                    JwtBlacklistService blacklistService,
                                    AuthSessionService authSessionService) {
        this.refreshParser = Jwts.parser().verifyWith(keyProvider.getRefresh()).build();
        this.tokenExtractor = tokenExtractor;
        this.blacklistService = blacklistService;
        this.authSessionService = authSessionService;
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

    /**
     * Validates the refresh token and returns the active session.
     * Triggers reuse detection if the token was already rotated.
     */
    public AuthSessionEntity validateAndGetSession(HttpServletRequest request) {
        String token = tokenExtractor.extract(request);
        String hash = blacklistService.sha256(token);
        return authSessionService.findActiveByHash(hash);
    }

    public String extractRawToken(HttpServletRequest request) {
        return tokenExtractor.extract(request);
    }
}
