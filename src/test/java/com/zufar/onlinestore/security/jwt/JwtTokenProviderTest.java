package com.zufar.onlinestore.security.jwt;

import com.zufar.onlinestore.security.exception.JwtTokenException;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;

import java.security.Key;
import java.util.HashMap;
import java.util.Map;

import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.any;

@ExtendWith(MockitoExtension.class)
class JwtTokenProviderTest {
    @InjectMocks
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private JwtSignKeyProvider jwtSignKeyProvider;
    @Mock
    private UserDetails userDetails;
    @Mock
    private Map<String, Object> extraClaims;
    @Spy
    private static JwtBuilder jwtBuilder;

    private String userName = "TestUserName";
    private Key key = Keys.secretKeyFor(SignatureAlgorithm.HS256);

    private static MockedStatic<Jwts> mockedJwts;

    @BeforeAll
    static void setUpOnce() {
        jwtBuilder = Jwts.builder();
        mockedJwts = mockStatic(Jwts.class);
    }

    @AfterAll
    static void tearDownOnce() {
        mockedJwts.close();
    }

    @BeforeEach
    void setUp() {
        mockedJwts.when(()->Jwts.builder()).thenReturn(jwtBuilder);
        when(userDetails.getUsername()).thenReturn(userName);
    }


    @Test
    @DisplayName("Given valid UserDetails, When generating a token, Then it should return a token with correct claims and signing")
    void shouldReturnTokenWithValidUserDetails() {
        when(jwtSignKeyProvider.get()).thenReturn(key);

        String token = jwtTokenProvider.generateToken(userDetails);

        assertNotNull(token);
        verifyClaimsAndSigning(new HashMap<>(), key);
        verify(jwtBuilder, times(1)).compact();
    }

    @Test
    @DisplayName("Given valid extraClaims and UserDetails, When generating a token, Then it should return a token with correct claims and signing")
    void shouldReturnTokenWithValidExtraClaimsAndUserDetails() {
        when(jwtSignKeyProvider.get()).thenReturn(key);

        String token = jwtTokenProvider.generateToken(extraClaims, userDetails);

        assertNotNull(token);
        verifyClaimsAndSigning(extraClaims, key);
        verify(jwtBuilder, times(1)).compact();
    }

    @Test
    @DisplayName("Given an invalid UserDetails, When generating a token, Then it should throw a JwtTokenException")
    void shouldThrowJwtTokenExceptionForInvalidUserDetails() {
        assertThrows(JwtTokenException.class, () -> jwtTokenProvider.generateToken(userDetails));
        verifyClaimsAndSigning(new HashMap<>(), null);
    }

    @Test
    @DisplayName("Given invalid extraClaims and UserDetails, When generating a token, Then it should throw a JwtTokenException")
    void shouldThrowJwtTokenExceptionForInvalidExtraClaimsAndUserDetails() {
        assertThrows(JwtTokenException.class, () -> jwtTokenProvider.generateToken(extraClaims, userDetails));
        verifyClaimsAndSigning(extraClaims, null);
    }

    private void verifyClaimsAndSigning(Map<String, Object> extraClaims, Key key) {
        verify(jwtBuilder, times(1))
                .setClaims(extraClaims);
        verify(jwtBuilder, times(1))
                .setSubject(userName);
        verify(jwtBuilder, times(1))
                .setIssuedAt(any());
        verify(jwtBuilder, times(1))
                .setExpiration(any());
        verify(jwtBuilder, times(1))
                .signWith(key, SignatureAlgorithm.HS256);
    }
}