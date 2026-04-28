package com.zufar.icedlatte.security.filter;

import com.zufar.icedlatte.common.util.ClientIpExtractor;
import com.zufar.icedlatte.security.configuration.RateLimitingConfiguration.RateLimitResult;
import com.zufar.icedlatte.security.configuration.RateLimiter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("PreAuthRateLimitingFilter Tests")
class PreAuthRateLimitingFilterTest {

    @Mock private RateLimiter floodRateLimiter;
    @Mock private RateLimiter authRateLimiter;
    @Mock private ClientIpExtractor clientIpExtractor;

    private PreAuthRateLimitingFilter filter;

    private static final long RESET_MILLIS = System.currentTimeMillis() + 60_000;

    @BeforeEach
    void setUp() {
        filter = new PreAuthRateLimitingFilter(
                floodRateLimiter, authRateLimiter, new SimpleMeterRegistry(), clientIpExtractor);
        ReflectionTestUtils.setField(filter, "maxRequests", 200);
        ReflectionTestUtils.setField(filter, "windowDuration", Duration.ofMinutes(1));
        ReflectionTestUtils.setField(filter, "authMaxRequests", 10);
        ReflectionTestUtils.setField(filter, "authWindowDuration", Duration.ofMinutes(1));
    }

    @Test
    @DisplayName("allowed non-auth request passes through the filter chain")
    void allowedRequestPassesThrough() throws Exception {
        when(clientIpExtractor.extract(any())).thenReturn("1.2.3.4");
        when(floodRateLimiter.tryConsume(any(), anyInt(), any()))
                .thenReturn(new RateLimitResult(true, 200, 199, RESET_MILLIS));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/products");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("flood guard blocks non-auth request and returns 429")
    void floodGuardBlockedRequestReturns429() throws Exception {
        when(clientIpExtractor.extract(any())).thenReturn("1.2.3.4");
        when(floodRateLimiter.tryConsume(contains("pre-auth"), anyInt(), any()))
                .thenReturn(new RateLimitResult(false, 200, 0, RESET_MILLIS));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/products");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        verify(chain, never()).doFilter(any(), any());
        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getHeader("Retry-After")).isNotNull();
        assertThat(response.getHeader("X-RateLimit-Limit")).isEqualTo("200");
        assertThat(response.getHeader("X-RateLimit-Remaining")).isEqualTo("0");
        assertThat(response.getHeader("X-RateLimit-Reset")).isNotNull();
        assertThat(response.getContentType()).contains("application/json");
    }

    @Test
    @DisplayName("auth bucket blocks login at 10/min fail-closed, before flood guard runs")
    void authBucketBlocksLoginFailClosed() throws Exception {
        when(clientIpExtractor.extract(any())).thenReturn("1.2.3.4");
        when(authRateLimiter.tryConsume(contains("auth:ip:"), anyInt(), any()))
                .thenReturn(new RateLimitResult(false, 10, 0, RESET_MILLIS));

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/authenticate");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        verify(chain, never()).doFilter(any(), any());
        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getHeader("X-RateLimit-Limit")).isEqualTo("10");
        // flood guard must not run after auth bucket blocks
        verify(floodRateLimiter, never()).tryConsume(contains("pre-auth"), anyInt(), any());
    }

    @Test
    @DisplayName("auth bucket blocks register at 10/min fail-closed")
    void authBucketBlocksRegisterFailClosed() throws Exception {
        when(clientIpExtractor.extract(any())).thenReturn("1.2.3.4");
        when(authRateLimiter.tryConsume(contains("auth:ip:"), anyInt(), any()))
                .thenReturn(new RateLimitResult(false, 10, 0, RESET_MILLIS));

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/register");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        verify(chain, never()).doFilter(any(), any());
        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getHeader("X-RateLimit-Limit")).isEqualTo("10");
    }

    @Test
    @DisplayName("auth bucket allowed login still passes through flood guard and chain")
    void authBucketAllowedLoginContinuesChain() throws Exception {
        when(clientIpExtractor.extract(any())).thenReturn("1.2.3.4");
        when(authRateLimiter.tryConsume(contains("auth:ip:"), anyInt(), any()))
                .thenReturn(new RateLimitResult(true, 10, 9, RESET_MILLIS));
        when(floodRateLimiter.tryConsume(contains("pre-auth"), anyInt(), any()))
                .thenReturn(new RateLimitResult(true, 200, 199, RESET_MILLIS));

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/authenticate");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
        verify(authRateLimiter).tryConsume(any(), anyInt(), any());
        verify(floodRateLimiter).tryConsume(any(), anyInt(), any());
    }

    @Test
    @DisplayName("auth bucket uses per-IP key")
    void authBucketKeyIsIpBased() throws Exception {
        when(clientIpExtractor.extract(any())).thenReturn("9.8.7.6");
        when(authRateLimiter.tryConsume(any(), anyInt(), any()))
                .thenReturn(new RateLimitResult(true, 10, 9, RESET_MILLIS));
        when(floodRateLimiter.tryConsume(any(), anyInt(), any()))
                .thenReturn(new RateLimitResult(true, 200, 199, RESET_MILLIS));

        filter.doFilterInternal(new MockHttpServletRequest("POST", "/api/v1/auth/authenticate"),
                new MockHttpServletResponse(), mock(FilterChain.class));

        ArgumentCaptor<String> authKeyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> floodKeyCaptor = ArgumentCaptor.forClass(String.class);
        verify(authRateLimiter).tryConsume(authKeyCaptor.capture(), anyInt(), any());
        verify(floodRateLimiter).tryConsume(floodKeyCaptor.capture(), anyInt(), any());
        assertThat(List.of(authKeyCaptor.getValue(), floodKeyCaptor.getValue()))
                .containsExactlyInAnyOrder("auth:ip:9.8.7.6", "pre-auth:ip:9.8.7.6");
    }

    @Test
    @DisplayName("flood guard key is IP-based for non-auth path")
    void floodGuardKeyIsIpBased() throws Exception {
        when(clientIpExtractor.extract(any())).thenReturn("9.8.7.6");
        when(floodRateLimiter.tryConsume(any(), anyInt(), any()))
                .thenReturn(new RateLimitResult(true, 200, 199, RESET_MILLIS));

        filter.doFilterInternal(new MockHttpServletRequest("GET", "/api/v1/products"),
                new MockHttpServletResponse(), mock(FilterChain.class));

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(floodRateLimiter).tryConsume(keyCaptor.capture(), anyInt(), any());
        assertThat(keyCaptor.getValue()).isEqualTo("pre-auth:ip:9.8.7.6");
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

    @Test
    @DisplayName("regular API paths are not skipped")
    void regularApiPathIsNotSkipped() {
        assertThat(filter.shouldNotFilter(new MockHttpServletRequest("POST", "/api/v1/auth/authenticate"))).isFalse();
    }

    @Test
    @DisplayName("validation rejects non-positive flood limit")
    void validateRejectsNonPositiveFloodLimit() {
        ReflectionTestUtils.setField(filter, "maxRequests", 0);

        assertThatThrownBy(filter::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("pre-auth.max-requests must be > 0");
    }

    @Test
    @DisplayName("validation rejects non-positive auth window")
    void validateRejectsNonPositiveAuthWindow() {
        ReflectionTestUtils.setField(filter, "authWindowDuration", Duration.ZERO);

        assertThatThrownBy(filter::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("auth.window-duration must be positive");
    }
}
