package com.zufar.icedlatte.security.jwt.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zufar.icedlatte.common.exception.handler.ProblemTypeUriFactory;
import com.zufar.icedlatte.common.util.ClientIpExtractor;
import com.zufar.icedlatte.security.api.CurrentUserProvider;
import com.zufar.icedlatte.security.config.SecurityProblemResponseWriter;
import com.zufar.icedlatte.security.jwt.exception.JwtTokenException;
import com.zufar.icedlatte.security.jwt.provider.JwtAuthenticationProvider;
import com.zufar.icedlatte.security.jwt.resolver.JwtBearerTokenResolver;
import com.zufar.icedlatte.security.jwt.resolver.JwtTokenClaims;
import com.zufar.icedlatte.security.signin.auth.SecurityUserDetails;
import com.zufar.icedlatte.security.signin.exception.AbsentBearerHeaderException;
import com.zufar.icedlatte.security.signin.exception.InvalidCredentialsException;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationFilter")
class JwtAuthenticationFilterTest {

    @Mock
    private JwtAuthenticationProvider jwtAuthenticationProvider;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private JwtTokenClaims jwtTokenClaims;

    @Mock
    private JwtBearerTokenResolver jwtBearerTokenResolver;

    @Mock
    private ClientIpExtractor clientIpExtractor;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        MDC.clear();
    }

    @Nested
    @DisplayName("shouldNotFilter")
    class ShouldNotFilter {

        @Test
        @DisplayName("skips refresh and oauth endpoints")
        void skipsRefreshAndOAuthEndpoints() {
            TestableJwtAuthenticationFilter filter = filter();

            assertThat(filter.shouldSkip(request("/api/v1/auth/refresh"))).isTrue();
            assertThat(filter.shouldSkip(request("/api/v1/auth/oauth/google"))).isTrue();
            assertThat(filter.shouldSkip(request("/api/v1/auth/oauth/google/callback")))
                    .isTrue();
            assertThat(filter.shouldSkip(request("/api/v1/auth/oauth/github"))).isTrue();
            assertThat(filter.shouldSkip(request("/api/v1/auth/oauth/github/callback")))
                    .isTrue();
            assertThat(filter.shouldSkip(request("/api/v1/products"))).isFalse();
        }
    }

    @Nested
    @DisplayName("doFilterInternal")
    class DoFilterInternal {

        @Test
        @DisplayName("authenticates request, enriches MDC, and clears MDC after chain")
        void authenticatesRequestEnrichesMdcAndClearsAfterChain() throws ServletException, IOException {
            UUID userId = UUID.randomUUID();
            UUID sessionId = UUID.randomUUID();
            SecurityUserDetails user = user(userId);
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(user, "credentials", user.getAuthorities());
            MockHttpServletRequest request = request("/api/v1/me");
            MockHttpServletResponse response = new MockHttpServletResponse();
            FilterChain chain = mock(FilterChain.class);

            when(jwtAuthenticationProvider.get(request)).thenReturn(authentication);
            when(currentUserProvider.getUserId()).thenReturn(userId);
            when(jwtBearerTokenResolver.extract(request)).thenReturn("jwt-token");
            when(jwtTokenClaims.extractAccessTokenSessionId("jwt-token")).thenReturn(Optional.of(sessionId));
            doAnswer(_ -> {
                        assertThat(SecurityContextHolder.getContext().getAuthentication())
                                .isSameAs(authentication);
                        assertThat(MDC.get("userId")).isEqualTo(userId.toString());
                        assertThat(MDC.get("sessionId")).isEqualTo(sessionId.toString());
                        return null;
                    })
                    .when(chain)
                    .doFilter(request, response);

            filter().run(request, response, chain);

            verify(chain).doFilter(request, response);
            assertThat(MDC.get("userId")).isNull();
            assertThat(MDC.get("sessionId")).isNull();
        }

        @Test
        @DisplayName("continues when session-id extraction fails after successful authentication")
        void continuesWhenSessionIdExtractionFailsAfterSuccessfulAuthentication() throws ServletException, IOException {
            UUID userId = UUID.randomUUID();
            SecurityUserDetails user = user(userId);
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(user, "credentials", user.getAuthorities());
            MockHttpServletRequest request = request("/api/v1/me");
            MockHttpServletResponse response = new MockHttpServletResponse();
            FilterChain chain = mock(FilterChain.class);

            when(jwtAuthenticationProvider.get(request)).thenReturn(authentication);
            when(currentUserProvider.getUserId()).thenReturn(userId);
            when(jwtBearerTokenResolver.extract(request)).thenReturn("jwt-token");
            when(jwtTokenClaims.extractAccessTokenSessionId("jwt-token")).thenThrow(new JwtTokenException("bad sid"));
            doAnswer(_ -> {
                        assertThat(MDC.get("userId")).isEqualTo(userId.toString());
                        assertThat(MDC.get("sessionId")).isNull();
                        return null;
                    })
                    .when(chain)
                    .doFilter(request, response);

            filter().run(request, response, chain);

            verify(chain).doFilter(request, response);
        }

        @Test
        @DisplayName("continues anonymously when bearer header is absent")
        void continuesAnonymouslyWhenBearerHeaderIsAbsent() throws ServletException, IOException {
            MockHttpServletRequest request = request("/api/v1/products");
            MockHttpServletResponse response = new MockHttpServletResponse();
            FilterChain chain = mock(FilterChain.class);
            when(jwtAuthenticationProvider.get(request)).thenThrow(new AbsentBearerHeaderException());

            filter().run(request, response, chain);

            verify(chain).doFilter(request, response);
            verifyNoInteractions(currentUserProvider, jwtBearerTokenResolver, jwtTokenClaims);
            assertThat(response.getStatus()).isEqualTo(200);
        }

        @Test
        @DisplayName("returns unauthorized JSON when authentication fails")
        void returnsUnauthorizedJsonWhenAuthenticationFails() throws ServletException, IOException {
            MockHttpServletRequest request = request("/api/v1/me");
            MockHttpServletResponse response = new MockHttpServletResponse();
            FilterChain chain = mock(FilterChain.class);
            when(jwtAuthenticationProvider.get(request)).thenThrow(new InvalidCredentialsException());
            when(clientIpExtractor.extract(request)).thenReturn("203.0.113.10");

            filter().run(request, response, chain);

            verifyNoInteractions(chain);
            assertThat(response.getStatus()).isEqualTo(401);
            assertThat(response.getContentType()).startsWith("application/json");
            assertThat(response.getCharacterEncoding()).isEqualTo("UTF-8");
            assertThat(response.getHeader("X-Content-Type-Options")).isEqualTo("nosniff");
            assertThat(response.getContentAsString())
                    .contains("\"title\":\"Authentication failed\"")
                    .contains("\"detail\":\"Authentication failed.\"")
                    .contains("\"status\":401");
        }

        @Test
        @DisplayName("clears security context and MDC when post-authentication enrichment fails")
        void clearsSecurityContextAndMdcWhenPostAuthenticationEnrichmentFails() throws ServletException, IOException {
            UUID userId = UUID.randomUUID();
            SecurityUserDetails user = user(userId);
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(user, "credentials", user.getAuthorities());
            MockHttpServletRequest request = request("/api/v1/me");
            MockHttpServletResponse response = new MockHttpServletResponse();
            FilterChain chain = mock(FilterChain.class);

            when(jwtAuthenticationProvider.get(request)).thenReturn(authentication);
            when(currentUserProvider.getUserId()).thenThrow(new RuntimeException("boom"));
            when(clientIpExtractor.extract(request)).thenReturn("203.0.113.10");

            filter().run(request, response, chain);

            verifyNoInteractions(chain);
            assertThat(response.getStatus()).isEqualTo(500);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            assertThat(MDC.get("userId")).isNull();
            assertThat(MDC.get("sessionId")).isNull();
        }
    }

    private TestableJwtAuthenticationFilter filter() {
        return new TestableJwtAuthenticationFilter(
                jwtAuthenticationProvider,
                currentUserProvider,
                jwtTokenClaims,
                jwtBearerTokenResolver,
                clientIpExtractor);
    }

    private static MockHttpServletRequest request(String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.setRequestURI(uri);
        return request;
    }

    private static SecurityUserDetails user(UUID userId) {
        return new SecurityUserDetails(
                userId, "user@example.com", "credentials", java.util.List.of(), true, true, true, true);
    }

    private static final class TestableJwtAuthenticationFilter extends JwtAuthenticationFilter {

        private TestableJwtAuthenticationFilter(
                JwtAuthenticationProvider jwtAuthenticationProvider,
                CurrentUserProvider currentUserProvider,
                JwtTokenClaims jwtTokenClaims,
                JwtBearerTokenResolver jwtBearerTokenResolver,
                ClientIpExtractor clientIpExtractor) {
            super(
                    jwtAuthenticationProvider,
                    currentUserProvider,
                    jwtTokenClaims,
                    jwtBearerTokenResolver,
                    clientIpExtractor,
                    new SecurityProblemResponseWriter(
                            new ObjectMapper(), new ProblemTypeUriFactory("https://errors.example.test/problems")),
                    new JwtAuthenticationFailureMapper());
        }

        private boolean shouldSkip(MockHttpServletRequest request) {
            return super.shouldNotFilter(request);
        }

        private void run(MockHttpServletRequest request, MockHttpServletResponse response, FilterChain chain)
                throws ServletException, IOException {
            super.doFilterInternal(request, response, chain);
        }
    }
}
