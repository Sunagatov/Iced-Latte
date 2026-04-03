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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PreAuthRateLimitingFilter Tests")
class PreAuthRateLimitingFilterTest {

    @Mock private RateLimiter rateLimiter;
    @Mock private ClientIpExtractor clientIpExtractor;

    private PreAuthRateLimitingFilter filter;

    private static final long RESET_MILLIS = System.currentTimeMillis() + 60_000;

    @BeforeEach
    void setUp() {
        filter = new PreAuthRateLimitingFilter(rateLimiter, new SimpleMeterRegistry(), clientIpExtractor);
        ReflectionTestUtils.setField(filter, "maxRequests", 200);
        ReflectionTestUtils.setField(filter, "windowDuration", Duration.ofMinutes(1));
    }

    @Test
    @DisplayName("allowed request passes through the filter chain")
    void allowedRequest_passesThrough() throws Exception {
        when(clientIpExtractor.extract(any())).thenReturn("1.2.3.4");
        when(rateLimiter.tryConsume(any(), anyInt(), any()))
                .thenReturn(new RateLimitResult(true, 200, 199, RESET_MILLIS));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/products");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("blocked request returns 429 with all required headers and JSON body")
    void blockedRequest_returns429_withHeaders() throws Exception {
        when(clientIpExtractor.extract(any())).thenReturn("1.2.3.4");
        when(rateLimiter.tryConsume(any(), anyInt(), any()))
                .thenReturn(new RateLimitResult(false, 200, 0, RESET_MILLIS));

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
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
    @DisplayName("rate-limit key is IP-based")
    void rateLimitKey_isIpBased() throws Exception {
        when(clientIpExtractor.extract(any())).thenReturn("9.8.7.6");
        when(rateLimiter.tryConsume(any(), anyInt(), any()))
                .thenReturn(new RateLimitResult(true, 200, 199, RESET_MILLIS));

        filter.doFilterInternal(new MockHttpServletRequest("GET", "/api/v1/products"),
                new MockHttpServletResponse(), mock(FilterChain.class));

        var keyCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(rateLimiter).tryConsume(keyCaptor.capture(), anyInt(), any());
        assertThat(keyCaptor.getValue()).isEqualTo("pre-auth:ip:9.8.7.6");
    }

    @Test
    @DisplayName("OPTIONS requests are skipped")
    void optionsRequest_isSkipped() {
        assertThat(filter.shouldNotFilter(new MockHttpServletRequest("OPTIONS", "/api/v1/auth/login"))).isTrue();
    }

    @Test
    @DisplayName("actuator and docs paths are skipped")
    void actuatorAndDocs_areSkipped() {
        assertThat(filter.shouldNotFilter(new MockHttpServletRequest("GET", "/actuator/health"))).isTrue();
        assertThat(filter.shouldNotFilter(new MockHttpServletRequest("GET", "/api/docs/swagger-ui"))).isTrue();
    }

    @Test
    @DisplayName("regular API paths are not skipped")
    void regularApiPath_isNotSkipped() {
        assertThat(filter.shouldNotFilter(new MockHttpServletRequest("POST", "/api/v1/auth/login"))).isFalse();
    }
}
