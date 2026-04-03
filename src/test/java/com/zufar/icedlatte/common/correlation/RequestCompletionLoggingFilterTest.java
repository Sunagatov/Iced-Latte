package com.zufar.icedlatte.common.correlation;

import com.zufar.icedlatte.common.util.ClientIpExtractor;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RequestCompletionLoggingFilter unit tests")
class RequestCompletionLoggingFilterTest {

    @Mock private ClientIpExtractor clientIpExtractor;
    @InjectMocks private RequestCompletionLoggingFilter filter;

    @Test
    @DisplayName("shouldNotFilter returns true for OPTIONS requests")
    void shouldNotFilterOptions() {
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/v1/products");
        assertThat(filter.shouldNotFilter(request)).isTrue();
    }

    @Test
    @DisplayName("shouldNotFilter returns true for actuator paths")
    void shouldNotFilterActuator() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        assertThat(filter.shouldNotFilter(request)).isTrue();
    }

    @Test
    @DisplayName("shouldNotFilter returns true for api docs paths")
    void shouldNotFilterApiDocs() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/docs/swagger-ui/index.html");
        assertThat(filter.shouldNotFilter(request)).isTrue();
    }

    @Test
    @DisplayName("shouldNotFilter returns false for regular API paths")
    void shouldNotFilterReturnsFalseForApi() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/products");
        assertThat(filter.shouldNotFilter(request)).isFalse();
    }

    @Test
    @DisplayName("doFilterInternal proceeds through filter chain for 2xx response")
    void doFilterInternalProceedsForSuccess() throws ServletException, IOException {
        ReflectionTestUtils.setField(filter, "slowRequestThresholdMs", 1000L);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/products");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);
        when(clientIpExtractor.extract(request)).thenReturn("127.0.0.1");
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("doFilterInternal proceeds through filter chain for 4xx response")
    void doFilterInternalProceedsForClientError() throws ServletException, IOException {
        ReflectionTestUtils.setField(filter, "slowRequestThresholdMs", 1000L);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/products");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(404);
        when(clientIpExtractor.extract(request)).thenReturn("127.0.0.1");
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("doFilterInternal proceeds through filter chain for 5xx response")
    void doFilterInternalProceedsForServerError() throws ServletException, IOException {
        ReflectionTestUtils.setField(filter, "slowRequestThresholdMs", 1000L);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/products");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(500);
        when(clientIpExtractor.extract(request)).thenReturn("127.0.0.1");
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("polling endpoints (brands, sellers) complete filter chain without error")
    void pollingEndpointsCompleteFilterChain() throws ServletException, IOException {
        ReflectionTestUtils.setField(filter, "slowRequestThresholdMs", 1000L);
        FilterChain chain = mock(FilterChain.class);

        for (String path : List.of("/api/v1/products/brands", "/api/v1/products/sellers")) {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
            MockHttpServletResponse response = new MockHttpServletResponse();
            response.setStatus(200);
            when(clientIpExtractor.extract(request)).thenReturn("127.0.0.1");

            filter.doFilterInternal(request, response, chain);
        }

        verify(chain, org.mockito.Mockito.times(2)).doFilter(any(), any());
    }
}
