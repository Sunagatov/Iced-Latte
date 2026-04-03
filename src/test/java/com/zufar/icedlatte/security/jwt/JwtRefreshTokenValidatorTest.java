package com.zufar.icedlatte.security.jwt;

import com.zufar.icedlatte.security.exception.JwtTokenBlacklistedException;
import com.zufar.icedlatte.security.exception.JwtTokenHasNoUserEmailException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

import javax.crypto.SecretKey;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtRefreshTokenValidator unit tests")
class JwtRefreshTokenValidatorTest {

    @Mock private JwtTokenFromAuthHeaderExtractor tokenExtractor;
    @Mock private JwtBlacklistService blacklistService;

    private JwtRefreshTokenValidator validator;

    @BeforeEach
    void setUp() {
        JwtSignKeyProvider keyProvider = mock(JwtSignKeyProvider.class);
        SecretKey key = io.jsonwebtoken.security.Keys.hmacShaKeyFor(new byte[64]);
        when(keyProvider.getRefresh()).thenReturn(key);
        validator = new JwtRefreshTokenValidator(keyProvider, tokenExtractor, blacklistService);
    }

    @Test
    @DisplayName("extractEmail throws JwtTokenBlacklistedException when refresh token is blacklisted")
    void extractEmailBlacklistedTokenThrows() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        String token = "some.refresh.token";
        when(tokenExtractor.extract(request)).thenReturn(token);
        when(blacklistService.isBlacklisted(token)).thenReturn(true);

        assertThatThrownBy(() -> validator.extractEmail(request))
                .isInstanceOf(JwtTokenBlacklistedException.class);
    }

    @Test
    @DisplayName("extractEmail throws JwtTokenHasNoUserEmailException when token is not blacklisted but invalid")
    void extractEmailInvalidTokenThrows() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        String token = "invalid.token.value";
        when(tokenExtractor.extract(request)).thenReturn(token);
        when(blacklistService.isBlacklisted(token)).thenReturn(false);

        assertThatThrownBy(() -> validator.extractEmail(request))
                .isInstanceOf(JwtTokenHasNoUserEmailException.class);
    }
}
