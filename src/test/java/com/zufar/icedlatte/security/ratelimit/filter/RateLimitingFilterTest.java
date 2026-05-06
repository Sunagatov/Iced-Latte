package com.zufar.icedlatte.security.ratelimit.filter;

import com.zufar.icedlatte.common.config.CaffeineSizeProperties;
import com.zufar.icedlatte.common.exception.handler.ProblemTypeUriFactory;
import com.zufar.icedlatte.common.util.ClientIpExtractor;
import com.zufar.icedlatte.security.jwt.JwtBlacklistValidator;
import com.zufar.icedlatte.security.jwt.JwtClaimExtractor;
import com.zufar.icedlatte.security.jwt.JwtTokenFromAuthHeaderExtractor;
import com.zufar.icedlatte.security.ratelimit.RateLimitProperties;
import com.zufar.icedlatte.security.ratelimit.RateLimitResult;
import com.zufar.icedlatte.security.ratelimit.RateLimiter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.servlet.FilterChain;
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

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("RateLimitingFilter Tests")
class RateLimitingFilterTest {

    @Mock private RateLimiter openRateLimiter;
    @Mock private RateLimiter closedRateLimiter;
    @Mock private ClientIpExtractor clientIpExtractor;
    @Mock private JwtTokenFromAuthHeaderExtractor jwtTokenFromAuthHeaderExtractor;
    @Mock private JwtClaimExtractor jwtClaimExtractor;
    @Mock private JwtBlacklistValidator jwtBlacklistValidator;

    private RateLimitingFilter filter;
    private final ProblemTypeUriFactory problemTypeUriFactory =
            new ProblemTypeUriFactory("https://errors.example.test/problems");

    private static final long RESET_MILLIS = System.currentTimeMillis() + 60_000;

