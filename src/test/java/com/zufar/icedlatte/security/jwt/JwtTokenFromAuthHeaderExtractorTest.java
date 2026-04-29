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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
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
        lenient().when(jwtProperties.header()).thenReturn("Authorization");
        extractor = new JwtTokenFromAuthHeaderExtractor(jwtProperties);
        request = new MockHttpServletRequest();
    }

    @Test
    @DisplayName("Should correctly extract token when bearer token is present")
    void shouldExtractTokenWhenBearerTokenPresent() {
        String expectedToken = "some.jwt.token";
        request.addHeader("Authorization", "Bearer " + expectedToken);

        String actualToken = extractor.extract(request);

        assertThat(actualToken).isEqualTo(expectedToken);
    }

    @Test
    @DisplayName("Should throw exception when bearer token is absent")
    void shouldThrowExceptionWhenBearerTokenAbsent() {
        request.addHeader("Authorization", "NonBearerToken");

        assertThatThrownBy(() -> extractor.extract(request))
                .isInstanceOf(AbsentBearerHeaderException.class)
                .hasMessageContaining("Missing or invalid Authorization header");
    }

    @Test
    @DisplayName("Should throw exception when authorization header is absent")
    void shouldThrowExceptionWhenAuthorizationHeaderAbsent() {
        assertThatThrownBy(() -> extractor.extract(request))
                .isInstanceOf(AbsentBearerHeaderException.class)
                .hasMessageContaining("Missing or invalid Authorization header");
    }

    @Test
    @DisplayName("Should throw exception when token is too short")
    void shouldThrowExceptionWhenTokenTooShort() {
        request.addHeader("Authorization", "Bearer short");

        assertThatThrownBy(() -> extractor.extract(request))
                .isInstanceOf(AbsentBearerHeaderException.class)
                .hasMessageContaining("Missing or invalid Authorization header");
    }

    @Test
    @DisplayName("Should throw exception when token format is invalid")
    void shouldThrowExceptionWhenTokenFormatInvalid() {
        request.addHeader("Authorization", "Bearer invalidtokenformat");

        assertThatThrownBy(() -> extractor.extract(request))
                .isInstanceOf(AbsentBearerHeaderException.class)
                .hasMessageContaining("Missing or invalid Authorization header");
    }

    @Test
    @DisplayName("Should extract valid JWT token")
    void shouldExtractValidJwtToken() {
        request.addHeader("Authorization", "Bearer " + VALID_JWT_TOKEN_FIXTURE);

        String actualToken = extractor.extract(request);

        assertThat(actualToken).isEqualTo(VALID_JWT_TOKEN_FIXTURE);
    }

    @Test
    @DisplayName("Should trim token value after bearer prefix")
    void shouldTrimTokenValueAfterBearerPrefix() {
        JwtTokenFromAuthHeaderExtractor standaloneExtractor = new JwtTokenFromAuthHeaderExtractor(jwtProperties);

        String actualToken = standaloneExtractor.extract("Bearer   " + VALID_JWT_TOKEN_FIXTURE + "   ");

        assertThat(actualToken).isEqualTo(VALID_JWT_TOKEN_FIXTURE);
    }

    @Test
    @DisplayName("Should use configured header name")
    void shouldUseConfiguredHeaderName() {
        when(jwtProperties.header()).thenReturn("X-Auth");
        request.addHeader("X-Auth", "Bearer " + VALID_JWT_TOKEN_FIXTURE);

        String actualToken = extractor.extract(request);

        assertThat(actualToken).isEqualTo(VALID_JWT_TOKEN_FIXTURE);
    }
}
