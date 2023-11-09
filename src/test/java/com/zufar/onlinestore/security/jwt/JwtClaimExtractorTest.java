package com.zufar.onlinestore.security.jwt;

import com.zufar.onlinestore.security.exception.JwtTokenHasNoUserEmailException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.JwtParserBuilder;

import org.apache.commons.lang3.StringUtils;
import org.instancio.Instancio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.Key;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mockStatic;

@ExtendWith(MockitoExtension.class)
class JwtClaimExtractorTest {

    @InjectMocks
    private JwtClaimExtractor jwtClaimExtractor;

    @Mock
    private JwtSignKeyProvider jwtSignKeyProvider;

    @Mock
    private JwtParser jwtParser;

    @Mock
    private JwtParserBuilder jwtParserBuilder;

    @Mock
    private Jws<Claims> jws;

    @Mock
    private Claims claims;

    private Key key = Instancio.create(Key.class);
    private String jwtToken = "TestJwtToken";
    private String userEmail = "TestEmail";

    private static MockedStatic<Jwts> mockedJwts;
    private static MockedStatic<StringUtils> mockedStringUtils;

    @BeforeAll
    static void setUpOnce() {
        mockedJwts = mockStatic(Jwts.class);
        mockedStringUtils = mockStatic(StringUtils.class);
    }

    @AfterAll
    static void tearDownOnce() {
        mockedJwts.close();
        mockedStringUtils.close();
    }

    @BeforeEach
    void setUp() {
        mockedJwts.when(() -> Jwts.parserBuilder()).thenReturn(jwtParserBuilder);
        when(jwtSignKeyProvider.get()).thenReturn(key);
        when(jwtParserBuilder.setSigningKey(key)).thenReturn(jwtParserBuilder);
        when(jwtParserBuilder.build()).thenReturn(jwtParser);

        when(jwtParser.parseClaimsJws(jwtToken)).thenReturn(jws);
        when(jws.getBody()).thenReturn(claims);
    }

    @Test
    @DisplayName("Given a JWT token with a valid email, When extracting email, Then the email should be successfully extracted")
    public void shouldExtractEmailSuccessfullyWithValidEmail() {
        when(claims.getSubject()).thenReturn(userEmail);
        mockedStringUtils.when(() -> StringUtils.isEmpty(userEmail)).thenReturn(false);

        String email = jwtClaimExtractor.extractEmail(jwtToken);

        assertTrue(email.equals(userEmail));
        mockTestExtractAllClaims();
        mockTestGetJwtParser();
        verify(claims, times(1)).getSubject();
    }

    @Test
    @DisplayName("Given a JWT token with no email in the claims, When extracting email, Then it should throw JwtTokenHasNoUserEmailException")
    public void shouldThrowJwtTokenHasNoUserEmailExceptionWhenNoEmailInClaims() {
        when(claims.getSubject()).thenReturn(userEmail);
        mockedStringUtils.when(() -> StringUtils.isEmpty(userEmail)).thenReturn(true);

        assertThrows(JwtTokenHasNoUserEmailException.class, () -> jwtClaimExtractor.extractEmail(jwtToken));

        mockTestExtractAllClaims();
        mockTestGetJwtParser();
        verify(claims, times(1)).getSubject();
    }

    @Test
    @DisplayName("Given a JWT token, When extracting expiration, Then it should be extracted successfully")
    public void shouldExtractExpirationSuccessfully() {
        Date date = new Date();
        when(claims.getExpiration()).thenReturn(date);

        LocalDateTime result = jwtClaimExtractor.extractExpiration(jwtToken);
        assertTrue(getLocalDataTimeByDate(date).equals(result));
    }

    private LocalDateTime getLocalDataTimeByDate(Date expiration) {
        return Instant
                .ofEpochMilli(expiration.getTime())
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }


    private void mockTestExtractAllClaims() {
        verify(jwtParser, times(1)).parseClaimsJws(jwtToken);
        verify(jws, times(1)).getBody();
    }

    private void mockTestGetJwtParser() {
        verify(jwtSignKeyProvider, times(1)).get();
        verify(jwtParserBuilder, times(1)).setSigningKey(key);
        verify(jwtParserBuilder, times(1)).build();
    }
}