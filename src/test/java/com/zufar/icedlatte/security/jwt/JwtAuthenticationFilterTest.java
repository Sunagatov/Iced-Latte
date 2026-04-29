package com.zufar.icedlatte.security.jwt;

import com.zufar.icedlatte.common.util.ClientIpExtractor;
import com.zufar.icedlatte.security.api.SecurityPrincipalProvider;
import com.zufar.icedlatte.security.exception.AbsentBearerHeaderException;
import com.zufar.icedlatte.security.exception.InvalidCredentialsException;
import com.zufar.icedlatte.user.entity.UserEntity;
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

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationFilter")
class JwtAuthenticationFilterTest {

    @Mock
    private JwtAuthenticationProvider jwtAuthenticationProvider;
    @Mock
    private SecurityPrincipalProvider securityPrincipalProvider;
    @Mock
    private JwtClaimExtractor jwtClaimExtractor;
    @Mock
    private JwtTokenFromAuthHeaderExtractor jwtTokenFromAuthHeaderExtractor;
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
        @DisplayName("skips refresh and google auth endpoints")
        void skipsRefreshAndGoogleAuthEndpoints() {
            TestableJwtAuthenticationFilter filter = filter();

            assertThat(filter.shouldSkip(request("/api/v1/auth/refresh"))).isTrue();
            assertThat(filter.shouldSkip(request("/api/v1/auth/google"))).isTrue();
            assertThat(filter.shouldSkip(request("/api/v1/auth/google/callback"))).isTrue();
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
            UserEntity user = UserEntity.builder().id(userId).build();
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(user, "credentials", user.getAuthorities());
            MockHttpServletRequest request = request("/api/v1/me");
            MockHttpServletResponse response = new MockHttpServletResponse();
            FilterChain chain = mock(FilterChain.class);

            when(jwtAuthenticationProvider.get(request)).thenReturn(authentication);
            when(securityPrincipalProvider.getUserId()).thenReturn(userId);
            when(jwtTokenFromAuthHeaderExtractor.extract(request)).thenReturn("jwt-token");
            when(jwtClaimExtractor.extractSessionId("jwt-token")).thenReturn(Optional.of(sessionId));
            doAnswer(_ -> {
                assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(authentication);
                assertThat(MDC.get("userId")).isEqualTo(userId.toString());
                assertThat(MDC.get("sessionId")).isEqualTo(sessionId.toString());
                return null;
            }).when(chain).doFilter(request, response);

            filter().run(request, response, chain);

            verify(chain).doFilter(request, response);
            assertThat(MDC.get("userId")).isNull();
            assertThat(MDC.get("sessionId")).isNull();
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
            verifyNoInteractions(securityPrincipalProvider, jwtTokenFromAuthHeaderExtractor, jwtClaimExtractor);
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
                    .contains("\"error\":\"Unauthorized\"")
                    .contains("\"message\":\"Authentication failed: invalid credentials\"")
                    .contains("\"status\":401");
        }
    }

    private TestableJwtAuthenticationFilter filter() {
        return new TestableJwtAuthenticationFilter(
                jwtAuthenticationProvider,
                securityPrincipalProvider,
                jwtClaimExtractor,
                jwtTokenFromAuthHeaderExtractor,
                clientIpExtractor
        );
    }

    private static MockHttpServletRequest request(String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.setRequestURI(uri);
        return request;
    }

    private static final class TestableJwtAuthenticationFilter extends JwtAuthenticationFilter {

        private TestableJwtAuthenticationFilter(JwtAuthenticationProvider jwtAuthenticationProvider,
                                                SecurityPrincipalProvider securityPrincipalProvider,
                                                JwtClaimExtractor jwtClaimExtractor,
                                                JwtTokenFromAuthHeaderExtractor jwtTokenFromAuthHeaderExtractor,
                                                ClientIpExtractor clientIpExtractor) {
            super(jwtAuthenticationProvider, securityPrincipalProvider, jwtClaimExtractor,
                    jwtTokenFromAuthHeaderExtractor, clientIpExtractor);
        }

        private boolean shouldSkip(MockHttpServletRequest request) {
            return super.shouldNotFilter(request);
        }

        private void run(MockHttpServletRequest request,
                         MockHttpServletResponse response,
                         FilterChain chain) throws ServletException, IOException {
            super.doFilterInternal(request, response, chain);
        }
    }
}
