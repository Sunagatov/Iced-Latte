package com.zufar.onlinestore.security.jwt;

import com.zufar.onlinestore.security.api.SecurityPrincipalProvider;
import com.zufar.onlinestore.security.exception.AbsentBearerHeaderException;
import com.zufar.onlinestore.security.exception.JwtTokenBlacklistedException;
import com.zufar.onlinestore.security.exception.JwtTokenHasNoUserEmailException;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.doAnswer;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Mock
    private JwtAuthenticationProvider jwtAuthenticationProvider;

    @Mock
    private SecurityPrincipalProvider securityPrincipalProvider;

    @Mock
    private HttpServletRequest httpRequest;

    @Mock
    private HttpServletResponse httpResponse;

    @Mock
    private FilterChain filterChain;

    @Mock
    private SecurityContext securityContext;

    private UsernamePasswordAuthenticationToken authenticationToken;
    private UUID userId = UUID.randomUUID();
    private String errorMessage = "Something went wrong";

    private static MockedStatic<SecurityContextHolder> mockedSecurityContextHolder;

    @BeforeAll
    static void setUpOnce() {
        mockedSecurityContextHolder = mockStatic(SecurityContextHolder.class);
    }

    @AfterAll
    static void tearDownOnce() {
        mockedSecurityContextHolder.close();
    }

    @BeforeEach
    void setUp() {
        when(jwtAuthenticationProvider.get(httpRequest)).thenReturn(authenticationToken);
        when(securityPrincipalProvider.getUserId()).thenReturn(userId);
        mockedSecurityContextHolder.when(() -> SecurityContextHolder.getContext()).thenReturn(securityContext);
    }

    @Test
    @DisplayName("Given No Exception When DoFilterInternal")
    void givenNoExceptionWhenDoFilterInternal() throws ServletException, IOException {
        assertDoesNotThrow(() -> jwtAuthenticationFilter.doFilterInternal(httpRequest, httpResponse, filterChain));

        mockTestDoFilterInternal();
    }

    @Test
    @DisplayName("Given JwtTokenBlacklistedException When DoFilterInternal")
    void givenJwtTokenBlacklistedExceptionWhenDoFilterInternal() throws ServletException, IOException {
        errorMessage = "JWT Token is blacklisted";
        doThrow(JwtTokenBlacklistedException.class).when(filterChain).doFilter(httpRequest, httpResponse);
        when(httpResponse.getWriter()).thenReturn(mock(PrintWriter.class));

        jwtAuthenticationFilter.doFilterInternal(httpRequest, httpResponse, filterChain);

        mockTestDoFilterInternal();
        testHandleExceptions(httpResponse, errorMessage);
    }

    @Test
    @DisplayName("Given AbsentBearerHeaderException When DoFilterInternal")
    void givenAbsentBearerHeaderExceptionWhenDoFilterInternal() throws ServletException, IOException {
        errorMessage = "Bearer authentication header is absent";
        doThrow(AbsentBearerHeaderException.class).when(filterChain).doFilter(httpRequest, httpResponse);
        when(httpResponse.getWriter()).thenReturn(mock(PrintWriter.class));

        jwtAuthenticationFilter.doFilterInternal(httpRequest, httpResponse, filterChain);

        mockTestDoFilterInternal();
        testHandleExceptions(httpResponse, errorMessage);
    }

    @Test
    @DisplayName("Given ExpiredJwtException When DoFilterInternal")
    void givenExpiredJwtExceptionWhenDoFilterInternal() throws ServletException, IOException {
        errorMessage = "Jwt token is expired";
        doThrow(ExpiredJwtException.class).when(filterChain).doFilter(httpRequest, httpResponse);
        when(httpResponse.getWriter()).thenReturn(mock(PrintWriter.class));

        jwtAuthenticationFilter.doFilterInternal(httpRequest, httpResponse, filterChain);

        mockTestDoFilterInternal();
        testHandleExceptions(httpResponse, errorMessage);
    }

    @Test
    @DisplayName("Given JwtTokenHasNoUserEmailException When DoFilterInternal")
    void givenJwtTokenHasNoUserEmailExceptionWhenDoFilterInternal() throws ServletException, IOException {
        errorMessage = "User email not found in jwtToken";
        doThrow(JwtTokenHasNoUserEmailException.class).when(filterChain).doFilter(httpRequest, httpResponse);
        when(httpResponse.getWriter()).thenReturn(mock(PrintWriter.class));

        jwtAuthenticationFilter.doFilterInternal(httpRequest, httpResponse, filterChain);

        mockTestDoFilterInternal();
        testHandleExceptions(httpResponse, errorMessage);
    }

    @Test
    @DisplayName("Given UsernameNotFoundException When DoFilterInternal")
    void givenUsernameNotFoundExceptionWhenDoFilterInternal() throws ServletException, IOException {
        errorMessage = "User with the provided email does not exist";
        doThrow(UsernameNotFoundException.class).when(filterChain).doFilter(httpRequest, httpResponse);
        when(httpResponse.getWriter()).thenReturn(mock(PrintWriter.class));

        jwtAuthenticationFilter.doFilterInternal(httpRequest, httpResponse, filterChain);

        mockTestDoFilterInternal();
        testHandleExceptions(httpResponse, errorMessage);
    }

    @Test
    @DisplayName("Given Exception When DoFilterInternal")
    void givenExceptionWhenDoFilterInternal() throws ServletException, IOException {
        errorMessage = "Internal server error";
        doAnswer(invocation -> {
            throw new Exception(errorMessage);
        }).when(filterChain).doFilter(httpRequest, httpResponse);
        when(httpResponse.getWriter()).thenReturn(mock(PrintWriter.class));

        jwtAuthenticationFilter.doFilterInternal(httpRequest, httpResponse, filterChain);

        mockTestDoFilterInternal();
        testHandleExceptions(httpResponse, errorMessage);
    }

    void mockTestDoFilterInternal() throws ServletException, IOException {
        verify(jwtAuthenticationProvider, times(1)).get(httpRequest);
        verify(securityContext, times(1)).setAuthentication(authenticationToken);
        verify(securityPrincipalProvider, times(1)).getUserId();
        verify(filterChain, times(1)).doFilter(httpRequest, httpResponse);
    }

    void testHandleExceptions(HttpServletResponse httpResponse, String errorMessage) throws IOException {
        verify(httpResponse, times(1)).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(httpResponse, times(1)).getWriter();
        verify(httpResponse.getWriter(), times(1)).write("{ \"message\": \"" + errorMessage + "\" }");
    }
}