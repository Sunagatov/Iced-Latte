package com.zufar.icedlatte.security.jwt;

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

    public JwtRefreshTokenValidator(JwtSignKeyProvider keyProvider,
                                    JwtTokenFromAuthHeaderExtractor tokenExtractor) {
        this.refreshParser = Jwts.parser().verifyWith(keyProvider.getRefresh()).build();
        this.tokenExtractor = tokenExtractor;
    }

    public String extractEmail(HttpServletRequest request) {
        String token = tokenExtractor.extract(request);
        try {
            String subject = refreshParser.parseSignedClaims(token).getPayload().getSubject();
            if (!StringUtils.hasText(subject)) {
                throw new JwtTokenHasNoUserEmailException("Refresh token has no subject");
            }
            return subject;
        } catch (JwtTokenHasNoUserEmailException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new JwtTokenHasNoUserEmailException("Invalid refresh token", ex);
        }
    }
}
