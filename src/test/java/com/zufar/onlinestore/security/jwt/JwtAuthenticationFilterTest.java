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
    private String errorMessage;

    private static MockedStatic<SecurityContextHolder> mockedSecurityContextHolder;
    private static MockedStatic<MDC> mockedMdc;

    @BeforeAll
    static void setUpOnce() {
        mockedSecurityContextHolder = mockStatic(SecurityContextHolder.class);
        mockedMdc = mockStatic(MDC.class);
    }

    @AfterAll
    static void tearDownOnce() {
        mockedSecurityContextHolder.close();
        mockedMdc.close();
    }

    @BeforeEach
    void setUp() {
        when(jwtAuthenticationProvider.get(httpRequest))
                .thenReturn(authenticationToken);
        when(securityPrincipalProvider.getUserId())
                .thenReturn(userId);
        when(SecurityContextHolder.getContext())
                .thenReturn(securityContext);
    }

    @Test
    @DisplayName("Test for doFilterInternal without Exception")
    public void testDoFilterInternalWithoutException() throws ServletException, IOException {
        jwtAuthenticationFilter.doFilterInternal(httpRequest, httpResponse, filterChain);

        mockTestDoFilterInternal();
    }

    @Test
    @DisplayName("Test for doFilterInternal with JwtTokenBlacklistedException")
    public void testDoFilterInternalThrowsJwtTokenBlacklistedException() throws ServletException, IOException, InstantiationException, IllegalAccessException {
        errorMessage = "JWT Token is blacklisted";
        doThrow(JwtTokenBlacklistedException.class)
                .when(filterChain).doFilter(httpRequest, httpResponse);
        when(httpResponse.getWriter())
                .thenReturn(mock(PrintWriter.class));

        jwtAuthenticationFilter.doFilterInternal(httpRequest, httpResponse, filterChain);

        mockTestDoFilterInternal();
        testHandleExceptions(httpResponse, errorMessage);
    }

    @Test
    @DisplayName("Test for doFilterInternal with AbsentBearerHeaderException")
    public void testDoFilterInternalThrowsAbsentBearerHeaderException() throws ServletException, IOException, InstantiationException, IllegalAccessException {
        errorMessage = "Bearer authentication header is absent";
        doThrow(AbsentBearerHeaderException.class)
                .when(filterChain).doFilter(httpRequest, httpResponse);
        when(httpResponse.getWriter())
                .thenReturn(mock(PrintWriter.class));

        jwtAuthenticationFilter.doFilterInternal(httpRequest, httpResponse, filterChain);

        mockTestDoFilterInternal();
        testHandleExceptions(httpResponse, errorMessage);
    }

    @Test
    @DisplayName("Test for doFilterInternal with ExpiredJwtException")
    public void testDoFilterInternalThrowsExpiredJwtException() throws ServletException, IOException, InstantiationException, IllegalAccessException {
        errorMessage = "Jwt token is expired";
        doThrow(ExpiredJwtException.class)
                .when(filterChain).doFilter(httpRequest, httpResponse);
        when(httpResponse.getWriter())
                .thenReturn(mock(PrintWriter.class));

        jwtAuthenticationFilter.doFilterInternal(httpRequest, httpResponse, filterChain);

        mockTestDoFilterInternal();
        testHandleExceptions(httpResponse, errorMessage);
    }

    @Test
    @DisplayName("Test for doFilterInternal with JwtTokenHasNoUserEmailException")
    public void testDoFilterInternalThrowsJwtTokenHasNoUserEmailException() throws ServletException, IOException, InstantiationException, IllegalAccessException {
        errorMessage = "User email not found in jwtToken";
        doThrow(JwtTokenHasNoUserEmailException.class)
                .when(filterChain).doFilter(httpRequest, httpResponse);
        when(httpResponse.getWriter())
                .thenReturn(mock(PrintWriter.class));

        jwtAuthenticationFilter.doFilterInternal(httpRequest, httpResponse, filterChain);

        mockTestDoFilterInternal();
        testHandleExceptions(httpResponse, errorMessage);
    }

    @Test
    @DisplayName("Test for doFilterInternal with UsernameNotFoundException")
    public void testDoFilterInternalThrows_UsernameNotFoundException() throws ServletException, IOException, InstantiationException, IllegalAccessException {
        errorMessage = "User with the provided email does not exist";
        doThrow(UsernameNotFoundException.class)
                .when(filterChain).doFilter(httpRequest, httpResponse);
        when(httpResponse.getWriter())
                .thenReturn(mock(PrintWriter.class));

        jwtAuthenticationFilter.doFilterInternal(httpRequest, httpResponse, filterChain);

        mockTestDoFilterInternal();
        testHandleExceptions(httpResponse, errorMessage);
    }

    @Test
    @DisplayName("Test for doFilterInternal with Exception")
    public void testDoFilterInternalThrowsException() throws ServletException, IOException, InstantiationException, IllegalAccessException {
        errorMessage = "Internal server error";
        doAnswer(invocation -> {
            throw new Exception(errorMessage);
        }).when(filterChain).doFilter(httpRequest, httpResponse);
        when(httpResponse.getWriter())
                .thenReturn(mock(PrintWriter.class));

        jwtAuthenticationFilter.doFilterInternal(httpRequest, httpResponse, filterChain);

        mockTestDoFilterInternal();
        testHandleExceptions(httpResponse, errorMessage);
    }

    private void mockTestDoFilterInternal() throws ServletException, IOException {
        verify(jwtAuthenticationProvider, times(1))
                .get(httpRequest);
        verify(securityContext, times(1))
                .setAuthentication(authenticationToken);
        verify(securityPrincipalProvider, times(1))
                .getUserId();
        verify(filterChain, times(1))
                .doFilter(httpRequest, httpResponse);
    }

    private void testHandleExceptions(HttpServletResponse httpResponse, String errorMessage) throws IOException {
        verify(httpResponse, times(1))
                .setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(httpResponse, times(1))
                .getWriter();
        verify(httpResponse.getWriter(), times(1))
                .write("{ \"message\": \"" + errorMessage + "\" }");
    }
}