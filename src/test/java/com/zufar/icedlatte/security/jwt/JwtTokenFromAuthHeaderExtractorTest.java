package com.zufar.icedlatte.security.jwt;

import com.zufar.icedlatte.security.exception.AbsentBearerHeaderException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("JwtTokenFromAuthHeaderExtractor Tests")
class JwtTokenFromAuthHeaderExtractorTest {

    private JwtTokenFromAuthHeaderExtractor extractor;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        extractor = new JwtTokenFromAuthHeaderExtractor();
        request = new MockHttpServletRequest();
        ReflectionTestUtils.setField(extractor, "jwtHttpRequestHeader", "Authorization");
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
}