    @BeforeEach
    void setUp() {
        filter = new RateLimitingFilter(
                openRateLimiter,
                closedRateLimiter,
                new SimpleMeterRegistry(),
                clientIpExtractor,
                jwtTokenFromAuthHeaderExtractor,
                jwtClaimExtractor,
                jwtBlacklistValidator,
                properties(),
                problemTypeUriFactory,
                new CaffeineSizeProperties()
        );
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
        "/api/v1/auth/authenticate,    global",
        "/api/v1/auth/register,        global",
        "/api/v1/auth/google/callback, global",
        "/api/v1/auth/google,          global",
        "/api/v1/auth/refresh,         auth",
        "/api/v1/telemetry/report,     telemetry",
        "/api/v1/payment,              payment",
        "/api/v1/payment/stripe/webhook, payment",
        "/api/v1/users/password/reset, auth",
        "/api/v1/cart,                 global",
        "/api/v1/users/me,             global",
    })
    @DisplayName("resolves correct rate-limit category for path")
    void categoryResolution(String path, String expectedCategory) throws Exception {
        when(clientIpExtractor.extract(any())).thenReturn("1.2.3.4");
        when(closedRateLimiter.tryConsume(any(), anyInt(), any()))
                .thenReturn(new RateLimitResult(true, 10, 9, RESET_MILLIS));
        when(openRateLimiter.tryConsume(argThat(key -> key != null && key.startsWith("pre-auth:")), anyInt(), any()))
                .thenReturn(new RateLimitResult(true, 200, 199, RESET_MILLIS));
        when(openRateLimiter.tryConsume(argThat(key -> key != null && key.startsWith(expectedCategory.trim() + ":")), anyInt(), any()))
                .thenReturn(new RateLimitResult(true, 60, 59, RESET_MILLIS));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", path.trim());
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, new MockHttpServletResponse(), chain);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(openRateLimiter, org.mockito.Mockito.atLeastOnce()).tryConsume(keyCaptor.capture(), anyInt(), any());
        assertThat(keyCaptor.getAllValues()).anyMatch(key -> key.startsWith(expectedCategory.trim() + ":"));
        verify(chain).doFilter(any(), any());
    }

    @Test
    @DisplayName("search category resolved when keyword parameter present")
    void searchCategoryWhenKeywordPresent() throws Exception {
        when(clientIpExtractor.extract(any())).thenReturn("1.2.3.4");
        when(openRateLimiter.tryConsume(argThat(key -> key != null && key.startsWith("pre-auth:")), anyInt(), any()))
                .thenReturn(new RateLimitResult(true, 200, 199, RESET_MILLIS));
        when(openRateLimiter.tryConsume(argThat(key -> key != null && key.startsWith("search:")), anyInt(), any()))
                .thenReturn(new RateLimitResult(true, 30, 29, RESET_MILLIS));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/products");
        request.addParameter("keyword", "espresso");
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, new MockHttpServletResponse(), chain);

        verify(openRateLimiter).tryConsume(argThat(key -> key != null && key.startsWith("search:")), anyInt(), any());
    }

    @Test
    @DisplayName("products path without keyword stays in the global bucket")
    void productsPathWithoutKeywordUsesGlobalBucket() throws Exception {
        when(clientIpExtractor.extract(any())).thenReturn("1.2.3.4");
        when(openRateLimiter.tryConsume(argThat(key -> key != null && key.startsWith("pre-auth:")), anyInt(), any()))
                .thenReturn(new RateLimitResult(true, 200, 199, RESET_MILLIS));
        when(openRateLimiter.tryConsume(argThat(key -> key != null && key.startsWith("global:")), anyInt(), any()))
                .thenReturn(new RateLimitResult(true, 60, 59, RESET_MILLIS));

        filter.doFilterInternal(
                new MockHttpServletRequest("GET", "/api/v1/products"),
                new MockHttpServletResponse(),
                mock(FilterChain.class)
        );

        verify(openRateLimiter).tryConsume(argThat(key -> key != null && key.startsWith("global:")), anyInt(), any());
        verify(openRateLimiter, never()).tryConsume(argThat(key -> key != null && key.startsWith("search:")), anyInt(), any());
    }

    @Test
    @DisplayName("allowed request passes through with rate-limit headers set")
    void allowedRequestPassesThroughWithHeaders() throws Exception {
        when(clientIpExtractor.extract(any())).thenReturn("1.2.3.4");
        when(openRateLimiter.tryConsume(argThat(key -> key != null && key.startsWith("pre-auth:")), anyInt(), any()))
                .thenReturn(new RateLimitResult(true, 200, 199, RESET_MILLIS));
        when(openRateLimiter.tryConsume(argThat(key -> key != null && key.startsWith("global:")), anyInt(), any()))
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
        when(closedRateLimiter.tryConsume(any(), anyInt(), any()))
                .thenReturn(new RateLimitResult(false, 10, 0, RESET_MILLIS));

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/authenticate");
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
        when(clientIpExtractor.extract(any())).thenReturn("5.5.5.5");
        when(jwtTokenFromAuthHeaderExtractor.extract(any(MockHttpServletRequest.class))).thenReturn("valid-token");
        when(openRateLimiter.tryConsume(any(), anyInt(), any()))
                .thenReturn(new RateLimitResult(true, 60, 59, RESET_MILLIS));
        when(jwtClaimExtractor.extractEmail("valid-token")).thenReturn("alice@example.com");

        filter.doFilterInternal(new MockHttpServletRequest("GET", "/api/v1/cart"),
                new MockHttpServletResponse(), mock(FilterChain.class));

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(openRateLimiter, org.mockito.Mockito.times(2)).tryConsume(keyCaptor.capture(), anyInt(), any());
        assertThat(keyCaptor.getAllValues()).anyMatch(key -> key.contains("user:alice@example.com"));
        assertThat(keyCaptor.getAllValues()).anyMatch(key -> key.contains("pre-auth:ip:5.5.5.5"));
    }

    @Test
    @DisplayName("anonymous request key uses IP")
    void anonymousRequestKeyContainsIp() throws Exception {
        when(clientIpExtractor.extract(any())).thenReturn("5.5.5.5");
        when(jwtTokenFromAuthHeaderExtractor.extract(any(MockHttpServletRequest.class))).thenThrow(new RuntimeException("no token"));
        when(openRateLimiter.tryConsume(any(), anyInt(), any()))
                .thenReturn(new RateLimitResult(true, 60, 59, RESET_MILLIS));

        filter.doFilterInternal(new MockHttpServletRequest("GET", "/api/v1/products"),
                new MockHttpServletResponse(), mock(FilterChain.class));

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(openRateLimiter, org.mockito.Mockito.times(2)).tryConsume(keyCaptor.capture(), anyInt(), any());
        assertThat(keyCaptor.getAllValues()).anyMatch(key -> key.contains("global:ip:5.5.5.5"));
    }

    @Test
    @DisplayName("invalid token falls back to IP-based key")
    void invalidTokenStillUsesIpKey() throws Exception {
        when(clientIpExtractor.extract(any())).thenReturn("7.7.7.7");
        when(jwtTokenFromAuthHeaderExtractor.extract(any(MockHttpServletRequest.class))).thenReturn("revoked-token");
        org.mockito.Mockito.doThrow(new RuntimeException("revoked")).when(jwtBlacklistValidator).validate("revoked-token");
        when(openRateLimiter.tryConsume(any(), anyInt(), any()))
                .thenReturn(new RateLimitResult(true, 60, 59, RESET_MILLIS));

        filter.doFilterInternal(new MockHttpServletRequest("GET", "/api/v1/products"),
                new MockHttpServletResponse(), mock(FilterChain.class));

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(openRateLimiter, org.mockito.Mockito.times(2)).tryConsume(keyCaptor.capture(), anyInt(), any());
        assertThat(keyCaptor.getAllValues()).anyMatch(key -> key.contains("global:ip:7.7.7.7"));
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
    @DisplayName("first blocked request for a key emits WARN; second emits DEBUG (no second 429 header change)")
    void firstBlockWarnSecondBlockDebug() throws Exception {
        when(clientIpExtractor.extract(any())).thenReturn("9.9.9.9");
        RateLimitResult blocked = new RateLimitResult(false, 10, 0, RESET_MILLIS);
        when(openRateLimiter.tryConsume(argThat(key -> key != null && key.startsWith("pre-auth:")), anyInt(), any()))
                .thenReturn(new RateLimitResult(true, 200, 199, RESET_MILLIS));
        when(openRateLimiter.tryConsume(argThat(key -> key != null && key.startsWith("auth:")), anyInt(), any()))
                .thenReturn(blocked);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/refresh");
        // First call — warnedKeys cache is empty, so WARN path is taken
        MockHttpServletResponse response1 = new MockHttpServletResponse();
        filter.doFilterInternal(request, response1, mock(FilterChain.class));
        assertThat(response1.getStatus()).isEqualTo(429);

        // Second call with same key — warnedKeys cache has the entry, so DEBUG path is taken
        MockHttpServletResponse response2 = new MockHttpServletResponse();
        filter.doFilterInternal(request, response2, mock(FilterChain.class));
        assertThat(response2.getStatus()).isEqualTo(429);

        // Both calls still blocked — the logging path difference is internal;
        // we verify the filter ran twice and both returned 429
        verify(openRateLimiter, org.mockito.Mockito.times(4)).tryConsume(any(), anyInt(), any());
    }

    @Test
    @DisplayName("validation rejects non-positive auth limit")
    void validateRejectsNonPositiveAuthLimit() {
        RateLimitProperties properties = properties();
        properties.getAuth().setMaxRequests(0);
        filter = new RateLimitingFilter(
                openRateLimiter, closedRateLimiter, new SimpleMeterRegistry(), clientIpExtractor,
                jwtTokenFromAuthHeaderExtractor, jwtClaimExtractor, jwtBlacklistValidator, properties,
                problemTypeUriFactory,
                new CaffeineSizeProperties());

        assertThatThrownBy(filter::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("auth.max-requests must be > 0");
    }

    @Test
    @DisplayName("validation rejects non-positive search window")
    void validateRejectsNonPositiveSearchWindow() {
        RateLimitProperties properties = properties();
        properties.getSearch().setWindowDuration(Duration.ZERO);
        filter = new RateLimitingFilter(
                openRateLimiter, closedRateLimiter, new SimpleMeterRegistry(), clientIpExtractor,
                jwtTokenFromAuthHeaderExtractor, jwtClaimExtractor, jwtBlacklistValidator, properties,
                problemTypeUriFactory,
                new CaffeineSizeProperties());

        assertThatThrownBy(filter::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("search.window-duration must be positive");
    }

    @Test
    @DisplayName("strict pre-auth bucket blocks login before flood and primary rules")
    void strictPreAuthBucketBlocksLogin() throws Exception {
        when(clientIpExtractor.extract(any())).thenReturn("1.2.3.4");
        when(closedRateLimiter.tryConsume(argThat(key -> key != null && key.startsWith("auth:ip:")), anyInt(), any()))
                .thenReturn(new RateLimitResult(false, 10, 0, RESET_MILLIS));

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/authenticate");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, mock(FilterChain.class));

        assertThat(response.getStatus()).isEqualTo(429);
        verify(openRateLimiter, never()).tryConsume(argThat(key -> key != null && key.startsWith("pre-auth:")), anyInt(), any());
    }

    @Test
    @DisplayName("POST to generic endpoint uses write bucket")
    void postToGenericEndpointUsesWriteBucket() throws Exception {
        when(clientIpExtractor.extract(any())).thenReturn("1.2.3.4");
        when(openRateLimiter.tryConsume(argThat(key -> key != null && key.startsWith("pre-auth:")), anyInt(), any()))
                .thenReturn(new RateLimitResult(true, 200, 199, RESET_MILLIS));
        when(openRateLimiter.tryConsume(argThat(key -> key != null && key.startsWith("write:")), anyInt(), any()))
                .thenReturn(new RateLimitResult(true, 20, 19, RESET_MILLIS));

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/cart");
        filter.doFilterInternal(request, new MockHttpServletResponse(), mock(FilterChain.class));

        verify(openRateLimiter).tryConsume(argThat(key -> key != null && key.startsWith("write:")), anyInt(), any());
    }

    @Test
    @DisplayName("multipart upload to avatar endpoint uses file-upload bucket")
    void avatarUploadUsesFileUploadBucket() throws Exception {
        when(clientIpExtractor.extract(any())).thenReturn("1.2.3.4");
        when(openRateLimiter.tryConsume(argThat(key -> key != null && key.startsWith("pre-auth:")), anyInt(), any()))
                .thenReturn(new RateLimitResult(true, 200, 199, RESET_MILLIS));
        when(openRateLimiter.tryConsume(argThat(key -> key != null && key.startsWith("file-upload:")), anyInt(), any()))
                .thenReturn(new RateLimitResult(true, 5, 4, RESET_MILLIS));

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/users/avatar");
        request.setContentType("multipart/form-data; boundary=----");
        filter.doFilterInternal(request, new MockHttpServletResponse(), mock(FilterChain.class));

        verify(openRateLimiter).tryConsume(argThat(key -> key != null && key.startsWith("file-upload:")), anyInt(), any());
    }

    @Test
    @DisplayName("password reset is blocked by strict pre-auth bucket")
    void passwordResetBlockedByStrictPreAuth() throws Exception {
        when(clientIpExtractor.extract(any())).thenReturn("1.2.3.4");
        when(closedRateLimiter.tryConsume(argThat(key -> key != null && key.startsWith("auth:ip:")), anyInt(), any()))
                .thenReturn(new RateLimitResult(false, 10, 0, RESET_MILLIS));

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/users/password/reset");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, mock(FilterChain.class));

        assertThat(response.getStatus()).isEqualTo(429);
    }

    @Test
    @DisplayName("repeat offender gets banned after threshold blocks")
    void repeatOffenderGetsBanned() throws Exception {
        when(clientIpExtractor.extract(any())).thenReturn("10.10.10.10");
        when(closedRateLimiter.tryConsume(any(), anyInt(), any()))
                .thenReturn(new RateLimitResult(false, 10, 0, RESET_MILLIS));

        // Use a filter with low ban threshold for testing
        RateLimitProperties props = properties();
        props.setBanThreshold(3);
        RateLimitingFilter banFilter = new RateLimitingFilter(
                openRateLimiter, closedRateLimiter, new SimpleMeterRegistry(), clientIpExtractor,
                jwtTokenFromAuthHeaderExtractor, jwtClaimExtractor, jwtBlacklistValidator, props,
                problemTypeUriFactory, new CaffeineSizeProperties());

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/authenticate");

        // Trigger 3 blocks to reach ban threshold
        for (int i = 0; i < 3; i++) {
            banFilter.doFilterInternal(request, new MockHttpServletResponse(), mock(FilterChain.class));
        }

        // Next request should be short-circuited (no limiter call beyond the 3 above)
        MockHttpServletResponse bannedResponse = new MockHttpServletResponse();
        banFilter.doFilterInternal(request, bannedResponse, mock(FilterChain.class));

        assertThat(bannedResponse.getStatus()).isEqualTo(429);
        // The closed limiter was called 3 times for the blocks, but NOT for the banned request
        verify(closedRateLimiter, org.mockito.Mockito.times(3)).tryConsume(any(), anyInt(), any());
    }

    private static RateLimitProperties properties() {
        RateLimitProperties properties = new RateLimitProperties();
        properties.getPreAuth().setMaxRequests(200);
        properties.getPreAuth().setWindowDuration(Duration.ofMinutes(1));
        properties.getAuth().setMaxRequests(10);
        properties.getAuth().setWindowDuration(Duration.ofMinutes(1));
        properties.getGlobal().setMaxRequests(60);
        properties.getGlobal().setWindowDuration(Duration.ofMinutes(1));
        properties.getSearch().setMaxRequests(30);
        properties.getSearch().setWindowDuration(Duration.ofMinutes(1));
        properties.getTelemetry().setMaxRequests(120);
        properties.getTelemetry().setWindowDuration(Duration.ofMinutes(1));
        properties.getPayment().setMaxRequests(20);
        properties.getPayment().setWindowDuration(Duration.ofMinutes(1));
        properties.getWrite().setMaxRequests(20);
        properties.getWrite().setWindowDuration(Duration.ofMinutes(1));
        properties.getFileUpload().setMaxRequests(5);
        properties.getFileUpload().setWindowDuration(Duration.ofMinutes(1));
        properties.setBanThreshold(10);
        properties.setBanDuration(Duration.ofMinutes(5));
        return properties;
    }
}
