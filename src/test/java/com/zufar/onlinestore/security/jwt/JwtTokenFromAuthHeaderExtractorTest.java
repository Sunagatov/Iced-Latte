package com.zufar.onlinestore.security.jwt;

import com.zufar.onlinestore.security.exception.AbsentBearerHeaderException;
import jakarta.servlet.http.HttpServletRequest;
import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtTokenFromAuthHeaderExtractorTest {
    @InjectMocks
    private JwtTokenFromAuthHeaderExtractor jwtTokenFromAuthHeaderExtractor;
    @Mock
    private HttpServletRequest request;
    private String jwtHttpRequestHeader = HttpHeaders.AUTHORIZATION;
    private String postfix = Instancio.create(String.class);
    private String BEARER_PREFIX;
    private String header;

    @BeforeEach
    void setUp() throws NoSuchFieldException, IllegalAccessException {
        ReflectionTestUtils.setField(jwtTokenFromAuthHeaderExtractor, "jwtHttpRequestHeader", jwtHttpRequestHeader);
        BEARER_PREFIX = JwtTokenFromAuthHeaderExtractor.class
                .getDeclaredField("BEARER_PREFIX")
                .get(null).toString();
        header = BEARER_PREFIX + postfix;
    }

    @Test
    @DisplayName("Test extracting token from HttpServletRequest with Bearer header")
    void testExtractRequestWithBearerHeader() {
        when(request.getHeader(jwtHttpRequestHeader))
                .thenReturn(header);

        String result = jwtTokenFromAuthHeaderExtractor.extract(request);

        assertEquals(postfix, result);
        verify(request, times(1))
                .getHeader(jwtHttpRequestHeader);
    }

    @Test
    @DisplayName("Test extracting token from HttpServletRequest without Bearer header")
    void testExtractRequestWithoutBearerHeader() {
        when(request.getHeader(jwtHttpRequestHeader))
                .thenReturn(postfix);

        assertThrows(AbsentBearerHeaderException.class,
                () -> jwtTokenFromAuthHeaderExtractor.extract(request));
        verify(request, times(1))
                .getHeader(jwtHttpRequestHeader);
    }

    @Test
    @DisplayName("Test extracting token from String with Bearer header")
    void testExtractStringWithBearerHeader() {
        String result = jwtTokenFromAuthHeaderExtractor.extract(header);
        assertEquals(postfix, result);
    }

    @Test
    @DisplayName("Test extracting token from String without Bearer header")
    void testExtractStringWithoutBearerHeader() {
        assertThrows(AbsentBearerHeaderException.class,
                () -> jwtTokenFromAuthHeaderExtractor.extract(postfix));
    }
}
