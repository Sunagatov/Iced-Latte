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
    private String postfix = "POSTFIX";
    private String BEARER_PREFIX;
    private String header;

    @BeforeEach
    void setUp() throws NoSuchFieldException, IllegalAccessException {
        ReflectionTestUtils.setField(jwtTokenFromAuthHeaderExtractor, "jwtHttpRequestHeader", jwtHttpRequestHeader);
        BEARER_PREFIX = jwtTokenFromAuthHeaderExtractor.BEARER_PREFIX;
        header = BEARER_PREFIX + postfix;
    }

    @Test
    @DisplayName("Given a HttpServletRequest with a Bearer header, When extracting the token, Then it should return the token")
    void shouldReturnTokenFromRequestWithBearerHeader() {
        when(request.getHeader(jwtHttpRequestHeader)).thenReturn(header);

        String result = jwtTokenFromAuthHeaderExtractor.extract(request);

        assertEquals(postfix, result);
        verify(request, times(1)).getHeader(jwtHttpRequestHeader);
    }

    @Test
    @DisplayName("Given a HttpServletRequest without a Bearer header, When extracting the token, Then it should throw an AbsentBearerHeaderException")
    void shouldThrowAbsentBearerHeaderExceptionForRequestWithoutBearerHeader() {
        when(request.getHeader(jwtHttpRequestHeader)).thenReturn(postfix);

        assertThrows(AbsentBearerHeaderException.class, () -> jwtTokenFromAuthHeaderExtractor.extract(request));
        verify(request, times(1)).getHeader(jwtHttpRequestHeader);
    }

    @Test
    @DisplayName("Given a String with a Bearer header, When extracting the token, Then it should return the token")
    void shouldReturnTokenFromStringWithBearerHeader() {
        String result = jwtTokenFromAuthHeaderExtractor.extract(header);
        assertEquals(postfix, result);
    }

    @Test
    @DisplayName("Given a String without a Bearer header, When extracting the token, Then it should throw an AbsentBearerHeaderException")
    void shouldThrowAbsentBearerHeaderExceptionForStringWithoutBearerHeader() {
        assertThrows(AbsentBearerHeaderException.class, () -> jwtTokenFromAuthHeaderExtractor.extract(postfix));
    }
}
