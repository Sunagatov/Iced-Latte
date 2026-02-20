package com.zufar.icedlatte.security.jwt;

import com.zufar.icedlatte.security.configuration.JwtProperties;
import com.zufar.icedlatte.security.exception.AbsentBearerHeaderException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtTokenFromAuthHeaderExtractor Tests")
class JwtTokenFromAuthHeaderExtractorTest {

    private static final String VALID_JWT_TOKEN_FIXTURE = "test-header.test-payload.test-signature";

    @Mock
    private JwtProperties jwtProperties;

    private JwtTokenFromAuthHeaderExtractor extractor;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        when(jwtProperties.header()).thenReturn("Authorization");
        extractor = new JwtTokenFromAuthHeaderExtractor(jwtProperties);
        request = new MockHttpServletRequest();
    }

    @Test
    @DisplayName("Should correctly extract token when bearer token is present")
    void shouldExtractTokenWhenBearerTokenPresent() {
        String expectedToken = "some.jwt.token";
        request.addHeader("Authorization", "Bearer " + expectedToken);

        String actualToken = extractor.extract(request);

        assertEquals(expectedToken, actualToken);
    }

    @Test
    @DisplayName("Should throw exception when bearer token is absent")
    void shouldThrowExceptionWhenBearerTokenAbsent() {
        request.addHeader("Authorization", "NonBearerToken");

        assertThrows(AbsentBearerHeaderException.class, () -> extractor.extract(request));
    }

    @Test
    @DisplayName("Should throw exception when authorization header is absent")
    void shouldThrowExceptionWhenAuthorizationHeaderAbsent() {
        assertThrows(AbsentBearerHeaderException.class, () -> extractor.extract(request));
    }

    @Test
    @DisplayName("Should throw exception when token is too short")
    void shouldThrowExceptionWhenTokenTooShort() {
        request.addHeader("Authorization", "Bearer short");
        
        assertThrows(AbsentBearerHeaderException.class, () -> extractor.extract(request));
    }

    @Test
    @DisplayName("Should throw exception when token format is invalid")
    void shouldThrowExceptionWhenTokenFormatInvalid() {
        request.addHeader("Authorization", "Bearer invalidtokenformat");
        
        assertThrows(AbsentBearerHeaderException.class, () -> extractor.extract(request));
    }

    @Test
    @DisplayName("Should extract valid JWT token")
    void shouldExtractValidJwtToken() {
        request.addHeader("Authorization", "Bearer " + VALID_JWT_TOKEN_FIXTURE);

        String actualToken = extractor.extract(request);

        assertEquals(VALID_JWT_TOKEN_FIXTURE, actualToken);
    }
}
