package com.zufar.icedlatte.security.filter;

import com.zufar.icedlatte.common.util.ClientIpExtractor;
import com.zufar.icedlatte.security.configuration.RateLimitingConfiguration.RateLimitResult;
import com.zufar.icedlatte.security.configuration.RateLimiter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("RateLimitingFilter Tests")
class RateLimitingFilterTest {

    @Mock private RateLimiter rateLimiter;
    @Mock private ClientIpExtractor clientIpExtractor;

    private RateLimitingFilter filter;

    private static final long RESET_MILLIS = System.currentTimeMillis() + 60_000;

    @BeforeEach
    void setUp() {
        filter = new RateLimitingFilter(rateLimiter, new SimpleMeterRegistry(), clientIpExtractor);
        ReflectionTestUtils.setField(filter, "globalMaxRequests", 60);
        ReflectionTestUtils.setField(filter, "globalWindowDuration", Duration.ofMinutes(1));
        ReflectionTestUtils.setField(filter, "authMaxRequests", 10);
        ReflectionTestUtils.setField(filter, "authWindowDuration", Duration.ofMinutes(1));
        ReflectionTestUtils.setField(filter, "searchMaxRequests", 30);
        ReflectionTestUtils.setField(filter, "searchWindowDuration", Duration.ofMinutes(1));
        ReflectionTestUtils.setField(filter, "telemetryMaxRequests", 120);
        ReflectionTestUtils.setField(filter, "telemetryWindowDuration", Duration.ofMinutes(1));
        ReflectionTestUtils.setField(filter, "paymentMaxRequests", 20);
        ReflectionTestUtils.setField(filter, "paymentWindowDuration", Duration.ofMinutes(1));
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
        "/api/v1/auth/login,           global",
        "/api/v1/auth/register,        global",
        "/api/v1/auth/google/callback, global",
        "/api/v1/auth/google,          global",
        "/api/v1/auth/refresh,         auth",
        "/api/v1/telemetry/report,     telemetry",
        "/api/v1/payment,              payment",
        "/api/v1/payment/stripe/webhook, payment",
        "/api/v1/cart,                 global",
        "/api/v1/users/me,             global",
    })
    @DisplayName("resolves correct rate-limit category for path")
    void categoryResolution(String path, String expectedCategory) throws Exception {
        when(clientIpExtractor.extract(any())).thenReturn("1.2.3.4");
        when(rateLimiter.tryConsume(contains(expectedCategory.trim()), anyInt(), any()))
                .thenReturn(new RateLimitResult(true, 60, 59, RESET_MILLIS));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", path.trim());
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, new MockHttpServletResponse(), chain);

        verify(rateLimiter).tryConsume(contains(expectedCategory.trim()), anyInt(), any());
        verify(chain).doFilter(any(), any());
    }

    @Test
    @DisplayName("search category resolved when keyword parameter present")
    void searchCategoryWhenKeywordPresent() throws Exception {
        when(clientIpExtractor.extract(any())).thenReturn("1.2.3.4");
        when(rateLimiter.tryConsume(contains("search"), anyInt(), any()))
                .thenReturn(new RateLimitResult(true, 30, 29, RESET_MILLIS));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/products");
        request.addParameter("keyword", "espresso");
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, new MockHttpServletResponse(), chain);

        verify(rateLimiter).tryConsume(contains("search"), anyInt(), any());
    }

    @Test
    @DisplayName("allowed request passes through with rate-limit headers set")
    void allowedRequestPassesThroughWithHeaders() throws Exception {
        when(clientIpExtractor.extract(any())).thenReturn("1.2.3.4");
        when(rateLimiter.tryConsume(any(), anyInt(), any()))
                .thenReturn(new RateLimitResult(true, 60, 42, RESET_MILLIS));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/products");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getHeader("X-RateLimit-Limit")).isEqualTo("60");
        assertThat(response.getHeader("X-RateLimit-Remaining")).isEqualTo("42");
        assertThat(response.getHeader("X-RateLimit-Reset")).isNotNull();
    }

    @Test
    @DisplayName("blocked request returns 429 with Retry-After and JSON body")
    void blockedRequestReturns429() throws Exception {
        when(clientIpExtractor.extract(any())).thenReturn("1.2.3.4");
        when(rateLimiter.tryConsume(any(), anyInt(), any()))
                .thenReturn(new RateLimitResult(false, 10, 0, RESET_MILLIS));

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        verify(chain, never()).doFilter(any(), any());
        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getHeader("Retry-After")).isNotNull();
        assertThat(response.getContentType()).contains("application/json");
    }

    @Test
    @DisplayName("authenticated user key uses username only, not username+ip")
    void authenticatedUserKeyContainsUsernameOnly() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("alice@example.com", null, List.of()));
        when(rateLimiter.tryConsume(any(), anyInt(), any()))
                .thenReturn(new RateLimitResult(true, 60, 59, RESET_MILLIS));

        filter.doFilterInternal(new MockHttpServletRequest("GET", "/api/v1/cart"),
                new MockHttpServletResponse(), mock(FilterChain.class));

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(rateLimiter).tryConsume(keyCaptor.capture(), anyInt(), any());
        assertThat(keyCaptor.getValue()).contains("user:alice@example.com");
        assertThat(keyCaptor.getValue()).doesNotContain(":ip:");
        verify(clientIpExtractor, never()).extract(any());
    }

    @Test
    @DisplayName("anonymous request key uses IP")
    void anonymousRequestKeyContainsIp() throws Exception {
        when(clientIpExtractor.extract(any())).thenReturn("5.5.5.5");
        when(rateLimiter.tryConsume(any(), anyInt(), any()))
                .thenReturn(new RateLimitResult(true, 60, 59, RESET_MILLIS));

        filter.doFilterInternal(new MockHttpServletRequest("GET", "/api/v1/products"),
                new MockHttpServletResponse(), mock(FilterChain.class));

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(rateLimiter).tryConsume(keyCaptor.capture(), anyInt(), any());
        assertThat(keyCaptor.getValue()).contains("ip:5.5.5.5");
    }

    @Test
    @DisplayName("OPTIONS requests are skipped")
    void optionsRequestIsSkipped() {
        assertThat(filter.shouldNotFilter(new MockHttpServletRequest("OPTIONS", "/api/v1/auth/login"))).isTrue();
    }

    @Test
    @DisplayName("actuator and docs paths are skipped")
    void actuatorAndDocsAreSkipped() {
        assertThat(filter.shouldNotFilter(new MockHttpServletRequest("GET", "/actuator/health"))).isTrue();
        assertThat(filter.shouldNotFilter(new MockHttpServletRequest("GET", "/api/docs/swagger-ui"))).isTrue();
    }
}
